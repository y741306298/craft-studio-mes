package com.mes.application.command.productionPiece;

import com.mes.application.dto.resp.productionpiece.DeleteProductionPieceVectorsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 定期清理达到保留期限的生产工件图片向量，不删除生产工件本身。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashVectorCleanupTask {

    static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    static final int RETENTION_DAYS = 15;

    private final AppProductionPieceService appProductionPieceService;

    /**
     * 每天北京时间 04:00 删除恰好在 15 天前创建的生产工件对应的 DashVector Doc。
     */
    @Scheduled(cron = "0 0 4 * * ?", zone = "Asia/Shanghai")
    public void deleteExpiredVectors() {
        deleteExpiredVectors(Clock.system(BEIJING_ZONE));
    }

    void deleteExpiredVectors(Clock clock) {
        LocalDate cleanupDate = LocalDate.now(clock).minusDays(RETENTION_DAYS);
        DeleteProductionPieceVectorsResponse result =
                appProductionPieceService.deleteVectorsCreatedBetween(cleanupDate, cleanupDate);
        log.info("DashVector 定时清理完成: cleanupDate={}, matchedPieceCount={}, deletedVectorCount={}",
                cleanupDate, result.matchedPieceCount(), result.deletedVectorCount());
    }
}
