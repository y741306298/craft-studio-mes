package com.mes.application.dto;

import com.mes.application.command.typesetting.enums.TypesettingQueryType;
import com.mes.domain.manufacturer.procedureFlow.vo.ProcessingFlowCondition;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TypesettingQuery {
    private Integer current;
    private Integer size;
    private String manufacturerMetaId;
    private String queryType;
    private String status;
    private String materialName;
    private List<ProcessingFlowCondition> processingName;
    private String typesettingId;
    private String orderId;
    private String orderItemId;
    /**
     * 电商模式：true 时生产工件按订单 ID 分组，false 或空时按订单项 ID 分组。
     */
    private Boolean eCommerceMmodel;
    private Date startTime;
    private Date endTime;
    private String sourceType;
    private String routeId;
}
