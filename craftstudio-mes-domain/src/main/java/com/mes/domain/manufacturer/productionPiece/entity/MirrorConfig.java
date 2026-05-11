package com.mes.domain.manufacturer.productionPiece.entity;

import lombok.Data;

@Data
public class MirrorConfig {
    /** 步骤1：算法回调返回的反面原图地址。 */
    private String img;
    /** 步骤2：算法回调返回的反面蒙版 SVG 地址。 */
    private String svg;
    /** 步骤3：反面预览图地址（已补全 OSS 前缀）。 */
    private String previewImg;
    /** 步骤4：反面缩略图地址（已补全 OSS 前缀）。 */
    private String thumbnail;
    /** 步骤5：反面出血信息。 */
    private Blood blood;
}
