package com.mes.infra.dal.order.preOrderLabelTask.po;

import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import com.mes.domain.order.preOrderLabelTask.entity.PreOrderLabelTask;
import com.mes.domain.order.preOrderLabelTask.enums.PreOrderLabelTaskStatus;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "preOrderLabelTask")
public class PreOrderLabelTaskPo extends BasePO<PreOrderLabelTask> {
    private String orderId;
    private OrderChannelInfo channel;
    private String status;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private String kuaidiNum;

    @Override
    public PreOrderLabelTask toDO() {
        PreOrderLabelTask task = new PreOrderLabelTask();
        task.setId(getId());
        task.setCreateTime(getCreateTime());
        task.setUpdateTime(getUpdateTime());
        task.setOrderId(this.orderId);
        task.setChannel(this.channel);
        task.setStatus(this.status == null ? null : PreOrderLabelTaskStatus.valueOf(this.status));
        task.setLogisticsCarrierInfo(this.logisticsCarrierInfo);
        task.setKuaidiNum(this.kuaidiNum);
        return task;
    }

    @Override
    protected BasePO<PreOrderLabelTask> fromDO(PreOrderLabelTask _do) {
        this.orderId = _do.getOrderId();
        this.channel = _do.getChannel();
        this.status = _do.getStatus() == null ? null : _do.getStatus().getCode();
        this.logisticsCarrierInfo = _do.getLogisticsCarrierInfo();
        this.kuaidiNum = _do.getKuaidiNum();
        return this;
    }
}
