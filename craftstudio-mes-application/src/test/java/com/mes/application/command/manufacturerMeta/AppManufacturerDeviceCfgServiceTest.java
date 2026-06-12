package com.mes.application.command.manufacturerMeta;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingPrintTaskRepository;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingDownloadTaskData;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppManufacturerDeviceCfgServiceTest {

    @Test
    void listDownloadTasksByTypesettingCodeMergesMultipleTaskDataIntoOneResult() {
        TypesettingPrintTaskRepository repository = mock(TypesettingPrintTaskRepository.class);
        AppManufacturerDeviceCfgService service = new AppManufacturerDeviceCfgService();
        ReflectionTestUtils.setField(service, "typesettingPrintTaskRepository", repository);

        TypesettingDownloadTaskData firstData = new TypesettingDownloadTaskData();
        firstData.setId("typesetting-1");
        firstData.setDeviceInfoId("device-info-1");
        firstData.setDeviceInfoIds(List.of("device-info-1"));
        firstData.setDeviceCodes(List.of("device-code-1"));
        firstData.setImamges(List.of("image-1", "image-duplicate"));
        firstData.setPlts(List.of("plt-1"));
        firstData.setJsons(List.of("json-1"));
        firstData.setMarks(List.of("mark-1"));

        TypesettingDownloadTaskData secondData = new TypesettingDownloadTaskData();
        secondData.setId("typesetting-1");
        secondData.setDeviceInfoId("device-info-2");
        secondData.setDeviceInfoIds(List.of("device-info-2"));
        secondData.setDeviceCodes(List.of("device-code-2"));
        secondData.setImamges(List.of("image-2", "image-duplicate"));
        secondData.setPlts(List.of("plt-2"));
        secondData.setJsons(List.of("json-2"));
        secondData.setMarks(List.of("mark-2"));

        TypesettingPrintTask firstTask = new TypesettingPrintTask();
        firstTask.setData(firstData);
        TypesettingPrintTask secondTask = new TypesettingPrintTask();
        secondTask.setData(secondData);
        when(repository.filterList(eq(1L), eq(100), anyMap())).thenReturn(List.of(firstTask, secondTask));

        TypesettingDownloadTaskData result = service.listDownloadTasksByTypesettingCode("typesetting-code-1");

        assertEquals("typesetting-1", result.getId());
        assertEquals("device-info-1", result.getDeviceInfoId());
        assertEquals(List.of("image-1", "image-duplicate", "image-2"), result.getImamges());
        assertEquals(List.of("plt-1", "plt-2"), result.getPlts());
        assertEquals(List.of("json-1", "json-2"), result.getJsons());
        assertEquals(List.of("mark-1", "mark-2"), result.getMarks());
        assertEquals(List.of("device-info-1", "device-info-2"), result.getDeviceInfoIds());
        assertEquals(List.of("device-code-1", "device-code-2"), result.getDeviceCodes());
        verify(repository).filterList(eq(1L), eq(100), eq(Map.of("typesettingCode", "typesetting-code-1")));
    }
}
