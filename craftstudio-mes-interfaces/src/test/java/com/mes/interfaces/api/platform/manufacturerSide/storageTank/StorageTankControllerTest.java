package com.mes.interfaces.api.platform.manufacturerSide.storageTank;

import com.mes.application.command.auth.AppLoginService;
import com.mes.application.command.transBox.AppStorageTankService;
import com.mes.application.dto.req.storageTank.StorageTankListRequest;
import com.mes.application.dto.req.storageTank.StorageTankRequest;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StorageTankControllerTest {
    private final AppStorageTankService service = mock(AppStorageTankService.class);
    private final AppLoginService login = mock(AppLoginService.class);
    private final StorageTankController controller = new StorageTankController(service, login);
    private static final String DEFAULT = "Bearer craftstudio-mes-default";
    private static final String MANUFACTURER = "69f956c00ff1ad90a9611464";

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(controller, "defaultAuthorization", DEFAULT);
        ReflectionTestUtils.setField(controller, "defaultManufacturerId", MANUFACTURER);
    }

    @Test
    void defaultAuthorizationUsesMappedManufacturerForAllEndpointsWithoutTokenLookup() {
        StorageTankRequest request = new StorageTankRequest();
        StorageTankListRequest query = new StorageTankListRequest();
        controller.add(DEFAULT, request);
        controller.edit(DEFAULT, request);
        controller.list(DEFAULT, query);
        controller.get(DEFAULT, "tank-id");
        controller.delete(DEFAULT, "tank-id");
        verify(service).add(MANUFACTURER, request);
        verify(service).edit(MANUFACTURER, request);
        verify(service).list(MANUFACTURER, query);
        verify(service).get(MANUFACTURER, "tank-id");
        verify(service).delete(MANUFACTURER, "tank-id");
        verifyNoInteractions(login);
    }

    @Test
    void realTokenStillUsesLoginManufacturer() {
        when(login.getManufacturerMetaIdByToken("real-token")).thenReturn("other-manufacturer");
        controller.get("Bearer real-token", "tank-id");
        verify(service).get("other-manufacturer", "tank-id");
    }

    @Test
    void missingDefaultMappingFailsWithoutCallingService() {
        ReflectionTestUtils.setField(controller, "defaultManufacturerId", "");
        assertThrows(BusinessNotAllowException.class, () -> controller.get(DEFAULT, "tank-id"));
        verifyNoInteractions(service, login);
    }

    @Test
    void configuredDefaultAuthorizationReplacesBuiltInDefault() {
        ReflectionTestUtils.setField(controller, "defaultAuthorization", "Bearer custom-default");
        controller.get("Bearer custom-default", "tank-id");
        verify(service).get(MANUFACTURER, "tank-id");
        verifyNoInteractions(login);
    }
}
