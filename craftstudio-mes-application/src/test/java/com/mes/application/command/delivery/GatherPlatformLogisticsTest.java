package com.mes.application.command.delivery;

import com.mes.application.command.order.AppPreOrderLabelTaskService;
import com.mes.domain.gatherplatform.wdt.entity.WdtConfig;
import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;
import com.mes.domain.gatherplatform.wdt.repository.WdtLabelRecordRepository;
import com.mes.domain.gatherplatform.wdt.service.WdtService;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsWarehouseConfigException;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsLabel;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import com.piliofpala.craftstudio.shared.domain.logistics.vo.LogisticsCarrierPresetType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** These tests mock the platform client; they never modify real logistics orders. */
class GatherPlatformLogisticsTest {
    @Test
    void resolvesJstAndPreservesLegacyWdt() {
        assertEquals(GatherPlatformType.JST, GatherPlatformResolver.resolveCode(" jst "));
        assertEquals(GatherPlatformType.WDT, GatherPlatformResolver.resolveCode("wdt"));
        assertEquals(GatherPlatformType.WDT, GatherPlatformResolver.resolveCode(null));
        assertEquals(GatherPlatformType.WDT, GatherPlatformResolver.resolveCode(" "));
        assertThrows(IllegalArgumentException.class, () -> GatherPlatformResolver.resolveCode("OTHER"));
    }

    @Test
    void preprintsJstUsingOnlyJstConfigurationAndSavesPlatformSnapshot() throws Exception {
        AppPreOrderLabelTaskService service = new AppPreOrderLabelTaskService();
        installGuard(service);
        WdtService configs = mock(WdtService.class);
        ReflectionTestUtils.setField(service, "wdtService", configs);
        OrderInfo order = order();
        WdtConfig config = new WdtConfig();
        config.setPlatformType("JST");
        config.setManufacturerMetaId("factory");
        config.setLogisticsId("ZTO");
        config.setWarehouseId("15715817");
        config.setPresetType("ZTO");
        when(configs.findConfig("JST", "factory", "ZTO")).thenReturn(config);
        GatherPlatform client = mock(GatherPlatform.class);
        LogisticsLabel label = new LogisticsLabel();
        label.setLogisticsOrderId("ZTO123");
        when(client.printLogisticsLabel("542919", "default", true, LogisticsCarrierPresetType.ZTO)).thenReturn(label);
        try (MockedStatic<GatherPlatform> factory = mockStatic(GatherPlatform.class)) {
            factory.when(() -> GatherPlatform.getInstance(GatherPlatformType.JST)).thenReturn(client);
            AppDeliveryPkgService.DeliveryPkgPrintResult result = ReflectionTestUtils.invokeMethod(
                    service, "preOrderGatherPlatformLabel", order, List.of());
            assertNotNull(result);
            assertEquals("ZTO123", result.getKuaidiNum());
            verify(client).configLogisticsWarehouse("ZTO", "15715817", "542919");
            var record = org.mockito.ArgumentCaptor.forClass(WdtLabelRecord.class);
            verify(configs).saveLabelRecord(record.capture());
            assertEquals("JST", record.getValue().getPlatformType());
            assertEquals("542919", record.getValue().getChannelOrderId());
            assertEquals("ZTO123", record.getValue().getLogisticsOrderId());
            factory.verify(() -> GatherPlatform.getInstance(GatherPlatformType.WDT), never());
        }
    }

    @Test
    void missingJstConfigDoesNotFallBackToWdt() {
        AppPreOrderLabelTaskService service = new AppPreOrderLabelTaskService();
        installGuard(service);
        WdtService configs = mock(WdtService.class);
        ReflectionTestUtils.setField(service, "wdtService", configs);
        try (MockedStatic<GatherPlatform> factory = mockStatic(GatherPlatform.class)) {
            assertNull(ReflectionTestUtils.invokeMethod(service, "preOrderGatherPlatformLabel", order(), List.of()));
            verify(configs).findConfig("JST", "factory", "ZTO");
            factory.verifyNoInteractions();
        }
    }

    @Test
    void jstCainiaoReprintReusesLocalDataWithoutCallingPlatform() {
        AppDeliveryPkgService service = spy(new AppDeliveryPkgService());
        var pkg = new com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg();
        pkg.setOrderId("10001");
        pkg.setDeliveryPkgId("pkg");
        WdtLabelRecord record = record();
        var printData = java.util.Map.of("cloudPrintPlatform", "CAINIAO", "printData", "stored-data");
        record.setLogisticsCloudPrintData(printData);
        pkg.setLogisticsCloudPrintData(printData);
        doReturn(pkg).when(service).findByDeliveryPkgId("pkg");
        var orders = mock(com.mes.domain.order.orderInfo.service.OrderInfoService.class);
        OrderInfo order = order();
        order.getChannel().setType(com.mes.domain.order.enums.OrderChannelType.GATHER_PLATFORM);
        when(orders.findByOrderId("10001")).thenReturn(order);
        ReflectionTestUtils.setField(service, "orderInfoService", orders);
        ReflectionTestUtils.setField(service, "kuaidi100Environment", "prod");
        WdtLabelRecordRepository repository = mock(WdtLabelRecordRepository.class);
        when(repository.findByDeliveryPkgId("pkg")).thenReturn(record);
        ReflectionTestUtils.setField(service, "wdtLabelRecordRepository", repository);
        var request = new com.mes.application.dto.req.delivery.DeliveryPkgActionRequest();
        request.setDeliveryPkgId("pkg");
        try (MockedStatic<GatherPlatform> factory = mockStatic(GatherPlatform.class)) {
            assertSame(pkg, service.reprintGatherPlatformLabel(request));
            assertSame(printData, pkg.getLogisticsCloudPrintData());
            assertSame(record, pkg.getWdtLabelRecord());
            factory.verifyNoInteractions();
            verify(repository, never()).update(any());
        }
    }

    @Test
    void reprintsJstWithStoredWaybillAndUpdatesRecord() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        installGuard(service);
        WdtLabelRecordRepository repository = mock(WdtLabelRecordRepository.class);
        ReflectionTestUtils.setField(service, "wdtLabelRecordRepository", repository);
        WdtLabelRecord record = record();
        LogisticsLabel label = new LogisticsLabel();
        label.setLogisticsOrderId("ZTO123");
        GatherPlatform client = mock(GatherPlatform.class);
        when(client.rePrint("542919", "mes-logistics", true, LogisticsCarrierPresetType.ZTO, "ZTO123")).thenReturn(label);
        try (MockedStatic<GatherPlatform> factory = mockStatic(GatherPlatform.class)) {
            factory.when(() -> GatherPlatform.getInstance(GatherPlatformType.JST)).thenReturn(client);
            WdtLabelRecord result = ReflectionTestUtils.invokeMethod(service, "reprintGatherPlatformLabel", record);
            assertSame(record, result);
            verify(repository).update(record);
            verify(client, never()).configLogisticsWarehouse(anyString(), anyString(), anyString());
        } catch (LogisticsWarehouseConfigException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void reprintCannotReplaceStoredWaybillWithAnotherNumber() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        installGuard(service);
        WdtLabelRecordRepository repository = mock(WdtLabelRecordRepository.class);
        ReflectionTestUtils.setField(service, "wdtLabelRecordRepository", repository);
        WdtLabelRecord record = record();
        LogisticsLabel label = new LogisticsLabel();
        label.setLogisticsOrderId("DIFFERENT");
        GatherPlatform client = mock(GatherPlatform.class);
        when(client.rePrint(anyString(), anyString(), anyBoolean(), any(), anyString())).thenReturn(label);
        try (MockedStatic<GatherPlatform> factory = mockStatic(GatherPlatform.class)) {
            factory.when(() -> GatherPlatform.getInstance(GatherPlatformType.JST)).thenReturn(client);
            assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(service, "reprintGatherPlatformLabel", record));
            assertEquals("ZTO123", record.getLogisticsOrderId());
            verifyNoInteractions(repository);
        }
    }

    @Test
    void testEnvironmentReprintNeverCallsRealPlatform() {
        AppDeliveryPkgService service = spy(new AppDeliveryPkgService());
        com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg pkg = new com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg();
        pkg.setOrderId("10001");
        pkg.setDeliveryPkgId("pkg");
        doReturn(pkg).when(service).findByDeliveryPkgId("pkg");
        var orders = mock(com.mes.domain.order.orderInfo.service.OrderInfoService.class);
        OrderInfo order = order();
        order.getChannel().setType(com.mes.domain.order.enums.OrderChannelType.GATHER_PLATFORM);
        when(orders.findByOrderId("10001")).thenReturn(order);
        ReflectionTestUtils.setField(service, "orderInfoService", orders);
        ReflectionTestUtils.setField(service, "kuaidi100Environment", "test");
        var request = new com.mes.application.dto.req.delivery.DeliveryPkgActionRequest();
        request.setDeliveryPkgId("pkg");
        try (MockedStatic<GatherPlatform> factory = mockStatic(GatherPlatform.class)) {
            assertSame(pkg, service.reprintGatherPlatformLabel(request));
            factory.verifyNoInteractions();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void lockedJstReprintCannotCallPlatformOrUpdateRecord() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        org.springframework.data.redis.core.RedisTemplate<String, Object> redis = mock(org.springframework.data.redis.core.RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> values = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true);
        when(redis.hasKey(JstPrintGuard.AUTH_LOCK_KEY)).thenReturn(true);
        ReflectionTestUtils.setField(service, "jstPrintGuard", new JstPrintGuard(redis));
        WdtLabelRecordRepository repository = mock(WdtLabelRecordRepository.class);
        ReflectionTestUtils.setField(service, "wdtLabelRecordRepository", repository);
        try (MockedStatic<GatherPlatform> factory = mockStatic(GatherPlatform.class)) {
            assertThrows(JstPrintUnavailableException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "reprintGatherPlatformLabel", record()));
            factory.verifyNoInteractions();
            verifyNoInteractions(repository);
        }
    }

    @Test
    void customerRetryUsesExistingWaybillEvenWhenLocked() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        installGuard(service);
        WdtLabelRecordRepository repository = mock(WdtLabelRecordRepository.class);
        ReflectionTestUtils.setField(service, "wdtLabelRecordRepository", repository);
        var orders = mock(com.mes.domain.order.orderInfo.service.OrderInfoService.class);
        ReflectionTestUtils.setField(service, "orderInfoService", orders);
        OrderInfo order = order();
        order.getChannel().setType(com.mes.domain.order.enums.OrderChannelType.GATHER_PLATFORM);
        when(orders.findByOrderId("10001")).thenReturn(order);
        WdtLabelRecord record = record();
        when(repository.findForPlatform("JST", "10001", "542919", null)).thenReturn(record);
        GatherPlatform client = mock(GatherPlatform.class);
        LogisticsLabel label = new LogisticsLabel();
        label.setLogisticsOrderId("ZTO123");
        when(client.rePrint("542919", "mes-logistics", true, LogisticsCarrierPresetType.ZTO, "ZTO123")).thenReturn(label);
        JstPrintGuard guard = (JstPrintGuard) ReflectionTestUtils.getField(service, "jstPrintGuard");
        org.springframework.data.redis.core.RedisTemplate<String, Object> redis =
                (org.springframework.data.redis.core.RedisTemplate<String, Object>) ReflectionTestUtils.getField(guard, "redisTemplate");
        when(redis.hasKey(JstPrintGuard.AUTH_LOCK_KEY)).thenReturn(true);
        try (MockedStatic<GatherPlatform> factory = mockStatic(GatherPlatform.class)) {
            factory.when(() -> GatherPlatform.getInstance(GatherPlatformType.JST)).thenReturn(client);
            assertSame(record, service.retryJstPrintForSmsAuth("10001"));
            verify(client).rePrint("542919", "mes-logistics", true, LogisticsCarrierPresetType.ZTO, "ZTO123");
            verify(repository).update(record);
            verify(redis, never()).delete(JstPrintGuard.AUTH_LOCK_KEY);
        }
    }

    @Test
    void preprintReusesSuccessfulCustomerRecoveryLabelWithoutPrintingAgain() {
        AppPreOrderLabelTaskService service = new AppPreOrderLabelTaskService();
        installGuard(service);
        WdtService configs = mock(WdtService.class);
        WdtConfig config = new WdtConfig();
        when(configs.findConfig("JST", "factory", "ZTO")).thenReturn(config);
        ReflectionTestUtils.setField(service, "wdtService", configs);
        WdtLabelRecordRepository repository = mock(WdtLabelRecordRepository.class);
        ReflectionTestUtils.setField(service, "wdtLabelRecordRepository", repository);
        WdtLabelRecord saved = record();
        when(repository.findForPlatform("JST", "10001", "542919", null)).thenReturn(saved);
        try (MockedStatic<GatherPlatform> factory = mockStatic(GatherPlatform.class)) {
            AppDeliveryPkgService.DeliveryPkgPrintResult result = ReflectionTestUtils.invokeMethod(
                    service, "preOrderGatherPlatformLabel", order(), List.of());
            assertNotNull(result);
            assertEquals("ZTO123", result.getKuaidiNum());
            factory.verifyNoInteractions();
            verify(configs, never()).saveLabelRecord(any());
        }
    }

    @SuppressWarnings("unchecked")
    private void installGuard(Object service) {
        org.springframework.data.redis.core.RedisTemplate<String, Object> redis = mock(org.springframework.data.redis.core.RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> values = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true);
        ReflectionTestUtils.setField(service, "jstPrintGuard", new JstPrintGuard(redis));
        if (service instanceof AppPreOrderLabelTaskService) {
            ReflectionTestUtils.setField(service, "wdtLabelRecordRepository", mock(WdtLabelRecordRepository.class));
        }
    }

    private WdtLabelRecord record() {
        WdtLabelRecord record = new WdtLabelRecord();
        record.setPlatformType("JST");
        record.setChannelOrderId("542919");
        record.setPresetType("ZTO");
        record.setLogisticsOrderId("ZTO123");
        return record;
    }

    private OrderInfo order() {
        OrderChannelInfo channel = new OrderChannelInfo();
        channel.setCode("JST");
        channel.setOrderId("542919");
        OrderInfo order = new OrderInfo();
        order.setChannel(channel);
        order.setOrderId("10001");
        order.setManufacturerId("factory");
        LogisticsCarrierInfo carrier = new LogisticsCarrierInfo();
        carrier.setPresetType("ZTO");
        order.setLogisticsCarrierInfo(carrier);
        return order;
    }
}
