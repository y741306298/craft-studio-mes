package com.mes.application.command.statistics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsFiltersVO {
    private List<OrderStatisticsDimensionVO> enterprises;
    private List<OrderStatisticsDimensionVO> materials;
    private List<OrderStatisticsDimensionVO> routes;
}
