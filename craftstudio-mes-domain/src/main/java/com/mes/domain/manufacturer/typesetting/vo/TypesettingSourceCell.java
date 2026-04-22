package com.mes.domain.manufacturer.typesetting.vo;

import lombok.Data;

@Data
public class TypesettingSourceCell {

    private String sourceType;

    private String sourceId;

    private String orderItemId;

    private Integer quantity;
}
