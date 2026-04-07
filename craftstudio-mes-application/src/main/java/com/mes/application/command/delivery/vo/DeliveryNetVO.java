package com.mes.application.command.delivery.vo;

import lombok.Data;

/**
 * 配送网络 VO
 */
@Data
public class DeliveryNetVO {

    /**
     * 工厂 ID
     */
    private String rmfId;
    
    /**
     * 物流方式 (承运商) ID，如：顺丰、韵达等
     */
    private String carrierId;
    
    /**
     * 物流供应商 ID
     */
    private String providerId;
    
    /**
     * 地区码
     */
    private String regionCode;
    
    /**
     * 价格信息
     */
    private DeliveryNetPrice price;
    
    /**
     * 配送网络价格
     */
    @Data
    public static class DeliveryNetPrice {
        /**
         * 首重价格
         */
        private Double firstWeightPrice;
        
        /**
         * 续重价格
         */
        private Double extraWeightPrice;
    }
}
