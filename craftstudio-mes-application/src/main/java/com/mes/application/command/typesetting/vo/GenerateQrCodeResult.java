package com.mes.application.command.typesetting.vo;

import lombok.Data;

@Data
public class GenerateQrCodeResult {
    private String manufacturerMetaId;
    private String content;
    /**
     * 二维码 PNG 的 Base64 内容
     */
    private String qrCodeBase64;
}
