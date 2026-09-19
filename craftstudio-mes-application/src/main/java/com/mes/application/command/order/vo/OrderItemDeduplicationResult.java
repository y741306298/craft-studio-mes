package com.mes.application.command.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDeduplicationResult {
    private String orderId;
    private String keptOrderItemId;
    private int matchedOrderItemCount;
    private int deletedOrderItemCount;
    private long deletedProductionPieceCount;
}
