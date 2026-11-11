package com.mes.application.command.orderPreprocessing.sublimation;

import com.mes.domain.manufacturer.typesetting.enums.TypesettingSequenceUsageType;
import com.mes.domain.manufacturer.typesetting.service.TypesettingSequencePoolService;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.entity.SublimationTraceOrder;
import com.mes.domain.order.orderInfo.repository.SublimationTraceOrderRepository;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SublimationTraceService {
    private final TypesettingSequencePoolService sequences;
    private final SublimationTraceOrderRepository traces;
    private final OrderInfoService orders;
    private final OrderItemService items;
    private final RedisTemplate<String, Object> redis;
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    public SublimationTraceService(TypesettingSequencePoolService sequences, SublimationTraceOrderRepository traces,
                                  OrderInfoService orders, OrderItemService items, RedisTemplate<String, Object> redis) {
        this.sequences = sequences; this.traces = traces; this.orders = orders; this.items = items; this.redis = redis;
    }

    public SublimationTraceIdentity resolve(OrderItem item) {
        if (item.getOrderId() == null || item.getManufacturerId() == null || item.getOrderItemId() == null
                || item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new IllegalArgumentException("热升华溯源号缺少订单、工厂、订单项或有效数量");
        }
        // 按工厂串行分配新用途的循环号，跨实例也不会同时读写同一序号池。
        String key = "orderPreprocessing:sublimationTrace:lock:" + item.getManufacturerId();
        String token = UUID.randomUUID().toString();
        acquire(key, token);
        try {
            if (orders.findByOrderIdAndManufacturerId(item.getOrderId(), item.getManufacturerId()) == null) {
                throw new IllegalArgumentException("热升华溯源订单不存在：" + item.getOrderId());
            }
            String id = "sublimation-trace-" + UUID.nameUUIDFromBytes(
                    (item.getManufacturerId() + "\u0000" + item.getOrderId()).getBytes(StandardCharsets.UTF_8));
            SublimationTraceOrder trace = traces.findById(id);
            // 该查询按createTime、_id升序，包含订单全部订单项，不仅热升华订单项。
            List<String> current = items.findAllByOrderId(item.getOrderId()).stream()
                    .filter(i -> Objects.equals(i.getManufacturerId(), item.getManufacturerId()))
                    .map(OrderItem::getOrderItemId).filter(Objects::nonNull).distinct().toList();
            List<String> ids = SublimationTraceIdentity.mergeItemIds(trace == null ? null : trace.getOrderItemIds(), current);
            int y = ids.indexOf(item.getOrderItemId()) + 1;
            if (!current.contains(item.getOrderItemId()) || y == 0) throw new IllegalArgumentException("当前订单项不属于热升华溯源订单");
            if (trace == null) {
                trace = new SublimationTraceOrder();
                trace.setId(id);
                trace.setOrderId(item.getOrderId());
                trace.setManufacturerId(item.getManufacturerId());
                trace.setSequence(sequences.nextSequence(item.getManufacturerId(), TypesettingSequenceUsageType.SUBLIMATION_TRACE));
                trace.setOrderItemIds(ids);
                traces.add(trace);
            } else if (!ids.equals(trace.getOrderItemIds())) {
                trace.setOrderItemIds(ids);
                traces.update(trace);
            }
            return new SublimationTraceIdentity(trace.getSequence(), y, ids.size(), item.getQuantity());
        } finally {
            redis.execute(RELEASE, List.of(key), token);
        }
    }

    private void acquire(String key, String token) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        do {
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, token, 1, TimeUnit.MINUTES))) return;
            try { Thread.sleep(50); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("等待热升华溯源号分配被中断", e); }
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("热升华溯源号正在分配，请重试");
    }
}
