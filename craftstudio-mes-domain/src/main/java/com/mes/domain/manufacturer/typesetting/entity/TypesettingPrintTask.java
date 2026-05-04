package com.mes.domain.manufacturer.typesetting.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingDownloadTaskData;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TypesettingPrintTask extends BaseEntity {
    private String typesettingInfoId;
    private java.util.List<String> deviceInfoId;
    private java.util.List<String> deviceCode;
    /**
     * 任务领取状态：待领取 / 已领取
     */
    private String status;
    private TypesettingDownloadTaskData data;
}
