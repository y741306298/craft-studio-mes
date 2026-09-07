package com.mes.application.command.order;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.vo.ManufacturerInfo;
import com.mes.domain.order.orderInfo.vo.OrderPriceInfo;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AppOrderServiceTest {

    private final AppOrderService service = new AppOrderService();

    @Test
    void allowsTransferQuantityEqualToPendingTypesettingQuantity() {
        assertThat(hasPendingTypesettingQuantityLessThan(productionPiece(5), 5)).isFalse();
    }

    @Test
    void rejectsTransferQuantityGreaterThanPendingTypesettingQuantity() {
        assertThat(hasPendingTypesettingQuantityLessThan(productionPiece(5), 6)).isTrue();
    }

    @Test
    void allowsTransferQuantityLessThanPendingTypesettingQuantity() {
        assertThat(hasPendingTypesettingQuantityLessThan(productionPiece(5), 4)).isFalse();
    }

    @Test
    void createsTransferOrderWithItsOwnOrderIdAndActualTransferPrice() {
        OrderInfo source = orderInfoWithManufacturerPrice("100.00");
        source.setOrderId("SOURCE-ORDER");
        ManufacturerMeta targetManufacturer = new ManufacturerMeta();
        targetManufacturer.setManufacturerMetaId("TARGET-MANUFACTURER");
        targetManufacturer.setName("目标工厂");

        OrderInfo target = ReflectionTestUtils.invokeMethod(service, "copyOrderInfoForTransfer",
                source, targetManufacturer, "TRANSFER-ORDER", new BigDecimal("30.00"));

        assertThat(target).isNotNull();
        assertThat(target.getOrderId()).isEqualTo("TRANSFER-ORDER");
        assertThat(target.getManufacturerId()).isEqualTo("TARGET-MANUFACTURER");
        assertThat(target.getManufacturerInfo().getPrice().getActualPrice()).isEqualByComparingTo("30.00");
        assertThat(target.getManufacturerInfo().getPrice().getOriActualPrice()).isEqualByComparingTo("30.00");
        assertThat(source.getManufacturerInfo().getPrice().getActualPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void attachesTransferredItemToGeneratedTransferOrder() {
        OrderItem source = new OrderItem();
        source.setOrderId("SOURCE-ORDER");
        source.setOrderItemId("SOURCE-ITEM");
        source.setQuantity(10);
        source.setProductionPieces(List.of(new ProductionPiece()));
        source.setStatus(OrderStatus.IN_PRODUCTION);
        source.setFailureReason("旧的失败原因");
        source.setPreprocessRequestId("SOURCE-REQUEST");

        OrderItem target = ReflectionTestUtils.invokeMethod(service, "copyOrderItemForTransfer",
                source, "TRANSFER-ORDER", "TRANSFER-ITEM", "TARGET-MANUFACTURER", 3);

        assertThat(target).isNotNull();
        assertThat(target.getOrderId()).isEqualTo("TRANSFER-ORDER");
        assertThat(target.getOrderItemId()).isEqualTo("TRANSFER-ITEM");
        assertThat(target.getQuantity()).isEqualTo(3);
        assertThat(target.getProductionPieces()).isNull();
        assertThat(target.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(target.getFailureReason()).isNull();
        assertThat(target.getPreprocessRequestId()).isNotBlank().isNotEqualTo("SOURCE-REQUEST");
    }

    @Test
    void rejectsTransferAmountGreaterThanSourceOrderPrice() {
        String error = service.validateTransferPrice(orderInfoWithManufacturerPrice("20.00"),
                new BigDecimal("20.01"));

        assertThat(error).isEqualTo("转单金额不能大于源订单剩余工厂实际价");
    }

    @Test
    void acceptsTransferAmountEqualToSourceOrderPrice() {
        String error = service.validateTransferPrice(orderInfoWithManufacturerPrice("20.00"),
                new BigDecimal("20.00"));

        assertThat(error).isNull();
    }

    @Test
    void deletesSourceOrderItemWhenItsFullQuantityIsTransferred() {
        OrderItemService orderItemService = mock(OrderItemService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        ReflectionTestUtils.setField(service, "domainOrderItemService", orderItemService);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        OrderItem sourceItem = new OrderItem();
        sourceItem.setId("SOURCE-ITEM-ID");
        sourceItem.setQuantity(3);
        ProductionPiece sourcePiece = new ProductionPiece();
        sourcePiece.setId("SOURCE-PIECE-ID");

        service.updateSourceOrderItemAfterTransfer(sourceItem, List.of(sourcePiece), 3);

        verify(productionPieceService).deleteProductionPiece("SOURCE-PIECE-ID");
        verify(orderItemService).deleteOrderItem("SOURCE-ITEM-ID");
        verify(orderItemService, never()).updateOrderItem(sourceItem);
    }

    @Test
    void deletesSourceOrderAfterAllActiveItemsAreTransferred() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        ReflectionTestUtils.setField(service, "domainOrderInfoService", orderInfoService);
        OrderInfo sourceOrder = orderInfoWithManufacturerPrice("30.00");
        sourceOrder.setId("SOURCE-ORDER-ID");

        service.updateSourceOrderAfterTransfer(sourceOrder, List.of(), new BigDecimal("30.00"));

        assertThat(sourceOrder.getManufacturerInfo().getPrice().getActualPrice()).isEqualByComparingTo("0.00");
        verify(orderInfoService).updateOrder(sourceOrder);
        verify(orderInfoService).deleteOrder("SOURCE-ORDER-ID");
    }

    private boolean hasPendingTypesettingQuantityLessThan(ProductionPiece piece, int transferQuantity) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                service,
                "hasPendingTypesettingQuantityLessThan",
                piece,
                transferQuantity
        ));
    }

    private ProductionPiece productionPiece(int pendingQuantity) {
        ProcedureFlowNode pendingTypesetting = new ProcedureFlowNode();
        pendingTypesetting.setNodeName("待排版");
        pendingTypesetting.setPieceQuantity(pendingQuantity);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(pendingTypesetting));
        ProductionPiece piece = new ProductionPiece();
        piece.setProcedureFlow(flow);
        return piece;
    }

    private OrderInfo orderInfoWithManufacturerPrice(String actualPrice) {
        OrderPriceInfo price = new OrderPriceInfo();
        price.setActualPrice(new BigDecimal(actualPrice));
        ManufacturerInfo manufacturerInfo = new ManufacturerInfo();
        manufacturerInfo.setPrice(price);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setManufacturerInfo(manufacturerInfo);
        return orderInfo;
    }
}
