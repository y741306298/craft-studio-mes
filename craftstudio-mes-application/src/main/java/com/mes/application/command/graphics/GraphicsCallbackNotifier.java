package com.mes.application.command.graphics;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.*;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Cross-instance wake-up only. MongoDB remains the authoritative callback inbox. */
@Component
public class GraphicsCallbackNotifier {
    private static final String CHANNEL="mes:graphics:callback";
    private final ConcurrentMap<String,CompletableFuture<Void>> waiting=new ConcurrentHashMap<>();
    private final StringRedisTemplate redis;
    private final RedisMessageListenerContainer listener;
    private final ExecutorService notifications=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(128),Thread.ofPlatform().name("graphics-callback-notification").factory(),new ThreadPoolExecutor.CallerRunsPolicy());
    public GraphicsCallbackNotifier(RedisConnectionFactory factory){
        redis=new StringRedisTemplate(factory);listener=new RedisMessageListenerContainer();listener.setConnectionFactory(factory);
        listener.setTaskExecutor(notifications);
        listener.addMessageListener((message,pattern)->signal(new String(message.getBody(),StandardCharsets.UTF_8)),new ChannelTopic(CHANNEL));
    }
    @PostConstruct public void start(){listener.afterPropertiesSet();listener.start();}
    public CompletableFuture<Void> listen(String taskId){return waiting.computeIfAbsent(taskId,key->new CompletableFuture<>());}
    public void release(String taskId,CompletableFuture<Void> signal){waiting.remove(taskId,signal);}
    public void publish(String taskId){signal(taskId);redis.convertAndSend(CHANNEL,taskId);}
    private void signal(String taskId){var future=waiting.get(taskId);if(future!=null)future.complete(null);}
    @PreDestroy public void close(){listener.stop();notifications.shutdownNow();waiting.values().forEach(f->f.completeExceptionally(new IllegalStateException("图形回调监听已停止")));waiting.clear();}
}
