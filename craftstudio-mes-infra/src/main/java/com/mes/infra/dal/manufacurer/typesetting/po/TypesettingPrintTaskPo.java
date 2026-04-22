package com.mes.infra.dal.manufacurer.typesetting.po;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingDownloadTaskData;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "typesetting_print_task")
public class TypesettingPrintTaskPo extends BasePO<TypesettingPrintTask> {
    private String typesettingInfoId;
    private String deviceInfoId;
    private TypesettingDownloadTaskData data;

    @Override
    public TypesettingPrintTask toDO() {
        TypesettingPrintTask task = new TypesettingPrintTask();
        task.setId(getId());
        task.setCreateTime(getCreateTime());
        task.setUpdateTime(getUpdateTime());
        task.setTypesettingInfoId(this.typesettingInfoId);
        task.setDeviceInfoId(this.deviceInfoId);
        task.setData(this.data);
        return task;
    }

    @Override
    protected BasePO<TypesettingPrintTask> fromDO(TypesettingPrintTask _do) {
        this.typesettingInfoId = _do.getTypesettingInfoId();
        this.deviceInfoId = _do.getDeviceInfoId();
        this.data = _do.getData();
        return this;
    }
}
