package com.mes.application.command.typesetting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TypesettingAndProductionPiecesResponse {

    private List<TypesettingProductionPieceVO> list;

    private List<String> processingFlowList;
}
