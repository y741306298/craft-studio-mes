package com.mes.infra.dal.manufacurer.ProductionPiece;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.repository.ProductionPieceRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.ProductionPiece.po.ProductionPiecePo;
import org.springframework.stereotype.Repository;

@Repository
public class ProductionPieceRepositoryImp extends BaseRepositoryImp<ProductionPiece, ProductionPiecePo> implements ProductionPieceRepository {

    @Override
    public Class<ProductionPiecePo> poClass() {
        return ProductionPiecePo.class;
    }
}
