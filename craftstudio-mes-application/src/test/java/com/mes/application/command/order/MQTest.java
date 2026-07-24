package com.mes.application.command.order;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MQTest {
    private static final String TOPIC = "mes-logistics";

    @Test
    void shouldListenLogisticsOrderInfoMessage() throws Exception {
        String nameServer = resolveNameServer();
        Assumptions.assumeTrue(nameServer != null && !nameServer.isBlank(),
                "需要通过 -Drocketmq.name-server=<host:port> 或 application-dev.yml/application.yml 配置 rocketmq.name-server 后才执行");
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

    private String resolveNameServer() throws Exception {
        String nameServer = System.getProperty("rocketmq.name-server");
        if (nameServer != null && !nameServer.isBlank()) {
            return nameServer;
        }
        nameServer = System.getenv("ROCKETMQ_NAME_SERVER");
        if (nameServer != null && !nameServer.isBlank()) {
            return nameServer;
        }
        String propertyName = "rocketmq.name-server";
        Properties properties = loadYamlProperties("application-dev.yml");
        nameServer = properties == null ? null : properties.getProperty(propertyName);
        if (nameServer != null && !nameServer.isBlank()) {
            return nameServer;
        }
        properties = loadYamlProperties("application-dev.yaml");
        nameServer = properties == null ? null : properties.getProperty(propertyName);
        if (nameServer != null && !nameServer.isBlank()) {
            return nameServer;
        }
        properties = loadYamlProperties("application.yml");
        nameServer = properties == null ? null : properties.getProperty(propertyName);
        if (nameServer != null && !nameServer.isBlank()) {
            return nameServer;
        }
        properties = loadYamlProperties("application.yaml");
        nameServer = properties == null ? null : properties.getProperty(propertyName);
        if (nameServer != null && !nameServer.isBlank()) {
            return nameServer;
        }
        Resource applicationProperties = findConfigResource("application.properties");
        if (applicationProperties == null || !applicationProperties.exists()) {
            return null;
        }
        properties = PropertiesLoaderUtils.loadProperties(applicationProperties);
        return properties.getProperty(propertyName);
    }

    private Properties loadYamlProperties(String location) throws Exception {
        Resource yaml = findConfigResource(location);
        if (yaml == null || !yaml.exists()) {
            return null;
        }
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(yaml);
        return yamlPropertiesFactoryBean.getObject();
    }

    private Resource findConfigResource(String location) throws Exception {
        ClassPathResource classPathResource = new ClassPathResource(location);
        if (classPathResource.exists()) {
            return classPathResource;
        }
        Path[] fileSystemPaths = new Path[] {
                Path.of(location),
                Path.of("src/main/resources", location),
                Path.of("src/test/resources", location),
                Path.of("craftstudio-mes-application/src/main/resources", location),
                Path.of("craftstudio-mes-application/src/test/resources", location),
                Path.of("craftstudio-mes-interfaces/src/main/resources", location),
                Path.of("craftstudio-mes-interfaces/src/test/resources", location)
        };
        for (Path fileSystemPath : fileSystemPaths) {
            FileSystemResource fileSystemResource = new FileSystemResource(fileSystemPath);
            if (fileSystemResource.exists()) {
                return fileSystemResource;
            }
        }
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path matched = findConfigResourceUnder(current, location);
            if (matched != null) {
                return new FileSystemResource(matched);
            }
            current = current.getParent();
        }
        return null;
    }

    private Path findConfigResourceUnder(Path root, String location) throws Exception {
        try (Stream<Path> paths = Files.find(root, 6,
                (path, attributes) -> attributes.isRegularFile() && path.getFileName().toString().equals(location))) {
            return paths.findFirst().orElse(null);
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
