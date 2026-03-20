package com.mes.domain.delivery.deliveryNet.enums;

/**
 * 快递配送方式枚举
 */
public enum DeliveryWayNUM {
    MYSELF_DELIVERY("自主配送", "MYSELF"),
    
    SF_STANDARD_EXPRESS("顺丰标快", "SF"),           // 顺丰标准快递
    SF_ECONOMY_EXPRESS("顺丰普快", "SF-ECONOMY"),    // 顺丰普通快递
    SF_SAME_CITY("顺丰同城", "SF-SAMECITY"),         // 顺丰同城急送
    
    ZTO_STANDARD("中通标快", "ZTO"),                 // 中通标准快递
    ZTO_ECONOMY("中通普快", "ZTO-ECONOMY"),          // 中通普通快递
    
    YTO_STANDARD("圆通标快", "YTO"),                 // 圆通标准快递
    YTO_ECONOMY("圆通普快", "YTO-ECONOMY"),          // 圆通普通快递
    
    STO_STANDARD("申通标快", "STO"),                 // 申通标准快递
    STO_ECONOMY("申通普快", "STO-ECONOMY"),          // 申通普通快递
    
    YUNDA_STANDARD("韵达标快", "YUNDA"),             // 韵达标准快递
    YUNDA_ECONOMY("韵达普快", "YUNDA-ECONOMY"),      // 韵达普通快递
    
    EMS_STANDARD("EMS 特快", "EMS"),                 // EMS 特快专递
    EMS_ECONOMY("EMS 经济", "EMS-ECONOMY"),          // EMS 经济快递
    
    JD_EXPRESS("京东快递", "JD"),                    // 京东快递
    JD_LOGISTICS("京东物流", "JD-LOGISTICS"),        // 京东物流
    
    BEST_STANDARD("百世标快", "BEST"),               // 百世标准快递
    BEST_ECONOMY("百世普快", "BEST-ECONOMY"),        // 百世普通快递
    
    DEPPON_EXPRESS("德邦快递", "DEPPON"),            // 德邦快递
    DEPPON_LOGISTICS("德邦物流", "DEPPON-LOGISTICS"),// 德邦物流
    
    OTHER("其他快递", "OTHER");                      // 其他快递
    
    private final String description;
    private final String code;
    
    DeliveryWayNUM(String description, String code) {
        this.description = description;
        this.code = code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static DeliveryWayNUM getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        for (DeliveryWayNUM deliveryWay : values()) {
            if (deliveryWay.getCode().equalsIgnoreCase(code)) {
                return deliveryWay;
            }
        }
        
        return null;
    }
    
    /**
     * 判断是否为顺丰快递
     */
    public boolean isSFExpress() {
        return this.code != null && this.code.startsWith("SF");
    }
    
    /**
     * 判断是否为标快类型
     */
    public boolean isStandard() {
        return this.name().contains("STANDARD") || 
               this.name().contains("EXPRESS") ||
               this.name().equals("JD_EXPRESS") ||
               this.name().equals("EMS_STANDARD");
    }
    
    /**
     * 判断是否为经济型快递
     */
    public boolean isEconomy() {
        return this.name().contains("ECONOMY") || this.name().contains("PRIVACY");
    }
}
