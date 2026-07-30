package com.mes.application.command.typesetting.layout;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class QrLayoutRedoResolver {

    private final ProductionPieceService productionPieceService;

    public QrLayoutRedoResolver(ProductionPieceService productionPieceService) {
        this.productionPieceService = productionPieceService;
    }

    /**
     * 只检查当前排版直接引用的生产工件；来源为印版的 cell 不递归检查其子 cell。
     */
    public boolean hasRedoProductionPiece(TypesettingInfo info) {
        if (info == null || info.getTypesettingCells() == null) {
            return false;
        }
        return info.getTypesettingCells().stream()
                .filter(cell -> cell != null
                        && TypesettingSourceType.PART.getCode().equals(cell.getSourceType())
                        && StringUtils.isNotBlank(cell.getSourceId()))
                .map(TypesettingSourceCell::getSourceId)
                .map(productionPieceService::findById)
                .anyMatch(this::isRedo);
    }

    private boolean isRedo(ProductionPiece piece) {
        return piece != null && Boolean.TRUE.equals(piece.getIsRedo());
    }
}
