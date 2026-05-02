package com.mes.application.command.delivery.vo;

import lombok.Data;

@Data
public class DeliveryPkgAddResultVO {

    private String pkgId;
    private String recipientName;
    private String recipientMobile;
    private String recipientAddress;
    private QrCodeInfo qrCode;
    private String routeDesc;
    private String remark;

    @Data
    public static class QrCodeInfo {
        private String format;
        private String content;
    }
}
