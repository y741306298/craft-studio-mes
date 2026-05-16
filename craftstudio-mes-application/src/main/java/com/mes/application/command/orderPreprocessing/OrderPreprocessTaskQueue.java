package com.mes.application.command.orderPreprocessing;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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

    private final AppOrderPreprocessingService appOrderPreprocessingService;
    private final BlockingQueue<OrderPreprocessTask> queue;
    private final ExecutorService workerExecutor;

    @Value("${order.preprocess.queue.capacity:1000}")
    private int queueCapacity;

    @Value("${order.preprocess.queue.max-retry:3}")
    private int maxRetry;

    @Value("${order.preprocess.queue.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    public OrderPreprocessTaskQueue(AppOrderPreprocessingService appOrderPreprocessingService) {
        this.appOrderPreprocessingService = appOrderPreprocessingService;
        this.queue = new LinkedBlockingQueue<>();
        this.workerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "order-preprocess-worker");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void start() {
        workerExecutor.submit(this::consumeLoop);
    }

    public void submit(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }
        if (queue.size() >= queueCapacity) {
            throw new IllegalStateException("订单预处理队列已满，请稍后重试");
        }
        queue.offer(new OrderPreprocessTask(orderItems, 0));
    }

    private void consumeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                OrderPreprocessTask task = queue.poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }
                handleTask(task);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                System.err.println("订单预处理任务队列消费异常: " + ex.getMessage());
            }
        }
    }

    private void handleTask(OrderPreprocessTask task) {
        try {
            appOrderPreprocessingService.preprocessOrder(task.getOrderItems());
        } catch (Exception ex) {
            int nextRetry = task.getRetryCount() + 1;
            if (nextRetry <= maxRetry) {
                sleepQuietly(retryBackoffMs * nextRetry);
                queue.offer(new OrderPreprocessTask(task.getOrderItems(), nextRetry));
                System.err.println("订单预处理任务失败，已重试入队，retry=" + nextRetry + ", err=" + ex.getMessage());
                return;
            }
            System.err.println("订单预处理任务失败且超过最大重试次数，task dropped, err=" + ex.getMessage());
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
