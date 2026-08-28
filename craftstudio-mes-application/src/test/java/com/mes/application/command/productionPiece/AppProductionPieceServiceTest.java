package com.mes.application.command.productionPiece;

import com.mes.application.dto.resp.productionpiece.DeleteProductionPieceVectorsResponse;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.infra.oss.ImageToImageSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppProductionPieceServiceTest {

    @Test
    void deletesProductionPieceVectorsCreatedBeforeDate() {
        ProductionPieceService pieceService = mock(ProductionPieceService.class);
        ImageToImageSearchService imageSearchService = mock(ImageToImageSearchService.class);
        AppProductionPieceService service = new AppProductionPieceService();
        ReflectionTestUtils.setField(service, "domainProductionPieceService", pieceService);
        ReflectionTestUtils.setField(service, "imageToImageSearchService", imageSearchService);

        when(pieceService.findCreatedBefore(any(), eq(1), eq(100)))
                .thenReturn(List.of(piece("piece-1"), piece("piece-2"), piece("")));
        when(imageSearchService.deleteImageVectors(List.of("piece-1", "piece-2"))).thenReturn(true);

        DeleteProductionPieceVectorsResponse result = service.deleteVectorsCreatedBefore(LocalDate.of(2024, 7, 8));

        assertThat(result.beforeDate()).isEqualTo("2024-07-08");
        assertThat(result.matchedPieceCount()).isEqualTo(3);
        assertThat(result.deletedVectorCount()).isEqualTo(2);
        verify(imageSearchService).deleteImageVectors(List.of("piece-1", "piece-2"));
    }

    private ProductionPiece piece(String productionPieceId) {
        ProductionPiece piece = new ProductionPiece();
        piece.setProductionPieceId(productionPieceId);
        return piece;
    }
}
