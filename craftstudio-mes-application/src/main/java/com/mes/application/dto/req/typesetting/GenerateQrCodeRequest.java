package com.mes.application.dto.req.typesetting;

import lombok.Data;

@Data
public class GenerateQrCodeRequest {
    /**
     * 制造商 ID
     */
    private String manufacturerMetaId;
    /**
     * 二维码扫描内容
     */
    private String content;
}
