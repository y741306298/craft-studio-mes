package com.mes.application.dto.req.delivery;

import lombok.Data;

import java.util.Date;

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
     * 开始时间
     */
    private Date startTime;

    /**
     * 返回结果数量，默认 50
     */
    private Integer topK = 50;
}
