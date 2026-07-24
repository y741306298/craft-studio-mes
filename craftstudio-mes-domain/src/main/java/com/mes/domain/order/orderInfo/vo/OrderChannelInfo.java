package com.mes.domain.order.orderInfo.vo;

import com.mes.domain.order.enums.OrderChannelType;
import lombok.Data;

/**
 * 订单渠道信息。
 */
@Data
public class OrderChannelInfo {
    /** 渠道类型：GATHER_PLATFORM 聚单平台单，MANUAL 手工单 */
    private OrderChannelType type;

    /** 聚单平台编码，手工单为空 */
    private String code;

    /** 聚单平台订单 ID，手工单为空 */
    private String orderId;
}
