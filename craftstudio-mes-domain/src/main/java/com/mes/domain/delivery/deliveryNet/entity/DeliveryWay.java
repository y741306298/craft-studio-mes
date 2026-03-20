package com.mes.domain.delivery.deliveryNet.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.delivery.deliveryNet.enums.DeliveryWayNUM;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryWay extends BaseEntity {
    
    private String name;
    private DeliveryWayNUM num;
    private Double firstWeightPrice;      // 首重价格
    private Double continueWeightPrice;   // 续重价格
    private Double firstVolumePrice;      // 首体积价格
    private Double continueVolumePrice;   // 续体积价格
    private String weightUnit;            // 重量单位：kg, g 等
    private String volumeUnit;            // 体积单位：m³, cm³ 等
    private Double maxWeight;             // 最大承重
    private Double maxVolume;             // 最大体积
    private Integer estimatedDays;        // 预计送达天数
    private String description;           // 服务描述
    private Boolean available;            // 是否可用
    
    /**
     * 计算重量运费
     * @param weight 货物重量（kg）
     * @return 运费
     */
    public Double calculateWeightFreight(Double weight) {
        if (weight == null || weight <= 0) {
            return 0.0;
        }
        
        if (firstWeightPrice == null) {
            return 0.0;
        }
        
        // 首重内
        if (weight <= 1.0) {
            return firstWeightPrice;
        }
        
        // 首重 + 续重
        if (continueWeightPrice != null) {
            double continueWeight = Math.ceil(weight - 1.0); // 向上取整
            return firstWeightPrice + continueWeight * continueWeightPrice;
        }
        
        return firstWeightPrice;
    }
    
    /**
     * 计算体积运费
     * @param volume 货物体积（m³）
     * @return 运费
     */
    public Double calculateVolumeFreight(Double volume) {
        if (volume == null || volume <= 0) {
            return 0.0;
        }
        
        if (firstVolumePrice == null) {
            return 0.0;
        }
        
        // 首体积内
        if (volume <= 1.0) {
            return firstVolumePrice;
        }
        
        // 首体积 + 续体积
        if (continueVolumePrice != null) {
            double continueVolume = Math.ceil(volume - 1.0); // 向上取整
            return firstVolumePrice + continueVolume * continueVolumePrice;
        }
        
        return firstVolumePrice;
    }
    
    /**
     * 计算运费（取重量和体积运费的较大值）
     * @param weight 货物重量（kg）
     * @param volume 货物体积（m³）
     * @return 运费
     */
    public Double calculateFreight(Double weight, Double volume) {
        Double weightFreight = calculateWeightFreight(weight);
        Double volumeFreight = calculateVolumeFreight(volume);
        
        // 返回较大值（物流通常按重量或体积中收费较高的计算）
        return Math.max(weightFreight, volumeFreight);
    }
    
    /**
     * 判断是否为顺丰快递
     */
    public boolean isSFExpress() {
        return num != null && num.isSFExpress();
    }
    
    /**
     * 判断是否为标快类型
     */
    public boolean isStandard() {
        return num != null && num.isStandard();
    }
    
    /**
     * 判断是否为经济型快递
     */
    public boolean isEconomy() {
        return num != null && num.isEconomy();
    }
    
    /**
     * 获取快递公司代码
     */
    public String getCarrierCode() {
        return num != null ? num.getCode() : null;
    }
    
    /**
     * 验证价格配置是否完整
     */
    public boolean validatePriceConfig() {
        // 至少需要配置首重价格
        if (firstWeightPrice == null || firstWeightPrice < 0) {
            return false;
        }
        
        // 如果配置了续重价格，必须大于等于 0
        if (continueWeightPrice != null && continueWeightPrice < 0) {
            return false;
        }
        
        // 如果配置了体积价格，必须大于等于 0
        if ((firstVolumePrice != null && firstVolumePrice < 0) ||
            (continueVolumePrice != null && continueVolumePrice < 0)) {
            return false;
        }
        
        return true;
    }
}
