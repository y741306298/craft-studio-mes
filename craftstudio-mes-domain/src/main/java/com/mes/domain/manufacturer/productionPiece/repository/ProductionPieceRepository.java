package com.mes.domain.manufacturer.productionPiece.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;

public interface ProductionPieceRepository extends BaseRepository<ProductionPiece> {
    
    /**
     * 根据 productionPieceId 更新生产工件
     * @param productionPiece 生产工件对象（包含 productionPieceId）
     */
    void updateByProductionPieceId(ProductionPiece productionPiece);
}
