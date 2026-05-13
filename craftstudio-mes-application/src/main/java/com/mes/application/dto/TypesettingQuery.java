package com.mes.application.dto;

import com.mes.application.command.typesetting.enums.TypesettingQueryType;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import lombok.Data;

import java.util.Date;

@Data
public class TypesettingQuery {
    private String manufacturerMetaId;
    private String queryType;
    private String status;
    private String materialName;
    private String processingName;
    private Date startTime;
    private Date endTime;
    private String sourceType;
}
