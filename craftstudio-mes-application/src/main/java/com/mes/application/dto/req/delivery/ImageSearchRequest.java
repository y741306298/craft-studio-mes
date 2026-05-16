package com.mes.application.dto.req.delivery;

import lombok.Data;

@Data
public class ImageSearchRequest {
    /**
     * 查询图片的 Base64 编码
     */
    private String queryImageBase64;

    /**
     * 制造商元数据 ID
     */
    private String manufacturerMetaId;

    /**
     * 开始时间（ISO-8601 格式）
     */
    private String startTime;

    /**
     * 返回结果数量，默认 50
     */
    private Integer topK = 50;
}
