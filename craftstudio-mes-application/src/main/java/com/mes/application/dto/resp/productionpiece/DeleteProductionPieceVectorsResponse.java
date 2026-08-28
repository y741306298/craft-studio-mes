package com.mes.application.dto.resp.productionpiece;

public record DeleteProductionPieceVectorsResponse(
        String beforeDate,
        int matchedPieceCount,
        int deletedVectorCount
) {
}
