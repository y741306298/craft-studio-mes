package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.ReleaseLayoutResult;
import com.mes.domain.manufacturer.productionPiece.entity.PieceQuantityTransfer;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReleaseLayoutTimeoutTest {
    private static final String RECORD_ID = "record-1";
    private static final String TYPESETTING_ID = "layout-1";

    private final AppTypesettingService service = new AppTypesettingService();
    private final TypesettingService layouts = mock(TypesettingService.class);
    private final ProductionPieceService pieces = mock(ProductionPieceService.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "domainTypesettingService", layouts);
        ReflectionTestUtils.setField(service, "productionPieceService", pieces);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void releasesExpiredNestingAndRollsBackReservedPieces(boolean hasUpdateTime) {
        TypesettingInfo layout = stubLayout(TypesettingStatus.IN_PROGRESS, hasUpdateTime ? 6 : null);
        TypesettingSourceCell cell = new TypesettingSourceCell();
        cell.setSourceType(TypesettingSourceType.PART.getCode());
        cell.setSourceId("piece-1");
        cell.setQuantity(2);
        layout.setTypesettingCells(List.of(cell));
        stubRedisLocks();

        ReleaseLayoutResult result = service.releaseLayout(List.of(RECORD_ID));

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals(List.of(RECORD_ID), result.getDeletedLayoutIds());
        var operations = inOrder(pieces, layouts);
        operations.verify(pieces).transferPieceQuantitiesBetweenNodesStrict(List.of(
                new PieceQuantityTransfer("piece-1", "NODE_TYPESETTING_IN_PROGRESS", "NODE_TYPESETTING", 2)));
        operations.verify(layouts).deleteTypesetting(RECORD_ID);
    }

    @Test
    void rejectsRecentNestingEvenWhenTheRecordWasCreatedBeforeTheCutoff() {
        stubLayout(TypesettingStatus.IN_PROGRESS, 4);
        stubRedisLocks();

        assertThrows(BusinessNotAllowException.class, () -> service.releaseLayout(List.of(RECORD_ID)));
        verifyNoInteractions(pieces);
        verify(layouts, never()).deleteTypesetting(anyString());
    }

    @Test
    void rejectsExpiredGraphicsPreparation() {
        stubLayout(TypesettingStatus.LAYOUT_PROCESSING, 6);
        stubRedisLocks();

        assertThrows(BusinessNotAllowException.class, () -> service.releaseLayout(List.of(RECORD_ID)));
        verifyNoInteractions(pieces);
        verify(layouts, never()).deleteTypesetting(anyString());
    }

    @Test
    void doesNotReleaseWhileTheNestingCallbackHoldsItsLock() {
        stubLayout(TypesettingStatus.IN_PROGRESS, 6);
        ValueOperations<String, Object> redisValues = stubRedisLocks();
        when(redisValues.setIfAbsent(eq("typesetting:operation:lock:nestingCallback:" + TYPESETTING_ID),
                anyString(), anyLong(), eq(TimeUnit.MINUTES))).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.releaseLayout(List.of(RECORD_ID)));
        verifyNoInteractions(pieces);
        verify(layouts, never()).deleteTypesetting(anyString());
    }

    private TypesettingInfo stubLayout(TypesettingStatus status, Integer updatedMinutesAgo) {
        Instant now = Instant.now();
        TypesettingInfo layout = new TypesettingInfo();
        layout.setId(RECORD_ID);
        layout.setTypesettingId(TYPESETTING_ID);
        layout.setStatus(status.getCode());
        layout.setCreateTime(Date.from(now.minus(10, ChronoUnit.MINUTES)));
        if (updatedMinutesAgo != null) {
            layout.setUpdateTime(Date.from(now.minus(updatedMinutesAgo, ChronoUnit.MINUTES)));
        }
        when(layouts.findById(RECORD_ID)).thenReturn(layout);
        return layout;
    }

    @SuppressWarnings("unchecked")
    private ValueOperations<String, Object> stubRedisLocks() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> redisValues = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(redisValues);
        when(redisValues.setIfAbsent(anyString(), any(), anyLong(), eq(TimeUnit.MINUTES))).thenReturn(true);
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        return redisValues;
    }
}
