package com.mes.application.dto.req.typesetting;

import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.req.base.ApiRequest;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class LayoutConfirmRequest extends ApiRequest {

    private List<TypesettingProductionPieceVO> typesettingCells;

    private List<ProductionPiece> productionPieces;

    private List<TypesettingInfo> typesettingInfos;

    private List<String> materialCodes;

    /**
     * 排版方式（用于判断调用异形排版还是网格排版算法）
     */
    private String layoutMode;

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getValidationMessage() {
        return "";
    }
}
