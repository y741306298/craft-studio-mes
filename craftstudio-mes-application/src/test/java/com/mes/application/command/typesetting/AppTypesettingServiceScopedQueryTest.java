package com.mes.application.command.typesetting;

import com.mes.application.dto.TypesettingQuery;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppTypesettingServiceScopedQueryTest {

    @Test
    void queriesAllOrderItemsInOneProductionPieceCall() {
        AppTypesettingService service = new AppTypesettingService();
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        when(productionPieceService.findPendingTypesettingPiecesByOrderItemIds(
                eq("factory-1"), eq(List.of("item-1")), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        TypesettingQuery query = new TypesettingQuery();
        query.setManufacturerMetaId("factory-1");
        query.setOrderItemId("item-1");

        assertThat(service.findTypesettingAndProductionPiecesById(query)).isEmpty();
        verify(productionPieceService).findPendingTypesettingPiecesByOrderItemIds(
                eq("factory-1"), eq(List.of("item-1")), any(), any(), any(), any(), any());
    }

    @Test
    void queriesPendingTypesettingByExactTypesettingId() {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingService typesettingService = mock(TypesettingService.class);
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesettingService);
        when(typesettingService.findPendingByTypesettingId(
                eq("factory-1"), eq("layout-1"), any(), any(), any(), any()))
                .thenReturn(List.of(new TypesettingInfo()));

        TypesettingQuery query = new TypesettingQuery();
        query.setManufacturerMetaId("factory-1");
        query.setTypesettingId("layout-1");

        assertThat(service.findTypesettingAndProductionPiecesById(query)).hasSize(1);
        verify(typesettingService).findPendingByTypesettingId(
                eq("factory-1"), eq("layout-1"), any(), any(), any(), any());
    }
}
