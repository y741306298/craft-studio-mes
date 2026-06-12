package com.mes.domain.manufacturer.typesetting.vo;

import lombok.Data;

@Data
public class TypesettingSourceCell {

    private String sourceType;

    private String sourceId;

    private String orderItemId;

    private Integer quantity;

    /**
     * 来源对象预览 URL：生产工件取产品图预览，印版取 nestedSvg。
     */
    private String previewUrl;
}
