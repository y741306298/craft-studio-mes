package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.service.TypesettingPrintTaskService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingDownloadTaskData;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

        assertThat(data.getPlts()).containsExactly("oss://bucket/child-normal.plt", "oss://bucket/child-reverse.plt");
    }

    @Test
    void savePltBroadcastPrintTaskAssignsAllCuttingDevices() {
        ManufacturerDeviceCfgRepository deviceCfgRepository = mock(ManufacturerDeviceCfgRepository.class);
        TypesettingPrintTaskService printTaskService = mock(TypesettingPrintTaskService.class);
        ReflectionTestUtils.setField(service, "manufacturerDeviceCfgRepository", deviceCfgRepository);
        ReflectionTestUtils.setField(service, "typesettingPrintTaskService", printTaskService);
        when(deviceCfgRepository.filterList(any(Integer.class), any(Integer.class), any())).thenReturn(List.of(
                deviceCfg("cutting-device-info-1", "cutting-device-code-1", "切割机1"),
                deviceCfg("printer-device-info-id", "printer-device-code", "严-测试打印机"),
                deviceCfg("cutting-device-info-2", "cutting-device-code-2", "自动切割机2")
        ));

        TypesettingDownloadTaskData originalData = new TypesettingDownloadTaskData();
        originalData.setId("print-task-id");
        originalData.setDeviceInfoId("printer-device-info-id");
        originalData.setDeviceInfoIds(List.of("printer-device-info-id"));
        originalData.setDeviceCodes(List.of("printer-device-code"));
        originalData.setPlts(List.of("oss://bucket/child-normal.plt", "oss://bucket/child-reverse.plt"));

        ReflectionTestUtils.invokeMethod(
                service,
                "savePltBroadcastPrintTask",
                "print-task-id",
                "typesetting-code",
                "manufacturer-meta-id",
                originalData);

        ArgumentCaptor<TypesettingPrintTask> taskCaptor = ArgumentCaptor.forClass(TypesettingPrintTask.class);
        verify(printTaskService).saveOrUpdate(taskCaptor.capture());
        TypesettingPrintTask task = taskCaptor.getValue();
        assertThat(task.getTypesettingInfoId()).isEqualTo("print-task-id_plt");
        assertThat(task.getDeviceInfoId()).containsExactly("cutting-device-info-1", "cutting-device-info-2");
        assertThat(task.getDeviceCode()).containsExactly("cutting-device-code-1", "cutting-device-code-2");
        assertThat(task.getData().getDeviceInfoId()).isEqualTo("cutting-device-info-1");
        assertThat(task.getData().getDeviceInfoIds()).containsExactly("cutting-device-info-1", "cutting-device-info-2");
        assertThat(task.getData().getDeviceCodes()).containsExactly("cutting-device-code-1", "cutting-device-code-2");
        assertThat(task.getData().getImamges()).isEmpty();
        assertThat(task.getData().getJsons()).isEmpty();
        assertThat(task.getData().getMarks()).isEmpty();
        assertThat(task.getData().getPlts()).containsExactly("oss://bucket/child-normal.plt", "oss://bucket/child-reverse.plt");
    }

    @Test
    void savePltBroadcastPrintTaskCollectsPltsFromRootWhenDownloadDataHasNoPlts() {
        ManufacturerDeviceCfgRepository deviceCfgRepository = mock(ManufacturerDeviceCfgRepository.class);
        TypesettingPrintTaskService printTaskService = mock(TypesettingPrintTaskService.class);
        TypesettingService typesettingService = mock(TypesettingService.class);
        ReflectionTestUtils.setField(service, "manufacturerDeviceCfgRepository", deviceCfgRepository);
        ReflectionTestUtils.setField(service, "typesettingPrintTaskService", printTaskService);
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesettingService);
        when(deviceCfgRepository.filterList(any(Integer.class), any(Integer.class), any())).thenReturn(List.of(
                deviceCfg("cutting-device-info-1", "cutting-device-code-1", "切割机1"),
                deviceCfg("cutting-device-info-2", "cutting-device-code-2", "自动切割机2")
        ));

        TypesettingInfo root = typesetting("root", TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_NO_FILM, null, null);
        root.setTypesettingCells(List.of(typesettingCell("child-require-plt")));
        TypesettingInfo childRequirePlt = typesetting(
                "child-require-plt",
                TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CIRCLE,
                "oss://bucket/child-normal.plt",
                "oss://bucket/child-reverse.plt");
        when(typesettingService.findById("child-require-plt")).thenReturn(childRequirePlt);

        TypesettingDownloadTaskData originalData = new TypesettingDownloadTaskData();
        originalData.setId("print-task-id");
        originalData.setDeviceInfoId("printer-device-info-id");
        originalData.setDeviceInfoIds(List.of("printer-device-info-id"));
        originalData.setDeviceCodes(List.of("printer-device-code"));
        originalData.setPlts(Collections.emptyList());

        ReflectionTestUtils.invokeMethod(
                service,
                "savePltBroadcastPrintTask",
                "print-task-id",
                "typesetting-code",
                "manufacturer-meta-id",
                originalData,
                root);

        ArgumentCaptor<TypesettingPrintTask> taskCaptor = ArgumentCaptor.forClass(TypesettingPrintTask.class);
        verify(printTaskService).saveOrUpdate(taskCaptor.capture());
        TypesettingPrintTask task = taskCaptor.getValue();
        assertThat(task.getDeviceInfoId()).containsExactly("cutting-device-info-1", "cutting-device-info-2");
        assertThat(task.getDeviceCode()).containsExactly("cutting-device-code-1", "cutting-device-code-2");
        assertThat(task.getData().getPlts())
                .containsExactly("oss://bucket/child-normal.plt", "oss://bucket/child-reverse.plt");
    }


    @Test
    void buildDownloadTaskDataCollectsRootPltWhenOnlyTypesettingIdIsAvailable() {
        TypesettingInfo root = typesetting(null, TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CIRCLE,
                "oss://bucket/root-normal.plt",
                "oss://bucket/root-reverse.plt");
        root.setTypesettingId("root-typesetting-id");

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

        assertThat(data.getPlts()).containsExactly("oss://bucket/root-normal.plt", "oss://bucket/root-reverse.plt");
    }

    @Test
    void savePltBroadcastPrintTaskSkipsWhenOnlyRootPltsExistButNoTargetDeviceCanBeResolved() {
        ManufacturerDeviceCfgRepository deviceCfgRepository = mock(ManufacturerDeviceCfgRepository.class);
        TypesettingPrintTaskService printTaskService = mock(TypesettingPrintTaskService.class);
        ReflectionTestUtils.setField(service, "manufacturerDeviceCfgRepository", deviceCfgRepository);
        ReflectionTestUtils.setField(service, "typesettingPrintTaskService", printTaskService);
        when(deviceCfgRepository.filterList(any(Integer.class), any(Integer.class), any())).thenReturn(Collections.emptyList());

        TypesettingInfo root = typesetting(null, TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CIRCLE,
                "oss://bucket/root-normal.plt",
                null);

        ReflectionTestUtils.invokeMethod(
                service,
                "savePltBroadcastPrintTask",
                "print-task-id",
                "typesetting-code",
                "manufacturer-meta-id",
                null,
                root);

        verify(printTaskService, never()).saveOrUpdate(any(TypesettingPrintTask.class));
    }

    private ManufacturerDeviceCfg deviceCfg(String deviceInfoId, String deviceCode, String deviceName) {
        ManufacturerDeviceCfg cfg = new ManufacturerDeviceCfg();
        cfg.setDeviceInfoId(deviceInfoId);
        cfg.setDeviceCode(deviceCode);
        cfg.setDeviceName(deviceName);
        return cfg;
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
