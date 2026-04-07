package com.mes.application.command.api.resp;

import lombok.Data;

@Data
public class MtsProductListResponse {

    private String id;
    private String code;
    private String name;
    private String cover;
    private SpecsPreview specsPreview;

    @Data
    public static class SpecsPreview {
        private Integer itemsTotal;
        private java.util.List<PreviewItem> previewItems;
    }

    @Data
    public static class PreviewItem {
        private MtsProductSpec mtsProductSpec;
        private SpecConfig config;
    }

    @Data
    public static class MtsProductSpec {
        private String id;
        private String code;
        private String name;
        private String productId;
        private String cover;
        private Size3D size3D;
        private String unitType;
        private Double unitWeight;
        private java.util.Date createTime;
        private java.util.Date updateTime;
    }

    @Data
    public static class Size3D {
        private Double width;
        private Double height;
        private Double depth;
    }

    @Data
    public static class SpecConfig {
        private String id;
        private String manufacturerId;
        private String mtsProductSpecId;
        private PriceInfo price;
        private PriceInfo unitPrice;
        private Integer stock;
        private java.util.Date createTime;
        private java.util.Date updateTime;
    }

    @Data
    public static class PriceInfo {
        private Double price;
        private Object totalPriceDiscountRule;
    }
}
