package com.mes.application.command.delivery;

import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Synchronous addPkg tracing. Nested service calls share the controller's scope. */
@Slf4j
public final class DeliveryPkgTrace implements AutoCloseable {
    private static final ThreadLocal<DeliveryPkgTrace> CURRENT = new ThreadLocal<>();
    private final DeliveryPkgTrace root;
    private final boolean owner;
    private final String traceId;
    private final String orderId;
    private final long startedAt;
    private String failedStage;
    private String pkgId;

    private DeliveryPkgTrace(DeliveryPkgAddRequest request) {
        DeliveryPkgTrace active = CURRENT.get();
        owner = active == null;
        root = owner ? this : active;
        if (owner) {
            traceId = UUID.randomUUID().toString();
            var pieces = request == null ? null : request.getPieces();
            var first = pieces == null ? null : pieces.stream().filter(Objects::nonNull)
                    .map(DeliveryPkgAddRequest.DeliveryPkgPieceItem::getPiece)
                    .filter(Objects::nonNull).findFirst().orElse(null);
            orderId = first == null ? null : first.getOrderId();
            startedAt = System.nanoTime();
            CURRENT.set(this);
            log.info("addPkg开始 orderId={} traceId={} presetType={} pieceCount={}", orderId, traceId,
                    first == null || first.getLogisticsCarrierInfo() == null ? null
                            : first.getLogisticsCarrierInfo().getPresetType(), pieces == null ? 0 : pieces.size());
        } else {
            traceId = active.traceId;
            orderId = active.orderId;
            startedAt = active.startedAt;
        }
    }

    public static DeliveryPkgTrace open(DeliveryPkgAddRequest request) {
        return new DeliveryPkgTrace(request);
    }

    public static <T> T stage(String stage, Supplier<T> action) {
        DeliveryPkgTrace trace = CURRENT.get();
        if (trace == null) {
            return action.get();
        }
        long start = System.nanoTime();
        log.info("addPkg阶段开始 orderId={} traceId={} stage={}", trace.orderId, trace.traceId, stage);
        try {
            T result = action.get();
            log.info("addPkg阶段完成 orderId={} traceId={} stage={} elapsedMs={}",
                    trace.orderId, trace.traceId, stage, elapsedMs(start));
            return result;
        } catch (RuntimeException | Error ex) {
            if (trace.failedStage == null) {
                trace.failedStage = stage;
            }
            log.warn("addPkg阶段失败 orderId={} traceId={} stage={} elapsedMs={} exceptionType={}",
                    trace.orderId, trace.traceId, stage, elapsedMs(start), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    public static void run(String stage, Runnable action) {
        stage(stage, () -> {
            action.run();
            return null;
        });
    }

    public static void packageSaved(String pkgId) {
        DeliveryPkgTrace trace = CURRENT.get();
        if (trace != null) {
            trace.pkgId = pkgId;
            log.info("addPkg包裹已保存 orderId={} traceId={} pkgId={} totalMs={}",
                    trace.orderId, trace.traceId, pkgId, elapsedMs(trace.startedAt));
        }
    }

    public void businessComplete(String pkgId) {
        root.pkgId = pkgId;
        log.info("addPkg业务完成 orderId={} traceId={} pkgId={} totalMs={}",
                orderId, traceId, pkgId, elapsedMs(startedAt));
        if (owner) {
            success(pkgId);
        }
    }

    public void success(String pkgId) {
        root.pkgId = pkgId;
        log.info("addPkg成功 orderId={} traceId={} pkgId={} totalMs={}",
                orderId, traceId, pkgId, elapsedMs(startedAt));
    }

    public void failure(Throwable ex) {
        if (owner) {
            log.error("addPkg失败 orderId={} traceId={} pkgId={} stage={} totalMs={} exceptionType={}",
                    orderId, traceId, root.pkgId, root.failedStage == null ? "业务处理" : root.failedStage,
                    elapsedMs(startedAt), ex.getClass().getSimpleName(), ex);
        }
    }

    private static double elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000.0;
    }

    @Override
    public void close() {
        if (owner) {
            CURRENT.remove();
        }
    }
}
