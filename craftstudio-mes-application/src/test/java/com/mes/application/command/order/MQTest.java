package com.mes.application.command.order;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
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
class MQTest {
    private static final String TOPIC = "mes-logistics";

    @Test
    void shouldListenLogisticsOrderInfoMessage() throws Exception {
        String nameServer = System.getProperty("rocketmq.name-server");
        String tag = "test-platform-" + System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<String, Object>> receivedMessage = new AtomicReference<>();

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("mes-logistics-test-consumer-" + System.currentTimeMillis());
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(TOPIC, tag);
        MessageListenerConcurrently listener = (messages, context) -> {
            for (MessageExt message : messages) {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                receivedMessage.set(JSON.parseObject(body, Map.class));
                latch.countDown();
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        };
        consumer.registerMessageListener(listener);

        DefaultMQProducer producer = new DefaultMQProducer("mes-logistics-test-producer-" + System.currentTimeMillis());
        producer.setNamesrvAddr(nameServer);
        try {
            consumer.start();
            producer.start();

            LogisticsOrderInfo info = new LogisticsOrderInfo();
            info.setKuaidiNum("SF1234567890");
            info.setManufacturerMetaId("manufacturer-meta-001");
            info.setOrderId("channel-order-001");

            Message message = new Message(TOPIC, tag, JSON.toJSONString(baseMessage(TOPIC, tag, info)).getBytes(StandardCharsets.UTF_8));
            SendResult sendResult = producer.send(message);

            assertNotNull(sendResult);
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            Map<String, Object> mqMessage = receivedMessage.get();
            assertNotNull(mqMessage);
            assertEquals(TOPIC, mqMessage.get("topic"));
            assertEquals(tag, mqMessage.get("tag"));
            assertNotNull(mqMessage.get("info"));
        } finally {
            producer.shutdown();
            consumer.shutdown();
        }
    }

    static Map<String, Object> baseMessage(String topic, String tag, Object info) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("topic", topic);
        message.put("tag", tag);
        message.put("info", info);
        return message;
    }

    @lombok.Data
    static class LogisticsOrderInfo {
        private String kuaidiNum;
        private String manufacturerMetaId;
        private String orderId;
    }
}
