package com.mes.application.command.orderPreprocessing;

import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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

    @Autowired
    private OrderItemService orderItemService;
    private BlockingQueue<OrderPreprocessTask> queue;
    private final Set<String> queuedOrderItemIds = ConcurrentHashMap.newKeySet();
    private final ExecutorService workerExecutor;

    @Value("${order.preprocess.queue.capacity:1000}")
    private int queueCapacity;

    @Value("${order.preprocess.queue.max-retry:3}")
    private int maxRetry;

    @Value("${order.preprocess.queue.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    @Value("${order.preprocess.queue.batch-size:100}")
    private int batchSize;

    @Value("${order.preprocess.queue.pending-recovery-age-ms:1800000}")
    private long pendingRecoveryAgeMs;

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
        List<OrderItem> newOrderItems = orderItems.stream()
                .filter(item -> item != null && item.getOrderItemId() != null)
                .filter(item -> queuedOrderItemIds.add(item.getOrderItemId()))
                .toList();
        int effectiveBatchSize = Math.max(1, batchSize);
        for (int fromIndex = 0; fromIndex < newOrderItems.size(); fromIndex += effectiveBatchSize) {
            int toIndex = Math.min(fromIndex + effectiveBatchSize, newOrderItems.size());
            // Copy the view so that a caller changing its list cannot corrupt a queued task.
            List<OrderItem> batch = new ArrayList<>(newOrderItems.subList(fromIndex, toIndex));
            try {
                queue.put(new OrderPreprocessTask(batch, 0));
                log.info("订单预处理任务已入队: batchItemCount={}, totalItemCount={}, queueSize={}",
                        batch.size(), newOrderItems.size(), queue.size());
            } catch (InterruptedException e) {
                newOrderItems.subList(fromIndex, newOrderItems.size())
                        .forEach(item -> queuedOrderItemIds.remove(item.getOrderItemId()));
                Thread.currentThread().interrupt();
                throw new IllegalStateException("订单预处理任务入队被中断", e);
            }
        }
    }

    /**
     * The local queue is deliberately lightweight and is lost on a process restart. Pending order
     * items are the durable source of truth, so periodically put stale ones back into the queue.
     * Always query page one: processing changes the status and therefore shrinks this result set;
     * incrementing the page would skip records.
     */
    @Scheduled(fixedDelayString = "${order.preprocess.queue.pending-recovery-interval-ms:60000}")
    public void recoverStalePendingItems() {
        if (queue == null) {
            return;
        }
        Map<String, Object> filters = new HashMap<>();
        filters.put("status", OrderStatus.PENDING.getCode());
        filters.put("updateTime_lte", new Date(System.currentTimeMillis() - Math.max(0, pendingRecoveryAgeMs)));
        List<OrderItem> pendingItems = orderItemService.filterList(1, Math.min(100, Math.max(1, batchSize)), filters);
        if (pendingItems == null || pendingItems.isEmpty()) {
            return;
        }
        log.warn("发现长时间待处理订单项，重新提交预处理: itemCount={}", pendingItems.size());
        submit(pendingItems);
    }

    private void consumeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                OrderPreprocessTask task = queue.poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }
                log.info("订单预处理任务开始消费: itemCount={}, retry={}, queueSize={}", task.getOrderItems().size(), task.getRetryCount(), queue.size());
                if (handleTask(task)) {
                    task.getOrderItems().forEach(item -> queuedOrderItemIds.remove(item.getOrderItemId()));
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log.error("订单预处理任务队列消费异常", ex);
            }
        }
    }

    private boolean handleTask(OrderPreprocessTask task) {
        try {
            List<OrderItem> readyOrderItems = appOrderPreprocessingService.convertMaskGrayImgToSvgIfNecessary(task.getOrderItems());
            log.info("订单预处理任务灰度图转换完成: originalItemCount={}, readyItemCount={}", task.getOrderItems().size(), readyOrderItems == null ? 0 : readyOrderItems.size());
            appOrderPreprocessingService.preprocessOrder(readyOrderItems);
            log.info("订单预处理任务处理完成: itemCount={}", readyOrderItems == null ? 0 : readyOrderItems.size());
            return true;
        } catch (Exception ex) {
            List<OrderItem> failedOrderItems = failedOrderItems(task, ex);
            Set<String> failedOrderItemIds = failedOrderItems.stream()
                    .map(OrderItem::getOrderItemId)
                    .collect(java.util.stream.Collectors.toSet());
            task.getOrderItems().stream()
                    .filter(item -> item != null && !failedOrderItemIds.contains(item.getOrderItemId()))
                    .forEach(item -> queuedOrderItemIds.remove(item.getOrderItemId()));
            int nextRetry = task.getRetryCount() + 1;
            if (nextRetry <= maxRetry) {
                sleepQuietly(retryBackoffMs * nextRetry);
                try {
                    queue.put(new OrderPreprocessTask(failedOrderItems, nextRetry));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return true;
                }
                log.warn("订单预处理任务失败，已重试入队: retry={}, itemCount={}, err={}", nextRetry, failedOrderItems.size(), ex.getMessage(), ex);
                return false;
            }
            markAsPermanentlyFailed(failedOrderItems, ex);
            log.error("订单预处理任务失败且超过最大重试次数，已标记为永久失败: itemCount={}", failedOrderItems.size(), ex);
            return true;
        }
    }

    private List<OrderItem> failedOrderItems(OrderPreprocessTask task, Exception failure) {
        if (!(failure instanceof OrderPreprocessBatchException batchFailure)) {
            return task.getOrderItems();
        }
        Set<String> failedIds = Set.copyOf(batchFailure.getFailedOrderItemIds());
        return task.getOrderItems().stream()
                .filter(item -> item != null && failedIds.contains(item.getOrderItemId()))
                .toList();
    }

    private void markAsPermanentlyFailed(List<OrderItem> orderItems, Exception failure) {
        String reason = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
        for (OrderItem orderItem : orderItems) {
            if (orderItem == null || orderItem.getOrderItemId() == null) {
                continue;
            }
            try {
                orderItemService.markAsFailed(orderItem.getOrderItemId(), reason);
            } catch (Exception markException) {
                log.error("订单预处理任务标记永久失败异常: orderItemId={}", orderItem.getOrderItemId(), markException);
            }
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
