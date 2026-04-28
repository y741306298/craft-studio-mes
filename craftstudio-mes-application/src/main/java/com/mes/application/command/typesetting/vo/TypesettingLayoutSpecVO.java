package com.mes.application.command.typesetting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 排版规格
 */
@Data
@AllArgsConstructor
public class TypesettingLayoutSpecVO {
    private String name;
    private Integer width;
    private Integer height;
}
