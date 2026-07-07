package com.mes.application.command.orderPreprocessing;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 订单预处理任务队列。
 *
 * <p>职责：
 * 1. 接收订单项批次并快速入队；
 * 2. 后台单线程顺序消费，避免同一进程内并发踩踏；
 * 3. 支持失败重试与退避；
 * 4. 支持优雅停机时尽量消费完成。</p>
 */
@Component
public class OrderPreprocessTaskQueue {

    private static final Logger log = LoggerFactory.getLogger(OrderPreprocessTaskQueue.class);

    private final AppOrderPreprocessingService appOrderPreprocessingService;
    private BlockingQueue<OrderPreprocessTask> queue;
    private final ExecutorService workerExecutor;

    @Value("${order.preprocess.queue.capacity:1000}")
    private int queueCapacity;

    @Value("${order.preprocess.queue.max-retry:3}")
    private int maxRetry;

    @Value("${order.preprocess.queue.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    public OrderPreprocessTaskQueue(AppOrderPreprocessingService appOrderPreprocessingService) {
        this.appOrderPreprocessingService = appOrderPreprocessingService;
        this.workerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "order-preprocess-worker");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void start() {
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
        workerExecutor.submit(this::consumeLoop);
        log.info("订单预处理任务队列已启动: capacity={}, maxRetry={}, retryBackoffMs={}", queueCapacity, maxRetry, retryBackoffMs);
    }

    public void submit(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            log.info("订单预处理任务跳过入队: orderItems为空");
            return;
        }
        if (queue == null) {
            throw new IllegalStateException("订单预处理任务队列尚未初始化");
        }
        try {
            queue.put(new OrderPreprocessTask(orderItems, 0));
            log.info("订单预处理任务已入队: itemCount={}, queueSize={}", orderItems.size(), queue.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("订单预处理任务入队被中断", e);
        }
    }

    private void consumeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                OrderPreprocessTask task = queue.poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }
                log.info("订单预处理任务开始消费: itemCount={}, retry={}, queueSize={}", task.getOrderItems().size(), task.getRetryCount(), queue.size());
                handleTask(task);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log.error("订单预处理任务队列消费异常", ex);
            }
        }
    }

    private void handleTask(OrderPreprocessTask task) {
        try {
            List<OrderItem> readyOrderItems = appOrderPreprocessingService.convertMaskGrayImgToSvgIfNecessary(task.getOrderItems());
            log.info("订单预处理任务灰度图转换完成: originalItemCount={}, readyItemCount={}", task.getOrderItems().size(), readyOrderItems == null ? 0 : readyOrderItems.size());
            appOrderPreprocessingService.preprocessOrder(readyOrderItems);
            log.info("订单预处理任务处理完成: itemCount={}", readyOrderItems == null ? 0 : readyOrderItems.size());
        } catch (Exception ex) {
            int nextRetry = task.getRetryCount() + 1;
            if (nextRetry <= maxRetry) {
                sleepQuietly(retryBackoffMs * nextRetry);
                try {
                    queue.put(new OrderPreprocessTask(task.getOrderItems(), nextRetry));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
                log.warn("订单预处理任务失败，已重试入队: retry={}, itemCount={}, err={}", nextRetry, task.getOrderItems().size(), ex.getMessage(), ex);
                return;
            }
            log.error("订单预处理任务失败且超过最大重试次数，task dropped: itemCount={}", task.getOrderItems().size(), ex);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void stop() {
        workerExecutor.shutdown();
        try {
            if (!workerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                workerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class OrderPreprocessTask {
        private final List<OrderItem> orderItems;
        private final int retryCount;

        private OrderPreprocessTask(List<OrderItem> orderItems, int retryCount) {
            this.orderItems = orderItems;
            this.retryCount = retryCount;
        }

        public List<OrderItem> getOrderItems() {
            return orderItems;
        }

        public int getRetryCount() {
            return retryCount;
        }
    }
}
