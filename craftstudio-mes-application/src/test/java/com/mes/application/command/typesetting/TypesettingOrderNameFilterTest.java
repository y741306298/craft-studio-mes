package com.mes.application.command.typesetting;

import com.mes.application.dto.TypesettingQuery;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TypesettingOrderNameFilterTest {
    private final AppTypesettingService service = new AppTypesettingService();
    private final OrderItemService items = mock(OrderItemService.class);
    private final ProductionPieceService pieces = mock(ProductionPieceService.class);
    private final TypesettingService plates = mock(TypesettingService.class);

    private TypesettingQuery query() {
        ReflectionTestUtils.setField(service, "orderItemService", items);
        ReflectionTestUtils.setField(service, "productionPieceService", pieces);
        ReflectionTestUtils.setField(service, "domainTypesettingService", plates);
        TypesettingQuery query = new TypesettingQuery();
        query.setManufacturerMetaId("factory");
        query.setCustomerName("张");
        query.setOrgName("公司");
        return query;
    }

    @Test
    void resolvesNamesOnceAndAppliesSameScopeToBothCountsAndPage() {
        TypesettingQuery query = query();
        List<String> scope = List.of("item", "mongo-item");
        when(items.findItemIdsByOrderNames("factory", "张", "公司")).thenReturn(scope);
        when(pieces.countPendingTypesettingPiecesByProcessingConditions(
                "factory", null, null, null, scope, null, null, null, true)).thenReturn(2L);
        when(pieces.countPendingTypesettingPiecesByProcessingConditions(
                "factory", null, null, null, scope, null, null, null, false)).thenReturn(3L);
        when(pieces.findPendingTypesettingPiecesByProcessingConditions(
                "factory", null, null, null, scope, null, null, null, true, 0L, 2)).thenReturn(List.of());
        when(pieces.findPendingTypesettingPiecesByProcessingConditions(
                "factory", null, null, null, scope, null, null, null, false, 0L, 3)).thenReturn(List.of());

        var result = service.findTypesettingAndProductionPieces(query);

        assertEquals(5L, result.getPagedResult().total());
        verify(items, times(1)).findItemIdsByOrderNames("factory", "张", "公司");
        verify(pieces).findPendingTypesettingPiecesByProcessingConditions(
                "factory", null, null, null, scope, null, null, null, true, 0L, 2);
        verify(pieces).findPendingTypesettingPiecesByProcessingConditions(
                "factory", null, null, null, scope, null, null, null, false, 0L, 3);
        verifyNoInteractions(plates);
    }

    @Test
    void unmatchedNamesReturnEmptyListAndFiltersWithoutPieceOrPlateQueries() {
        TypesettingQuery query = query();
        when(items.findItemIdsByOrderNames("factory", "张", "公司")).thenReturn(List.of());
        assertEquals(0L, service.findTypesettingAndProductionPieces(query).getPagedResult().total());
        assertTrue(service.findAllTypesettingAndProductionPieces(query).isEmpty());
        verifyNoInteractions(pieces, plates);
    }

    @Test
    void blankNamesKeepExistingPlateQueryAndSkipNameLookup() {
        TypesettingQuery query = query();
        query.setCustomerName("  ");
        query.setOrgName("");
        query.setSourceType("TYPESETTING");
        when(plates.findPendingTypesettingByProcessingConditions(
                "factory", null, null, null, null, null, 0L, 50)).thenReturn(List.of());
        assertEquals(0L, service.findTypesettingAndProductionPieces(query).getPagedResult().total());
        verifyNoInteractions(items, pieces);
        verify(plates).countPendingTypesettingByProcessingConditions("factory", null, null, null, null, null);
    }
}
