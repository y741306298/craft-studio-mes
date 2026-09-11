package com.mes.application.command.order;

import com.mes.application.command.statistics.vo.OrderStatisticsFiltersVO;
import com.mes.application.dto.req.order.OrderAddRequest;
import com.mes.application.support.PodvOrgInfoHelper;
import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.command.orderPreprocessing.OrderPreprocessTaskQueue;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
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
import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.mes.domain.order.orderStatistics.entity.OrderStatisticsType;
import com.mes.domain.order.orderStatistics.service.OrderDailyStatisticsService;
import com.mes.domain.order.preOrderLabelTask.service.PreOrderLabelTaskService;
import com.mes.domain.order.productionPieceGenerationTask.service.ProductionPieceGenerationTaskService;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    void createsTransferOrderWithTransferredManufacturerPriceWithoutChangingOrderPrice() {
        OrderInfo source = orderInfoWithManufacturerPrice("100.00");
        source.setOrderId("SOURCE-ORDER");
        source.getManufacturerInfo().getPrice().setLogisticsPrice(new BigDecimal("12.00"));
        source.getManufacturerInfo().getPrice().setPaymentPrice(new BigDecimal("112.00"));
        OrderPriceInfo orderPrice = new OrderPriceInfo();
        orderPrice.setLogisticsPrice(new BigDecimal("20.00"));
        orderPrice.setPaymentPrice(new BigDecimal("120.00"));
        source.setPrice(orderPrice);
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
        assertThat(target.getManufacturerInfo().getPrice().getLogisticsPrice()).isEqualByComparingTo("0.00");
        assertThat(target.getManufacturerInfo().getPrice().getPaymentPrice()).isEqualByComparingTo("30.00");
        assertThat(target.getPrice().getLogisticsPrice()).isEqualByComparingTo("20.00");
        assertThat(target.getPrice().getPaymentPrice()).isEqualByComparingTo("120.00");
        assertThat(source.getManufacturerInfo().getPrice().getActualPrice()).isEqualByComparingTo("100.00");
        assertThat(source.getManufacturerInfo().getPrice().getLogisticsPrice()).isEqualByComparingTo("12.00");
        assertThat(source.getManufacturerInfo().getPrice().getPaymentPrice()).isEqualByComparingTo("112.00");
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

    @Test
    void fillsMissingRouteNameInOrderStatisticsFilters() {
        OrderDailyStatisticsService statisticsService = mock(OrderDailyStatisticsService.class);
        DeliveryRouteRepository routeRepository = mock(DeliveryRouteRepository.class);
        ReflectionTestUtils.setField(service, "orderDailyStatisticsService", statisticsService);
        ReflectionTestUtils.setField(service, "deliveryRouteRepository", routeRepository);
        LocalDate date = LocalDate.of(2026, 9, 10);
        OrderDailyStatistics statistics = new OrderDailyStatistics();
        statistics.setType(OrderStatisticsType.ROUTE);
        statistics.setIndexId("ROUTE-DOCUMENT-ID");
        statistics.setTotalOrderCount(1L);
        DeliveryRoute route = new DeliveryRoute();
        route.setId("ROUTE-DOCUMENT-ID");
        route.setRouteId("ROUTE-BUSINESS-ID");
        route.setRouteName("华东路线");
        when(statisticsService.list("MANUFACTURER", date, date)).thenReturn(List.of(statistics));
        when(routeRepository.findByIdsOrRouteIds(Set.of("ROUTE-DOCUMENT-ID"))).thenReturn(List.of(route));

        OrderStatisticsFiltersVO result = service.findOrderStatisticsFilters("MANUFACTURER", date, date);

        assertThat(result.getRoutes()).singleElement().satisfies(dimension -> {
            assertThat(dimension.getId()).isEqualTo("ROUTE-DOCUMENT-ID");
            assertThat(dimension.getName()).isEqualTo("华东路线");
        });
    }

    @Test
    void persistsRouteNameWhenGeneratingOrderDailyStatistics() {
        OrderDailyStatisticsService statisticsService = mock(OrderDailyStatisticsService.class);
        DeliveryRouteRepository routeRepository = mock(DeliveryRouteRepository.class);
        ReflectionTestUtils.setField(service, "orderDailyStatisticsService", statisticsService);
        ReflectionTestUtils.setField(service, "deliveryRouteRepository", routeRepository);
        DeliveryRoute route = new DeliveryRoute();
        route.setId("ROUTE-DOCUMENT-ID");
        route.setRouteId("ROUTE-BUSINESS-ID");
        route.setRouteName("华东路线");
        when(routeRepository.findByIdsOrRouteIds(Set.of("ROUTE-BUSINESS-ID"))).thenReturn(List.of(route));
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setRouteId("ROUTE-BUSINESS-ID");

        ReflectionTestUtils.invokeMethod(service, "incrementOrderDimensions",
                "MANUFACTURER", orderInfo, List.of(), 1L, BigDecimal.ONE);

        verify(statisticsService).increment(
                eq("MANUFACTURER"), any(LocalDate.class),
                eq("ROUTE-BUSINESS-ID"), eq("华东路线"), eq(OrderStatisticsType.ROUTE),
                eq(1L), eq(new BigDecimal("0.00")), eq(new BigDecimal("0.00")));
    }

    @Test
    void skipsAllAddOperationsWhenNonReturnedOrderAlreadyExists() {
        OrderAddRequest request = mock(OrderAddRequest.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        OrderDailyStatisticsService statisticsService = mock(OrderDailyStatisticsService.class);
        ProductionPieceGenerationTaskService generationTaskService = mock(ProductionPieceGenerationTaskService.class);
        PreOrderLabelTaskService labelTaskService = mock(PreOrderLabelTaskService.class);
        AppOrderPreprocessingService preprocessingService = mock(AppOrderPreprocessingService.class);
        OrderPreprocessTaskQueue preprocessTaskQueue = mock(OrderPreprocessTaskQueue.class);
        ReflectionTestUtils.setField(service, "podvOrgInfoHelper", mock(PodvOrgInfoHelper.class));
        ReflectionTestUtils.setField(service, "domainOrderInfoService", orderInfoService);
        ReflectionTestUtils.setField(service, "orderDailyStatisticsService", statisticsService);
        ReflectionTestUtils.setField(service, "productionPieceGenerationTaskService", generationTaskService);
        ReflectionTestUtils.setField(service, "preOrderLabelTaskService", labelTaskService);
        ReflectionTestUtils.setField(service, "appOrderPreprocessingService", preprocessingService);
        ReflectionTestUtils.setField(service, "orderPreprocessTaskQueue", preprocessTaskQueue);
        OrderInfo orderInfo = orderInfoWithManufacturerPrice("1555.20");
        orderInfo.setOrderId("2098348860696453122");
        orderInfo.setManufacturerId("6a26a93758a9abfcdc66d93c");
        OrderInfo existingOrder = new OrderInfo();
        existingOrder.setStatus(OrderStatus.PENDING);
        when(request.toOrderInfo()).thenReturn(orderInfo);
        when(request.toOrderItems()).thenReturn(List.of());
        when(orderInfoService.findByOrderIdAndManufacturerId(
                "2098348860696453122", "6a26a93758a9abfcdc66d93c")).thenReturn(existingOrder);

        OrderInfo result = service.addOrderWithItems(request);

        assertThat(result).isSameAs(existingOrder);
        verify(orderInfoService, never()).addOrderWithItems(any(), any());
        verifyNoInteractions(statisticsService);
        verifyNoInteractions(generationTaskService, labelTaskService, preprocessingService, preprocessTaskQueue);
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
