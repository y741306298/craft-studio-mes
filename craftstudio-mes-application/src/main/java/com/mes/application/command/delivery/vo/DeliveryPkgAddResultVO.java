package com.mes.application.command.delivery.vo;

import lombok.Data;

@Data
public class DeliveryPkgAddResultVO {

    private String pkgId;
    private String recipientName;
    private String recipientMobile;
    private String recipientAddress;
    private QrCodeInfo qrCode;
    private BarCodeInfo barCode;
    private String routeDesc;
    private String remark;
    private String width;
    private String height;

    @Data
    public static class QrCodeInfo {
        private String format;
        private String content;
        private Double width;
        private Double height;
    }

    @Data
    public static class BarCodeInfo {
        private String format;
        private String content;
        private Double width;
        private Double height;
    }

}
