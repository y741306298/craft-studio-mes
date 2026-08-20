package com.mes.application.command.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import com.mes.domain.order.enums.OrderChannelType;
import com.mes.infra.mq.LogisticsOrderInfo;
import com.mes.infra.mq.LogisticsOrderProducer;
import com.piliofpala.craftstudio.shared.infra.mq.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppPreOrderLabelTaskServiceMqTest {

    private static final String MANUFACTURER_META_ID = "69f956c00ff1ad90a9611464";
    private static final String ORDER_ID = "2090245888676642818";
    private static final String CHANNEL_ORDER_ID = "JY2608200002";
    private static final String LOGISTICS_ORDER_ID = "76969189136656";

    @Test
    void shouldSendLogisticsOrderMessageWithRequiredMqParameters() {
        LogisticsOrderProducer producer = mock(LogisticsOrderProducer.class);
        ObjectProvider<RocketMQTemplate> templateProvider = mock(ObjectProvider.class);
        when(templateProvider.getIfAvailable()).thenReturn(mock(RocketMQTemplate.class));
        AppPreOrderLabelTaskService service = service(producer, templateProvider);

        OrderInfo orderInfo = orderInfoFromWdtLabelRecord();

        String failureReason = notifyLogisticsOrderInfo(service, orderInfo, LOGISTICS_ORDER_ID);

        assertThat(failureReason).isNull();
        ArgumentCaptor<Message<LogisticsOrderInfo>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(messageCaptor.capture());
        Message<LogisticsOrderInfo> message = messageCaptor.getValue();
        assertThat(ReflectionTestUtils.getField(message, "topic")).isEqualTo("mes-logistics");
        assertThat(ReflectionTestUtils.getField(message, "tag")).isEqualTo(MANUFACTURER_META_ID);
        LogisticsOrderInfo payload = (LogisticsOrderInfo) ReflectionTestUtils.getField(message, "info");
        assertThat(payload.getOrderId()).isEqualTo(2090245888676642818L);
        assertThat(payload.getLogisticsOrderId()).isEqualTo(LOGISTICS_ORDER_ID);
    }

    @Test
    void shouldRejectMessageWhenRequiredParametersAreMissing() {
        LogisticsOrderProducer producer = mock(LogisticsOrderProducer.class);
        ObjectProvider<RocketMQTemplate> templateProvider = mock(ObjectProvider.class);
        when(templateProvider.getIfAvailable()).thenReturn(mock(RocketMQTemplate.class));
        AppPreOrderLabelTaskService service = service(producer, templateProvider);
        OrderInfo orderInfo = orderInfoFromWdtLabelRecord();
        orderInfo.setPlatformCode(null);

        assertThat(notifyLogisticsOrderInfo(service, orderInfo, LOGISTICS_ORDER_ID))
                .isEqualTo("MQ通知失败：platformCode为空");
        assertThat(notifyLogisticsOrderInfo(service, orderInfo, " "))
                .isEqualTo("MQ通知失败：kuaidiNum为空");
        verify(producer, never()).send(org.mockito.ArgumentMatchers.any());
    }

    /**
     * Rebuild the order-side fields that are available in the supplied WDT label record. The MQ
     * contract calls this value platformCode; in this flow the record's manufacturerMetaId is the
     * routing tag supplied by the caller.
     */
    private OrderInfo orderInfoFromWdtLabelRecord() {
        OrderChannelInfo channel = new OrderChannelInfo();
        channel.setType(OrderChannelType.GATHER_PLATFORM);
        channel.setCode("WDT");
        channel.setOrderId(CHANNEL_ORDER_ID);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(ORDER_ID);
        orderInfo.setManufacturerId(MANUFACTURER_META_ID);
        orderInfo.setPlatformCode(MANUFACTURER_META_ID);
        orderInfo.setChannel(channel);
        return orderInfo;
    }

    private AppPreOrderLabelTaskService service(LogisticsOrderProducer producer,
                                                 ObjectProvider<RocketMQTemplate> templateProvider) {
        AppPreOrderLabelTaskService service = new AppPreOrderLabelTaskService();
        ReflectionTestUtils.setField(service, "producer", producer);
        ReflectionTestUtils.setField(service, "rocketMQTemplateProvider", templateProvider);
        return service;
    }

    private String notifyLogisticsOrderInfo(AppPreOrderLabelTaskService service,
                                            OrderInfo orderInfo,
                                            String kuaidiNum) {
        return ReflectionTestUtils.invokeMethod(service, "notifyLogisticsOrderInfo", orderInfo, kuaidiNum);
    }
}
