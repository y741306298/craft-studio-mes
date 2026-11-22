package com.mes.application.command.typesetting;

import com.mes.application.command.api.resp.FormeGenerationResponse;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingPrintTaskService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrintTaskSeparatorTest {

    @Test
    void savesPersistedSeparatorMarkAndImageReferencesInThePrintTask() {
        String recordId = "plate";
        String png = "https://assets.example/imgutil/marker/result.png";
        String legacyImage = "https://assets.example/piece.jpg";
        String formeSvg = "https://assets.example/forme.svg";
        AppTypesettingService service = new AppTypesettingService();
        TypesettingService typesetting = mock(TypesettingService.class);
        TypesettingPrintTaskService tasks = mock(TypesettingPrintTaskService.class);
        ManufacturerDeviceCfgRepository devices = mock(ManufacturerDeviceCfgRepository.class);
        RestTemplate downloads = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesetting);
        ReflectionTestUtils.setField(service, "typesettingPrintTaskService", tasks);
        ReflectionTestUtils.setField(service, "manufacturerDeviceCfgRepository", devices);
        ReflectionTestUtils.setField(service, "restTemplate", downloads);

        TypesettingInfo layout = new TypesettingInfo();
        layout.setId(recordId);
        layout.setTypesettingId("layout");
        layout.setLayoutMode(TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_NO_FILM.getCode());
        layout.setStatus(TypesettingStatus.DISPATCH_IN_PROGRESS.getCode());
        layout.setRemark("FORME_OP:PRINT:printer");
        TypesettingInfo persisted = new TypesettingInfo();
        persisted.setMarks(Map.of("caifuMarker_d14a0630-292c-41db-831c-27e9ffc8a51c", png));
        when(typesetting.findById(recordId)).thenReturn(layout, persisted);
        ManufacturerDeviceCfg printer = new ManufacturerDeviceCfg();
        printer.setDeviceCode("printer");
        printer.setDeviceInfoId("device");
        when(devices.filterList(eq(1L), eq(1), anyMap())).thenReturn(List.of(printer));
        when(downloads.getForObject(URI.create(formeSvg), String.class)).thenReturn("""
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
                  <image xlink:href="%s"/>
                  <image href="%s"/>
                  <image href="data:image/png;base64,AA=="/>
                  <img src="%s"/>
                </svg>
                """.formatted(png, png, legacyImage));
        FormeGenerationResponse.Result result = new FormeGenerationResponse.Result();
        result.setFormeSvg(formeSvg);
        FormeGenerationResponse response = new FormeGenerationResponse();
        response.setStatus("success");
        response.setResult(result);

        ReflectionTestUtils.invokeMethod(service, "doHandleGenerateFormeCallback", response, recordId);

        ArgumentCaptor<TypesettingPrintTask> task = ArgumentCaptor.forClass(TypesettingPrintTask.class);
        verify(tasks).saveOrUpdate(task.capture());
        assertEquals(List.of(png), task.getValue().getData().getMarks());
        assertEquals(List.of(png, legacyImage), task.getValue().getData().getImamges());
    }
}
