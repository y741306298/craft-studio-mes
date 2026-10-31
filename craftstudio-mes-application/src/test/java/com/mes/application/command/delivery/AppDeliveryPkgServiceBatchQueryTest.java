package com.mes.application.command.delivery;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgScopedRequest;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

class AppDeliveryPkgServiceBatchQueryTest {

    @Test
    void imageSearchQueriesAllDistinctPieceIdsOnce() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        when(productionPieceService.findByProductionPieceIds(anyCollection()))
                .thenReturn(Collections.emptyList());

        List<DeliveryPkgPieceVO> result = service.findPendingPackagingPiecesByIds(
                java.util.Arrays.asList("piece-1", "", null, "piece-2", "piece-1"));

        assertEquals(Collections.emptyList(), result);
        verify(productionPieceService, times(1)).findByProductionPieceIds(
                argThat(ids -> ids.equals(new LinkedHashSet<>(List.of("piece-1", "piece-2")))));
        verifyNoMoreInteractions(productionPieceService);
    }

    @Test
    void imageSearchSkipsRelatedQueriesWhenPiecesAreNotPendingPackaging() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        OrderItemService orderItemService = mock(OrderItemService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        ReflectionTestUtils.setField(service, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(service, "orderInfoService", orderInfoService);
        when(productionPieceService.findByProductionPieceIds(anyCollection()))
                .thenReturn(List.of(new ProductionPiece()));

        assertEquals(Collections.emptyList(), service.findPendingPackagingPiecesByIds(List.of("piece-1")));
        verifyNoInteractions(orderItemService, orderInfoService);
    }

    @Test
    void imageSearchSkipsQueryWhenNoValidIdsRemain() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);

        assertEquals(Collections.emptyList(), service.findPendingPackagingPiecesByIds(null));
        assertEquals(Collections.emptyList(), service.findPendingPackagingPiecesByIds(Collections.emptyList()));
        assertEquals(Collections.emptyList(), service.findPendingPackagingPiecesByIds(
                java.util.Arrays.asList(null, "", "  ")));
        verifyNoInteractions(productionPieceService);
    }

    @Test
    void queriesProductionPiecesOnceForAllOrderItems() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        OrderItemService orderItemService = mock(OrderItemService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        ReflectionTestUtils.setField(service, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);

        OrderItem first = orderItem("item-1");
        OrderItem second = orderItem("item-2");
        when(orderItemService.findByOrderId("order-1", "manufacturer-1", 1, 100))
                .thenReturn(List.of(first, second));
        when(productionPieceService.findPendingPackagingPiecesByOrderItemIds(
                eq("manufacturer-1"), anyCollection(), eq("acrylic")))
                .thenReturn(Collections.emptyList());

        DeliveryPkgScopedRequest request = new DeliveryPkgScopedRequest();
        request.setManufacturerMetaId("manufacturer-1");
        request.setOrderId("order-1");
        request.setMaterialName("acrylic");

        service.listPendingPackagingPiecesById(request);

        verify(productionPieceService, times(1)).findPendingPackagingPiecesByOrderItemIds(
                eq("manufacturer-1"),
                argThat(ids -> ids.equals(new LinkedHashSet<>(List.of("item-1", "item-2")))),
                eq("acrylic"));
    }

    @Test
    void putsOriginalOrderRemarkBetweenOrderIdAndFileName() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        OrderItemService orderItemService = mock(OrderItemService.class);
        ReflectionTestUtils.setField(service, "orderInfoService", orderInfoService);
        ReflectionTestUtils.setField(service, "orderItemService", orderItemService);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setRemark("原备注");
        when(orderInfoService.findByOrderId("order-1")).thenReturn(orderInfo);

        OrderItem orderItem = orderItem("item-1");
        ImageFile productionImage = new ImageFile();
        productionImage.setName("production.png");
        orderItem.setProductionImgFile(productionImage);
        when(orderItemService.findByOrderItemIds(List.of("item-1"))).thenReturn(List.of(orderItem));

        DeliveryPkgPieceVO piece = new DeliveryPkgPieceVO();
        piece.setOrderItemId("item-1");
        DeliveryPkgAddRequest.DeliveryPkgPieceItem pieceItem = new DeliveryPkgAddRequest.DeliveryPkgPieceItem();
        pieceItem.setPiece(piece);

        String remarks = ReflectionTestUtils.invokeMethod(service, "buildDeliveryPkgRemarks",
                "order-1", "CUSTOM", null, List.of(pieceItem), null);

        assertEquals("订单:order-1\n原备注\n文件:production.png", remarks);
    }

    @Test
    void remarksBatchQueryPreservesRequestOrderAndDeduplicatesItemsAndFileNames() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        OrderItemService items = mock(OrderItemService.class);
        ReflectionTestUtils.setField(service, "orderItemService", items);
        OrderItem first = imageOrderItem("item-1", "first.png");
        OrderItem second = imageOrderItem("item-2", "second.png");
        OrderItem sameFile = imageOrderItem("item-3", "first.png");
        when(items.findByOrderItemIds(List.of("item-1", "item-2", "missing", "item-3")))
                .thenReturn(List.of(sameFile, second, first));

        String remarks = ReflectionTestUtils.invokeMethod(service, "buildDeliveryPkgRemarks",
                "order-1", "CUSTOM", null,
                java.util.Arrays.asList(requestPiece("p1", "item-1", 1), null,
                        requestPiece("p2", "item-2", 1), requestPiece("p3", "item-1", 1),
                        requestPiece("p4", "missing", 1), requestPiece("p5", "item-3", 1),
                        requestPiece("p6", " ", 1)), new OrderInfo());

        assertEquals("订单:order-1\n文件:first.png,second.png", remarks);
        verify(items).findByOrderItemIds(List.of("item-1", "item-2", "missing", "item-3"));
        verifyNoMoreInteractions(items);
    }

    @Test
    void addPkgDelegatesToDurableOperationBeforeRunningBusinessLogic() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        var operation = mock(com.mes.application.command.delivery.packaging.PackagingOperation.class);
        ReflectionTestUtils.setField(service, "packagingOperation", operation);
        DeliveryPkgAddRequest request = new DeliveryPkgAddRequest();
        var expected = new com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg();
        expected.setDeliveryPkgId("existing-package");
        when(operation.execute(eq(request), org.mockito.ArgumentMatchers.any())).thenReturn(expected);
        org.junit.jupiter.api.Assertions.assertSame(expected, service.addPkg(request));
        verify(operation).execute(eq(request), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void transferBatchLoadsRequiredQuantitiesAndPreservesRedoAndMissingItemFallback() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        ProductionPieceService pieces = mock(ProductionPieceService.class);
        OrderItemService items = mock(OrderItemService.class);
        ReflectionTestUtils.setField(service, "productionPieceService", pieces);
        ReflectionTestUtils.setField(service, "orderItemService", items);
        ProductionPiece normal = packingPiece("mongo-1", "piece-1");
        normal.setOrderItemId("shared-item");
        ProductionPiece redo = packingPiece("mongo-2", "piece-2");
        redo.setOrderItemId("shared-item");
        redo.setIsRedo(true);
        ProductionPiece missingItem = packingPiece("mongo-3", "piece-3");
        missingItem.setOrderItemId("missing-item");
        ProductionPiece noItem = packingPiece("mongo-4", "piece-4");
        OrderItem shared = orderItem("shared-item");
        shared.setQuantity(2);
        when(items.findByOrderItemIds(anyCollection())).thenReturn(List.of(shared));
        when(pieces.findByOrderItemIds(anyCollection())).thenReturn(Collections.emptyList());

        ReflectionTestUtils.invokeMethod(service, "transferPiecesToPacked",
                List.of(normal, redo, missingItem, noItem),
                java.util.Map.of("mongo-1", 1, "mongo-2", 2, "mongo-3", 10, "mongo-4", 3),
                "carrier", "送货上门", null, null, null);

        String completed = com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus.COMPLETED.getCode();
        org.junit.jupiter.api.Assertions.assertNotEquals(completed, normal.getStatus());
        assertEquals(completed, redo.getStatus());
        assertEquals(completed, missingItem.getStatus());
        org.junit.jupiter.api.Assertions.assertNotEquals(completed, noItem.getStatus());
        assertEquals(1, normal.getDeliveryPkgInfos().get(0).getQuantity());
        assertEquals(2, redo.getDeliveryPkgInfos().get(0).getQuantity());
        // One batch for per-piece completion and one batch for the aggregate refresh, regardless of piece count.
        verify(items, times(2)).findByOrderItemIds(argThat(ids -> ids.size() == 2
                && ids.containsAll(List.of("shared-item", "missing-item"))));
        verify(items).batchUpdateOrderItems(Collections.emptyList());
        verifyNoMoreInteractions(items);
        verify(pieces).findByOrderItemIds(argThat(ids -> ids.size() == 2
                && ids.containsAll(List.of("shared-item", "missing-item"))));
    }

    private ProductionPiece packingPiece(String id, String productionPieceId) {
        ProductionPiece piece = new ProductionPiece();
        piece.setId(id);
        piece.setProductionPieceId(productionPieceId);
        piece.setQuantity(10);
        var pending = new com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode();
        pending.setNodeName("待打包");
        pending.setPieceQuantity(10);
        var packed = new com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode();
        packed.setNodeName("已打包");
        packed.setPieceQuantity(0);
        var flow = new com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow();
        flow.setNodes(List.of(pending, packed));
        piece.setProcedureFlow(flow);
        return piece;
    }

    private DeliveryPkgAddRequest.DeliveryPkgPieceItem requestPiece(String pieceId, String itemId, int quantity) {
        DeliveryPkgPieceVO piece = new DeliveryPkgPieceVO();
        piece.setProductionPieceId(pieceId);
        piece.setOrderItemId(itemId);
        piece.setOrderId("order-1");
        var carrier = new com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo();
        carrier.setCarrierId("custom-carrier");
        carrier.setPresetType("CUSTOM");
        piece.setLogisticsCarrierInfo(carrier);
        var item = new DeliveryPkgAddRequest.DeliveryPkgPieceItem();
        item.setPiece(piece);
        item.setQuantity(quantity);
        return item;
    }

    private OrderItem imageOrderItem(String id, String fileName) {
        OrderItem item = orderItem(id);
        ImageFile image = new ImageFile();
        image.setName(fileName);
        item.setProductionImgFile(image);
        return item;
    }

    private OrderItem orderItem(String id) {
        OrderItem item = new OrderItem();
        item.setOrderItemId(id);
        return item;
    }
}
