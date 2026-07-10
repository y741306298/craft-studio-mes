package com.mes.application.command.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPackagingSyncResult {
    private long checkedCount;
    private long updatedCount;
    private List<ItemPackagingSyncResult> items = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemPackagingSyncResult {
        private String orderItemId;
        private Integer orderItemQuantity;
        private long packedQuantity;
        private boolean updated;
    }
}
