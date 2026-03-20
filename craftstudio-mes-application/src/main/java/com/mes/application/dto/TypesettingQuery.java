package com.mes.application.dto;

import com.mes.application.command.typesetting.enums.TypesettingQueryType;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import lombok.Data;

@Data
public class TypesettingQuery {
    private TypesettingQueryType queryType;
    private TypesettingStatus status;
    private String material;
    private String nodeName;
    private PagedQuery pagedQuery;
}
