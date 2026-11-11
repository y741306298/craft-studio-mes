package com.mes.domain.order.orderInfo.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/** 订单热升华溯源身份；独立保存，避免分配号码时改写订单状态。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SublimationTraceOrder extends BaseEntity {
    private String orderId;
    private String manufacturerId;
    private Integer sequence;
    /** 冻结初次订单项顺序，后续新增项追加，确保已分配的y不漂移。 */
    private List<String> orderItemIds;
}
