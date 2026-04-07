package com.mes.application.command.api.req;

import lombok.Data;

@Data
public class ConfigLogisticsRequest {

    private String rmfId;
    private String carrierId;
    private String providerId;
    private String regionCode;
    private PriceInfo price;

    @Data
    public static class PriceInfo {
        private Double firstWeightPrice;
        private Double extraWeightPrice;
    }
}
