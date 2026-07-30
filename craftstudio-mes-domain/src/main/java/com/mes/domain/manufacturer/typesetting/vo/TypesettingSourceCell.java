package com.mes.domain.manufacturer.typesetting.vo;

import lombok.Data;

@Data
public class TypesettingSourceCell {

    private String sourceType;

    private String sourceId;

    private String orderItemId;

    private Integer quantity;

    /**
     * 排版时来源生产工件是否为重做件。
     *
     * <p>作为排版来源快照保存，避免生成标签时来源工件状态已经变化，导致“重做”标记丢失。</p>
     */
    private Boolean isRedo;

    /**
     * 来源对象预览 URL：生产工件取产品图预览，印版取 nestedSvg。
     */
    private String previewUrl;
}
