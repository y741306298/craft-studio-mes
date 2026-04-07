package com.mes.application.command.api.resp;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class LogisticsConfigResponse {

    private Map<String, List<LogisticsItem>> regionConfigs;

    @Data
    public static class LogisticsItem {
        private String providerId;
        private CarrierInfo carrier;
        private PriceInfo price;
    }

    @Data
    public static class CarrierInfo {
        private String id;
        private String name;
        private WeightRule weightRule;
        private java.util.Date createTime;
        private java.util.Date updateTime;
    }

    @Data
    public static class WeightRule {
        private Double firstWeight;
    }

    @Data
    public static class PriceInfo {
        private Double firstWeightPrice;
        private Double extraWeightPrice;
    }
}
