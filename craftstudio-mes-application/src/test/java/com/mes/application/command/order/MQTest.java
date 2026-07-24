package com.mes.application.command.order;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "rocketmq.name-server", matches = ".+",
        disabledReason = "需要通过 -Drocketmq.name-server=<host:port> 指定 RocketMQ broker 后才执行")
@EnableAutoConfiguration
@SpringBootTest(
        classes = MQTest.LogisticsOrderInfoTestListener.class,
        properties = {
                "rocketmq.name-server=${rocketmq.name-server}",
                "rocketmq.producer.group=mes-logistics-test-producer"
        })
class MQTest {
    private static final String TOPIC = "mes-logistics";
    private static final String TAG = "test-platform";

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Test
    void shouldListenLogisticsOrderInfoMessage() throws InterruptedException {
        LogisticsOrderInfo info = new LogisticsOrderInfo();
        info.setKuaidiNum("SF1234567890");
        info.setManufacturerMetaId("manufacturer-meta-001");
        info.setOrderId("channel-order-001");

        rocketMQTemplate.syncSend(TOPIC + ":" + TAG, baseMessage(TOPIC, TAG, info));

        assertTrue(LogisticsOrderInfoTestListener.LATCH.await(10, TimeUnit.SECONDS));
        Map<String, Object> message = LogisticsOrderInfoTestListener.MESSAGE.get();
        assertNotNull(message);
        assertEquals(TOPIC, message.get("topic"));
        assertEquals(TAG, message.get("tag"));
        assertNotNull(message.get("info"));
    }

    static Map<String, Object> baseMessage(String topic, String tag, Object info) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("topic", topic);
        message.put("tag", tag);
        message.put("info", info);
        return message;
    }

    @Component
    @RocketMQMessageListener(topic = TOPIC, consumerGroup = "mes-logistics-test-consumer", selectorExpression = TAG)
    static class LogisticsOrderInfoTestListener implements RocketMQListener<Map<String, Object>> {
        private static final CountDownLatch LATCH = new CountDownLatch(1);
        private static final AtomicReference<Map<String, Object>> MESSAGE = new AtomicReference<>();

        @Override
        public void onMessage(Map<String, Object> message) {
            MESSAGE.set(message);
            LATCH.countDown();
        }
    }

    @lombok.Data
    static class LogisticsOrderInfo {
        private String kuaidiNum;
        private String manufacturerMetaId;
        private String orderId;
    }
}
