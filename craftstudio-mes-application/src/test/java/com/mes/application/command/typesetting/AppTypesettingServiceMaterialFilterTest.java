package com.mes.application.command.typesetting;

import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppTypesettingServiceMaterialFilterTest {

    @Test
    void appliesMaterialIdToEveryStatusInConfirmingList() {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingService typesettingService = mock(TypesettingService.class);
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesettingService);

        for (TypesettingStatus status : new TypesettingStatus[]{
                TypesettingStatus.CONFIRMING, TypesettingStatus.IN_PROGRESS, TypesettingStatus.FAILED}) {
            when(typesettingService.findTypesettingByConditions(
                    "manufacturer-1", status.getCode(), null, "material-1", null,
                    null, null, null, 1, Integer.MAX_VALUE))
                    .thenReturn(Collections.emptyList());
        }

        service.findConfirmingTypesetting("manufacturer-1", null, "material-1", 1, 20);

        for (TypesettingStatus status : new TypesettingStatus[]{
                TypesettingStatus.CONFIRMING, TypesettingStatus.IN_PROGRESS, TypesettingStatus.FAILED}) {
            verify(typesettingService).findTypesettingByConditions(
                    "manufacturer-1", status.getCode(), null, "material-1", null,
                    null, null, null, 1, Integer.MAX_VALUE);
        }
    }

    @Test
    void pendingMaterialQueryOnlySelectsPendingTypesettings() {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingService typesettingService = mock(TypesettingService.class);
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesettingService);
        when(typesettingService.findMaterialsByStatuses(
                "manufacturer-1", Collections.singletonList(TypesettingStatus.PENDING.getCode())))
                .thenReturn(Collections.emptyList());

        service.findPendingTypesettingMaterials("manufacturer-1");

        verify(typesettingService).findMaterialsByStatuses(
                "manufacturer-1", Collections.singletonList(TypesettingStatus.PENDING.getCode()));
    }
}
