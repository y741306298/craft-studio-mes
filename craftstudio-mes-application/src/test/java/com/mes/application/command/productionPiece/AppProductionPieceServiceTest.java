package com.mes.application.command.productionPiece;

import com.mes.application.dto.resp.productionpiece.DeleteProductionPieceVectorsResponse;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.infra.oss.ImageToImageSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppProductionPieceServiceTest {

    @Test
    void deletesVectorsCreatedDuringInclusiveDateRange() {
        ProductionPieceService pieceService = mock(ProductionPieceService.class);
        ImageToImageSearchService imageSearchService = mock(ImageToImageSearchService.class);
        AppProductionPieceService service = new AppProductionPieceService();
        ReflectionTestUtils.setField(service, "domainProductionPieceService", pieceService);
        ReflectionTestUtils.setField(service, "imageToImageSearchService", imageSearchService);

        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 7);
        ZoneId zoneId = ZoneId.systemDefault();
        Date startTime = Date.from(startDate.atStartOfDay(zoneId).toInstant());
        Date endExclusive = Date.from(endDate.plusDays(1).atStartOfDay(zoneId).toInstant());
        List<ProductionPiece> pieces = List.of(piece("piece-1"), piece("piece-2"));

        when(pieceService.findCreatedBetween(eq(startTime), eq(endExclusive), eq(1), eq(100)))
                .thenReturn(pieces);
        when(imageSearchService.deleteImageVectors(List.of("piece-1", "piece-2"))).thenReturn(2);

        DeleteProductionPieceVectorsResponse result =
                service.deleteVectorsCreatedBetween(startDate, endDate);

        assertThat(result.startDate()).isEqualTo("2026-08-01");
        assertThat(result.endDate()).isEqualTo("2026-08-07");
        assertThat(result.matchedPieceCount()).isEqualTo(2);
        assertThat(result.deletedVectorCount()).isEqualTo(2);
        verify(pieceService).findCreatedBetween(startTime, endExclusive, 1, 100);
        verify(imageSearchService).deleteImageVectors(List.of("piece-1", "piece-2"));
    }

    private static ProductionPiece piece(String id) {
        ProductionPiece piece = new ProductionPiece();
        piece.setProductionPieceId(id);
        return piece;
    }
}
