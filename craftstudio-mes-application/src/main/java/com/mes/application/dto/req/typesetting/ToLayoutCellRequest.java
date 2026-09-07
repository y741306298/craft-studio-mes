package com.mes.application.dto.req.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import lombok.Data;

/**
 * 单个排版来源的入参。
 *
 * <p>材料、工艺、SVG、尺寸和状态等属性由服务端根据来源 ID 查询，不由客户端提交。</p>
 */
@Data
public class ToLayoutCellRequest {

    private String sourceType;
    private String sourceId;
    private Integer quantity;

    public boolean isValid() {
        return TypesettingSourceType.fromCode(sourceType) != null
                && sourceId != null && !sourceId.isBlank()
                && quantity != null && quantity > 0;
    }

    public TypesettingProductionPieceVO toTypesettingProductionPiece() {
        TypesettingProductionPieceVO cell = new TypesettingProductionPieceVO();
        cell.setSourceType(sourceType);
        cell.setSourceId(sourceId);
        cell.setQuantity(quantity);
        return cell;
    }
}
