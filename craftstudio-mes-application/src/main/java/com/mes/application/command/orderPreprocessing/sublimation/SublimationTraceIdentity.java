package com.mes.application.command.orderPreprocessing.sublimation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** x为订单循环号，y为订单项编号，z为订单项总数量。 */
public record SublimationTraceIdentity(int orderSequence, int itemNumber, int itemCount, int quantity) {
    public SublimationTraceIdentity {
        if (orderSequence < 1 || orderSequence > 10000 || itemNumber < 1 || itemNumber > itemCount || quantity < 1) {
            throw new IllegalArgumentException("热升华订单溯源号无效");
        }
    }
    public String code() { return orderSequence + "-" + itemNumber + "-" + quantity; }

    /** 数据库稳定顺序初次编号，后续新增项追加，不重排已有编号。 */
    public static List<String> mergeItemIds(List<String> allocated, List<String> current) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (allocated != null) ids.addAll(allocated);
        ids.addAll(current);
        return new ArrayList<>(ids);
    }
}
