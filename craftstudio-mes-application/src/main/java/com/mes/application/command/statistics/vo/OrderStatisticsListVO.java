package com.mes.application.command.statistics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsListVO {
    private List<OrderStatisticsItemVO> items;
    private long total;
    private Long totalOrderCount;
    private BigDecimal totalArea;
    private BigDecimal totalAmount;
    private List<OrderStatisticsMaterialVO> materialList;
    private List<OrderStatisticsStatusVO> statusList;
    private List<String> orgNameList;
}
