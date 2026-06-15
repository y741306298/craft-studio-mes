package com.mes.application.command.typesetting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;

import java.util.List;

@Data
@AllArgsConstructor
public class TypesettingAndProductionPiecesResponse {

    private List<TypesettingProductionPieceVO> list;

    private Long total;

    private Long current;

    private List<String> processingFlowList;

    private List<String> materialList;

    private List<SourceTypeOption> sourceType;

    private OrgInfo orgInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceTypeOption {
        private String code;
        private String description;
    }
}
