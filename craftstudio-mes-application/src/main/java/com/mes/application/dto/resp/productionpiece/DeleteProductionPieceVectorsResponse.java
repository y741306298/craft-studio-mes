package com.mes.application.dto.resp.productionpiece;

public record DeleteProductionPieceVectorsResponse(
        String startDate,
        String endDate,
        int matchedPieceCount,
        int deletedVectorCount
) {
}
