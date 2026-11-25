package com.mes.application.command.delivery;

import com.mes.application.dto.req.delivery.DeliveryPkgActionRequest;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.delivery.deliveryPkg.service.DeliveryPkgService;
import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;
import com.mes.domain.gatherplatform.wdt.repository.WdtLabelRecordRepository;
import com.mes.domain.order.enums.OrderChannelType;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppDeliveryPkgServiceCloudPrintTest {

    @Test
    void localReprintReturnsSignedCloudPrintDataFromLabelRecord() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        DeliveryPkgService packages = mock(DeliveryPkgService.class);
        OrderInfoService orders = mock(OrderInfoService.class);
        WdtLabelRecordRepository labels = mock(WdtLabelRecordRepository.class);
        ReflectionTestUtils.setField(service, "deliveryPkgService", packages);
        ReflectionTestUtils.setField(service, "orderInfoService", orders);
        ReflectionTestUtils.setField(service, "wdtLabelRecordRepository", labels);

        DeliveryPkg pkg = new DeliveryPkg();
        pkg.setDeliveryPkgId("pkg-1");
        pkg.setOrderId("order-1");
        pkg.setLogisticsCloudPrintData(Map.of("cloudPrintPlatform", "CAINIAO", "printData", "old-data"));
        when(packages.findByDeliveryPkgId("pkg-1")).thenReturn(List.of(pkg));

        OrderChannelInfo channel = new OrderChannelInfo();
        channel.setType(OrderChannelType.GATHER_PLATFORM);
        channel.setCode("JST");
        OrderInfo order = new OrderInfo();
        order.setChannel(channel);
        when(orders.findByOrderId("order-1")).thenReturn(order);

        Map<String, String> signedPrintData = Map.of(
                "cloudPrintPlatform", "CAINIAO", "printData", "signed-data", "signature", "label-signature");
        WdtLabelRecord record = new WdtLabelRecord();
        record.setPlatformType("JST");
        record.setLogisticsCloudPrintData(signedPrintData);
        when(labels.findByDeliveryPkgId("pkg-1")).thenReturn(record);

        DeliveryPkgActionRequest request = new DeliveryPkgActionRequest();
        request.setDeliveryPkgId("pkg-1");
        assertEquals(signedPrintData, service.reprintGatherPlatformLabel(request).getLogisticsCloudPrintData());
    }
}
