package com.mes.application.command.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductionPieceDeletionResult {
    private String orderId;
    private List<String> orderItemIds;
    private long deletedProductionPieceCount;
}
