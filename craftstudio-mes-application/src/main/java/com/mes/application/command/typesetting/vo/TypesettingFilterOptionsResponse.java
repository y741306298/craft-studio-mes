package com.mes.application.command.typesetting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 待排版列表的全量筛选项。
 */
@Data
@AllArgsConstructor
public class TypesettingFilterOptionsResponse {

    private List<TypesettingAndProductionPiecesResponse.ProcessingFlowOption> processingFlowList;

    private List<String> materialList;
}
