package com.mes.application.command.delivery;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryPkgTraceTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(DeliveryPkgTrace.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void captureLogs() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachLogs() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void controllerAndServiceShareTraceAndSuccessFollowsResponseConstruction() {
        try (DeliveryPkgTrace controller = DeliveryPkgTrace.open(request())) {
            try (DeliveryPkgTrace service = DeliveryPkgTrace.open(request())) {
                DeliveryPkgTrace.run("查询生产件", () -> {});
                service.businessComplete("pkg-1");
            }
            DeliveryPkgTrace.run("构建打印响应", () -> {});
            controller.success("pkg-1");
        }
        List<String> messages = messages();
        assertEquals(1, messages.stream().filter(m -> m.startsWith("addPkg开始 ")).count());
        assertEquals(1, messages.stream().filter(m -> m.startsWith("addPkg成功 ")).count());
        assertTrue(messages.getLast().startsWith("addPkg成功 orderId=order-1 "));
        assertTrue(messages.getLast().contains("pkgId=pkg-1 totalMs="));
        assertTrue(messages.get(messages.size() - 2).contains("stage=构建打印响应 elapsedMs="));
        assertEquals(1, messages.stream().map(this::traceId).distinct().count());
        int count = messages.size();
        DeliveryPkgTrace.run("scope已关闭", () -> {});
        assertEquals(count, messages().size());
    }

    @Test
    void failureRetainsDeepestStageAndOriginalExceptionAndDoesNotLeakToNextRequest() {
        RuntimeException failure = new IllegalStateException("failed");
        try (DeliveryPkgTrace trace = DeliveryPkgTrace.open(request())) {
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> DeliveryPkgTrace.run("更新打包状态", () ->
                            DeliveryPkgTrace.run("批量更新生产件", () -> { throw failure; })));
            assertSame(failure, thrown);
            trace.failure(thrown);
        }
        String failed = messages().getLast();
        assertTrue(failed.startsWith("addPkg失败 "));
        assertTrue(failed.contains("stage=批量更新生产件 totalMs="));
        assertSame(failure, appender.list.getLast().getThrowableProxy() instanceof
                ch.qos.logback.classic.spi.ThrowableProxy proxy ? proxy.getThrowable() : null);
        assertFalse(messages().stream().anyMatch(m -> m.startsWith("addPkg成功 ")));
        String failedTraceId = traceId(failed);
        try (DeliveryPkgTrace next = DeliveryPkgTrace.open(request())) {
            next.success("pkg-2");
        }
        assertNotEquals(failedTraceId, traceId(messages().getLast()));
    }

    @Test
    void responseFailureIncludesAlreadyCreatedPackage() {
        try (DeliveryPkgTrace trace = DeliveryPkgTrace.open(request())) {
            try (DeliveryPkgTrace service = DeliveryPkgTrace.open(request())) {
                service.businessComplete("pkg-created");
            }
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> DeliveryPkgTrace.run("构建打印响应", () -> { throw new IllegalStateException(); }));
            trace.failure(failure);
        }
        assertTrue(messages().getLast().contains("pkgId=pkg-created stage=构建打印响应"));
        assertFalse(messages().stream().anyMatch(m -> m.startsWith("addPkg成功 ")));
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private String traceId(String message) {
        return message.split("traceId=", 2)[1].split(" ", 2)[0];
    }

    private DeliveryPkgAddRequest request() {
        DeliveryPkgPieceVO piece = new DeliveryPkgPieceVO();
        piece.setOrderId("order-1");
        var item = new DeliveryPkgAddRequest.DeliveryPkgPieceItem();
        item.setPiece(piece);
        var request = new DeliveryPkgAddRequest();
        request.setPieces(List.of(item));
        return request;
    }
}
