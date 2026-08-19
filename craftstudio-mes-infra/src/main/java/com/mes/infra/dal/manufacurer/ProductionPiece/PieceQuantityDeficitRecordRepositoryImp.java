package com.mes.infra.dal.manufacurer.ProductionPiece;

import com.mes.domain.manufacturer.productionPiece.entity.PieceQuantityDeficitRecord;
import com.mes.domain.manufacturer.productionPiece.repository.PieceQuantityDeficitRecordRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.ProductionPiece.po.PieceQuantityDeficitRecordPo;
import org.springframework.stereotype.Repository;

@Repository
public class PieceQuantityDeficitRecordRepositoryImp
        extends BaseRepositoryImp<PieceQuantityDeficitRecord, PieceQuantityDeficitRecordPo>
        implements PieceQuantityDeficitRecordRepository {
    @Override
    public Class<PieceQuantityDeficitRecordPo> poClass() {
        return PieceQuantityDeficitRecordPo.class;
    }
}
