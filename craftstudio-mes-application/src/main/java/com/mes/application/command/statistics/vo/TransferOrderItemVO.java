package com.mes.application.command.statistics.vo;

import com.mes.application.command.order.vo.OrderItemVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Transferred order item enriched with its source and target factory snapshots. */
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferOrderItemVO extends OrderItemVO {
    private String sourceId;
    private String sourceName;
    private String targetId;
    private String targetName;
}
