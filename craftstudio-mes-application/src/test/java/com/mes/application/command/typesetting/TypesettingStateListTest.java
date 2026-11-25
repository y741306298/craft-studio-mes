package com.mes.application.command.typesetting;

import com.mes.application.command.print.AppPrintService;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.TypesettingQuery;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TypesettingStateListTest {
    private final TypesettingService layouts = mock(TypesettingService.class);
    private final AppTypesettingService typesetting = new AppTypesettingService();
    private final AppPrintService printing = new AppPrintService();

    @BeforeEach
    void injectServices() {
        ReflectionTestUtils.setField(typesetting, "domainTypesettingService", layouts);
        ReflectionTestUtils.setField(printing, "typesettingService", layouts);
    }

    @Test
    void confirmingListCoversBothNestingStages() {
        List<String> statuses = List.of("confirming", "in_progress", "layout_processing", "failed");
        when(layouts.findByStatusesFromPrimary("factory", statuses, "material"))
                .thenReturn(statuses.stream().map(this::layout).toList());

        var result = typesetting.findConfirmingTypesetting("factory", null, "material", 1, 20);

        assertEquals(statuses, result.items().stream().map(TypesettingInfo::getStatus).toList());
    }

    @Test
    void typesettingListIncludesPlateBeingConfirmedInItsPageAndTotal() {
        var query = new TypesettingQuery();
        query.setManufacturerMetaId("factory");
        query.setSourceType(TypesettingSourceType.TYPESETTING.getCode());
        query.setSize(20);
        when(layouts.countPendingTypesettingByProcessingConditions(
                "factory", null, null, null, null, null, true)).thenReturn(1L);
        when(layouts.findPendingTypesettingByProcessingConditions(
                "factory", null, null, null, null, null, true, 0, 20))
                .thenReturn(List.of(layout("confirm_in_progress")));

        var result = typesetting.findTypesettingAndProductionPieces(query).getPagedResult();

        assertEquals(1L, result.total());
        assertEquals(List.of("confirm_in_progress"),
                result.items().stream().map(TypesettingProductionPieceVO::getStatus).toList());
    }

    @Test
    void pendingPrintListIncludesDispatchByDefaultAndAsAnExplicitFilter() {
        List<String> defaultStatuses = List.of("printing", "printing_in_progress", "dispatch_in_progress");
        when(layouts.findTypesettingByStatuses(
                "factory", defaultStatuses, null, "material", null, null, null, 1, Integer.MAX_VALUE))
                .thenReturn(defaultStatuses.stream().map(this::layout).toList());
        when(layouts.findTypesettingByStatuses(
                "factory", List.of("dispatch_in_progress"), null, "material", null, null, null, 1, Integer.MAX_VALUE))
                .thenReturn(List.of(layout("dispatch_in_progress")));

        var all = printing.findPendingPrintTypesetting("factory", null, null, "material", null, null, null, 1, 20);
        var dispatch = printing.findPendingPrintTypesetting(
                "factory", null, null, "material", null, null, "dispatch_in_progress", 1, 20);

        assertEquals(defaultStatuses, all.items().stream().map(TypesettingInfo::getStatus).toList());
        assertEquals(List.of("dispatch_in_progress"), dispatch.items().stream().map(TypesettingInfo::getStatus).toList());
    }

    private TypesettingInfo layout(String status) {
        var layout = new TypesettingInfo();
        layout.setId(status);
        layout.setTypesettingId("TS-" + status);
        layout.setStatus(status);
        return layout;
    }
}
