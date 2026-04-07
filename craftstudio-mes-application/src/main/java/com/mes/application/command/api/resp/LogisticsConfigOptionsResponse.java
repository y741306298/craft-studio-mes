package com.mes.application.command.api.resp;

import lombok.Data;
import java.util.List;

@Data
public class LogisticsConfigOptionsResponse {

    private List<ProviderItem> providers;
    private List<CarrierItem> carriers;

    @Data
    public static class ProviderItem {
        private String id;
        private String name;
        private ProviderMeta providerMeta;
        private java.util.Date createTime;
        private java.util.Date updateTime;
    }

    @Data
    public static class ProviderMeta {
        private String refId;
        private String type;
    }

    @Data
    public static class CarrierItem {
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
}
