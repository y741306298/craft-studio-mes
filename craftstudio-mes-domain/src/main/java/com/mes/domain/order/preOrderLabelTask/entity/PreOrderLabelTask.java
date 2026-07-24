package com.mes.domain.order.preOrderLabelTask.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import com.mes.domain.order.preOrderLabelTask.enums.PreOrderLabelTaskStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预下快递单批处理任务。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PreOrderLabelTask extends BaseEntity {
    private String orderId;
    private OrderChannelInfo channel;
    private PreOrderLabelTaskStatus status;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private String kuaidiNum;
}
