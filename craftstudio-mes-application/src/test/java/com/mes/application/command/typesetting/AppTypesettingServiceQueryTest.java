package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.api.resp.FormeGenerationResponse;
import com.mes.application.command.typesetting.vo.TypesettingPiecesQueryResult;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.TypesettingQuery;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppTypesettingServiceQueryTest {

    @Test
    void shouldResolveBloodFromToLayoutCacheWithoutDatabaseLookup() {
        AppTypesettingService service = new AppTypesettingService();
        LayoutConfirmRequest cachedRequest = new LayoutConfirmRequest();
        cachedRequest.setTypesettingCells(List.of(
                cachedCell("part-1", false),
                cachedCell("part-2", true)
        ));

        Boolean result = ReflectionTestUtils.invokeMethod(service, "resolveCachedHaveBlood",
                List.of(sourceCell("part-1"), sourceCell("part-2")), cachedRequest);

        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void shouldFallBackWhenCachedBloodSnapshotIsIncomplete() {
        AppTypesettingService service = new AppTypesettingService();
        LayoutConfirmRequest cachedRequest = new LayoutConfirmRequest();
        cachedRequest.setTypesettingCells(List.of(cachedCell("part-1", false)));

        Boolean result = ReflectionTestUtils.invokeMethod(service, "resolveCachedHaveBlood",
                List.of(sourceCell("part-1"), sourceCell("part-2")), cachedRequest);

        assertNull(result);
    }

    @Test
    void shouldTreatCompletedNestingStateAsDuplicateCallback() {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingInfo completed = new TypesettingInfo();
        completed.setStatus(TypesettingStatus.CONFIRMING.getCode());

        Boolean duplicate = ReflectionTestUtils.invokeMethod(service, "isNestingCallbackAlreadyHandled",
                List.of(completed));

        assertEquals(Boolean.TRUE, duplicate);
    }

    @Test
    void shouldTreatClearedFormeOperationAsDuplicateCallback() {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingInfo completed = new TypesettingInfo();
        completed.setStatus(TypesettingStatus.PRINTING.getCode());
        completed.setRemark(null);
        FormeGenerationResponse response = new FormeGenerationResponse();
        response.setStatus("success");

        Boolean duplicate = ReflectionTestUtils.invokeMethod(service, "isFormeCallbackAlreadyHandled",
                completed, response);

        assertEquals(Boolean.TRUE, duplicate);
    }

    @Test
    void shouldBatchLoadOrderItemsWhenListingECommercePieces() {
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        OrderItemService orderItemService = mock(OrderItemService.class);
        AppTypesettingService service = new AppTypesettingService();
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        ReflectionTestUtils.setField(service, "orderItemService", orderItemService);

        ProductionPiece firstPiece = pendingPiece("piece-1", "item-1");
        ProductionPiece secondPiece = pendingPiece("piece-2", "item-2");
        when(productionPieceService.findPendingTypesettingPiecesByProcessingConditions(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(firstPiece, secondPiece));
        when(orderItemService.findByOrderItemIds(any())).thenReturn(List.of(
                orderItem("item-1", "order-1"),
                orderItem("item-2", "order-2")
        ));

        TypesettingQuery query = new TypesettingQuery();
        query.setManufacturerMetaId("manufacturer-1");
        query.setSourceType(TypesettingSourceType.PART.getCode());
        query.setECommerceMmodel(true);

        TypesettingPiecesQueryResult result = service.findTypesettingAndProductionPieces(query);

        List<TypesettingProductionPieceVO> items = result.getAllItems();
        assertEquals(List.of("order-1", "order-2"), items.stream()
                .map(TypesettingProductionPieceVO::getGroupId)
                .toList());
        verify(orderItemService, times(1)).findByOrderItemIds(any(Collection.class));
        verify(orderItemService, never()).findByOrderItemId(any());
        verify(orderItemService, never()).findById(any());
    }

    private ProductionPiece pendingPiece(String id, String orderItemId) {
        ProcedureFlowNode pendingNode = new ProcedureFlowNode();
        pendingNode.setNodeName("待排版");
        pendingNode.setPieceQuantity(1);
        ProcedureFlow procedureFlow = new ProcedureFlow();
        procedureFlow.setNodes(List.of(pendingNode));

        ProductionPiece piece = new ProductionPiece();
        piece.setId(id);
        piece.setOrderItemId(orderItemId);
        piece.setProcedureFlow(procedureFlow);
        return piece;
    }

    private TypesettingProductionPieceVO cachedCell(String id, Boolean haveBlood) {
        TypesettingProductionPieceVO cell = new TypesettingProductionPieceVO();
        cell.setSourceType(TypesettingSourceType.PART.getCode());
        cell.setSourceId(id);
        cell.setHaveBlood(haveBlood);
        return cell;
    }

    private TypesettingSourceCell sourceCell(String id) {
        TypesettingSourceCell cell = new TypesettingSourceCell();
        cell.setSourceType(TypesettingSourceType.PART.getCode());
        cell.setSourceId(id);
        return cell;
    }

    private OrderItem orderItem(String orderItemId, String orderId) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(orderItemId);
        orderItem.setOrderId(orderId);
        return orderItem;
    }
}
