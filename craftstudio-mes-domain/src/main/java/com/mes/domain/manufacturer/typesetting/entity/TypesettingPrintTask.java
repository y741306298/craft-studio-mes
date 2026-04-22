package com.mes.domain.manufacturer.typesetting.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingDownloadTaskData;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TypesettingPrintTask extends BaseEntity {
    private String typesettingInfoId;
    private String deviceInfoId;
    private TypesettingDownloadTaskData data;
}
