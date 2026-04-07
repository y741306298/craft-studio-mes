package com.mes.domain.delivery.deliveryPkg.vo;

import lombok.Data;

@Data
public class AuthOrderResponse {

    private Boolean success;

    private int code;

    private String message;

    private OrderResult data;

    @Data
    public class OrderResult {

        private String taskId;

        private String kuaidinum;

        private String childNum;

        private String returnNum;

        private String label;

        private String bulkpen;

        private String orgCode;

        private String orgName;

        private String destCode;

        private String destName;

        private String orgSortingCode;

        private String orgSortingName;

        private String destSortingCode;

        private String destSortingName;

        private String orgExtra;

        private String destExtra;

        private String pkgCode;

        private String pkgName;

        private String road;

        private String qrCode;

        private String kdComOrderNum;

        private String expressCode;

        private String expressName;


    }

}
