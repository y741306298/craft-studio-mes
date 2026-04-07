package com.mes.application.command.api.resp;

import lombok.Data;

@Data
public class ProcessMetaResponse {

    private ProcessMeta processMeta;
    private ProcessConfig config;

    @Data
    public static class ProcessMeta {
        private String id;
        private String code;
        private String name;
        private String cover;
        private java.util.List<ParamMeta> paramMetas;
        private java.util.Date createTime;
        private java.util.Date updateTime;
    }

    @Data
    public static class ParamMeta {
        private String id;
        private String name;
        private String presetType;
        private Boolean required;
        private java.util.List<String> supportedFormats;
        private java.util.List<String> supportedAccessoryIds;
    }

    @Data
    public static class ProcessConfig {
        private String id;
        private String manufacturerId;
        private String processMetaId;
        private PriceInfo unitPrice;
        private java.util.Date createTime;
        private java.util.Date updateTime;
    }

    @Data
    public static class PriceInfo {
        private Double price;
        private Object totalPriceDiscountRule;
    }
}
