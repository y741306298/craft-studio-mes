package com.mes.application.command.statistics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsMaterialVO {
    private String materialId;
    private String materialName;
    private String materialType;
}
