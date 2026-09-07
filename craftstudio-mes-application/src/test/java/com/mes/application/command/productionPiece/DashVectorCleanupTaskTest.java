package com.mes.application.command.productionPiece;

import com.mes.application.dto.resp.productionpiece.DeleteProductionPieceVectorsResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashVectorCleanupTaskTest {

    @Test
    void deletesVectorsCreatedExactlyFifteenDaysBeforeBeijingToday() {
        AppProductionPieceService service = mock(AppProductionPieceService.class);
        DashVectorCleanupTask task = new DashVectorCleanupTask(service);
        Clock clock = Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), DashVectorCleanupTask.BEIJING_ZONE);
        LocalDate cleanupDate = LocalDate.of(2026, 8, 24);
        when(service.deleteVectorsCreatedBetween(cleanupDate, cleanupDate))
                .thenReturn(new DeleteProductionPieceVectorsResponse("2026-08-24", "2026-08-24", 2, 2));

        task.deleteExpiredVectors(clock);

        verify(service).deleteVectorsCreatedBetween(cleanupDate, cleanupDate);
    }
}
