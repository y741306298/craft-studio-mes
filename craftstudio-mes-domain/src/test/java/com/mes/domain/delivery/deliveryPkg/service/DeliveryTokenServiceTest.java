package com.mes.domain.delivery.deliveryPkg.service;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryTokenRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeliveryTokenServiceTest {
    private DeliveryTokenService service;
    private DeliveryTokenRepository repository;

    @BeforeEach
    void setUp() {
        service = new DeliveryTokenService();
        repository = mock(DeliveryTokenRepository.class);
        ReflectionTestUtils.setField(service, "deliveryTokenRepository", repository);
    }

    @Test
    void createsConfigurationForNewCarrierAndFactory() {
        DeliveryToken request = request();
        request.setId("client-supplied-id");
        when(repository.add(any())).thenAnswer(invocation -> {
            DeliveryToken saved = invocation.getArgument(0);
            assertNull(saved.getId());
            saved.setId("new-id");
            return saved;
        });
        assertEquals("new-id", service.configureDeliveryToken(request).getId());
        verify(repository).findByCarrierIdAndManufacturerMetaId("carrier-1", "factory-1");
        verify(repository, never()).update(any());
    }

    @Test
    void replacesConfigurationWhilePreservingIdentityAndCreationTime() {
        DeliveryToken existing = request();
        existing.setId("existing-id");
        existing.setCreateTime(new Date(123456));
        when(repository.findByCarrierIdAndManufacturerMetaId("carrier-1", "factory-1"))
                .thenReturn(existing);
        DeliveryToken replacement = request();
        replacement.setPartnerKey("replacement-key");
        DeliveryToken saved = service.configureDeliveryToken(replacement);
        assertEquals("existing-id", saved.getId());
        assertEquals(existing.getCreateTime(), saved.getCreateTime());
        assertEquals("replacement-key", saved.getPartnerKey());
        verify(repository).update(replacement);
        verify(repository, never()).add(any());
    }

    @Test
    void rejectsMissingRoutingOrAccountFieldsBeforeWriting() {
        assertThrows(BusinessNotAllowException.class, () -> service.configureDeliveryToken(null));
        for (String field : new String[]{"carrierId", "manufacturerMetaId", "partnerId", "kuaidicom"}) {
            DeliveryToken invalid = request();
            ReflectionTestUtils.setField(invalid, field, " ");
            assertThrows(BusinessNotAllowException.class, () -> service.configureDeliveryToken(invalid));
        }
        verifyNoInteractions(repository);
    }

    private DeliveryToken request() {
        DeliveryToken token = new DeliveryToken();
        token.setCarrierId(" carrier-1 ");
        token.setManufacturerMetaId(" factory-1 ");
        token.setPartnerId("monthly-account");
        token.setKuaidicom("shunfeng");
        return token;
    }
}
