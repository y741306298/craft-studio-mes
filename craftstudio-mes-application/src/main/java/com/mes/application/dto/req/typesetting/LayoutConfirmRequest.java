package com.mes.application.dto.req.typesetting;

import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.req.base.ApiRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class LayoutConfirmRequest extends ApiRequest {

    private List<TypesettingProductionPieceVO> typesettingCells;

    private List<ContainerInfo> containers;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContainerInfo {
        private Integer width;
        private Integer height;
    }
}
