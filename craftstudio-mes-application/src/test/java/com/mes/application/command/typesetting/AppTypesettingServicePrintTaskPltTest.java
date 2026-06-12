package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingDownloadTaskData;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppTypesettingServicePrintTaskPltTest {

    private final AppTypesettingService service = new AppTypesettingService();

    @Test
    void buildDownloadTaskDataCollectsPltsFromChildTypesettingCellsByTheirOwnLayoutMode() {
        TypesettingService typesettingService = mock(TypesettingService.class);
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesettingService);

        TypesettingInfo root = typesetting("root", TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A20PR0, null, null);
        root.setTypesettingCells(List.of(typesettingCell("child-require-plt"), typesettingCell("child-no-plt")));

        TypesettingInfo childRequirePlt = typesetting(
                "child-require-plt",
                TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CIRCLE,
                "oss://bucket/child-normal.plt",
                "oss://bucket/child-reverse.plt");
        TypesettingInfo childNoPlt = typesetting(
                "child-no-plt",
                TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A30_SMALL_GRAPH,
                "oss://bucket/stale-normal.plt",
                "oss://bucket/stale-reverse.plt");
        when(typesettingService.findById("child-require-plt")).thenReturn(childRequirePlt);
        when(typesettingService.findById("child-no-plt")).thenReturn(childNoPlt);

        TypesettingDownloadTaskData data = ReflectionTestUtils.invokeMethod(
                service,
                "buildDownloadTaskData",
                "print-task-id",
                "device-info-id",
                "device-code",
                root.getElement(),
                Collections.emptyMap(),
                Collections.emptySet(),
                root);

        assertThat(data.getPlts()).containsExactly("child-normal.plt", "child-reverse.plt");
    }

    private TypesettingInfo typesetting(String id,
                                        TypesettingLayoutMode layoutMode,
                                        String normalPlt,
                                        String reversePlt) {
        TypesettingInfo info = new TypesettingInfo();
        info.setId(id);
        info.setLayoutMode(layoutMode.getCode());
        info.applyLayoutModeConfig();
        TypesettingElement element = new TypesettingElement();
        element.setJson("oss://bucket/" + id + ".json");
        if (normalPlt != null || reversePlt != null) {
            element.setPlt(new TypesettingElement.PltObjectName(normalPlt, reversePlt));
        }
        info.setElement(element);
        return info;
    }

    private TypesettingSourceCell typesettingCell(String sourceId) {
        TypesettingSourceCell cell = new TypesettingSourceCell();
        cell.setSourceType(TypesettingSourceType.TYPESETTING.getCode());
        cell.setSourceId(sourceId);
        cell.setQuantity(1);
        return cell;
    }
}
