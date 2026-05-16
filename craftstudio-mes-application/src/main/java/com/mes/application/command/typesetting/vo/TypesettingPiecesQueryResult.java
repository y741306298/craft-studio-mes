package com.mes.application.command.typesetting.vo;

import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TypesettingPiecesQueryResult {
    private PagedResult<TypesettingProductionPieceVO> pagedResult;
    private List<TypesettingProductionPieceVO> allItems;
}
