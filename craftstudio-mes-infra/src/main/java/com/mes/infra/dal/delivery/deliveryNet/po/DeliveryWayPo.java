package com.mes.infra.dal.delivery.deliveryNet.po;

import com.mes.domain.delivery.deliveryNet.entity.DeliveryWay;
import com.mes.domain.delivery.deliveryNet.enums.DeliveryWayNUM;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryWayPo extends BasePO<DeliveryWay> {
    
    private String name;
    private String numCode; // 存储枚举的 code
    private Double firstWeightPrice;
    private Double continueWeightPrice;
    private Double firstVolumePrice;
    private Double continueVolumePrice;
    private String weightUnit;
    private String volumeUnit;
    private Double maxWeight;
    private Double maxVolume;
    private Integer estimatedDays;
    private String description;
    private Boolean available;

    @Override
    public DeliveryWay toDO() {
        DeliveryWay deliveryWay = new DeliveryWay();
        copyBaseFieldsToDO(deliveryWay);
        
        deliveryWay.setName(this.name);
        
        // 将 code 转换为枚举
        if (this.numCode != null && !this.numCode.isEmpty()) {
            deliveryWay.setNum(DeliveryWayNUM.getByCode(this.numCode));
        }
        
        deliveryWay.setFirstWeightPrice(this.firstWeightPrice);
        deliveryWay.setContinueWeightPrice(this.continueWeightPrice);
        deliveryWay.setFirstVolumePrice(this.firstVolumePrice);
        deliveryWay.setContinueVolumePrice(this.continueVolumePrice);
        deliveryWay.setWeightUnit(this.weightUnit);
        deliveryWay.setVolumeUnit(this.volumeUnit);
        deliveryWay.setMaxWeight(this.maxWeight);
        deliveryWay.setMaxVolume(this.maxVolume);
        deliveryWay.setEstimatedDays(this.estimatedDays);
        deliveryWay.setDescription(this.description);
        deliveryWay.setAvailable(this.available);
        
        return deliveryWay;
    }

    @Override
    protected BasePO<DeliveryWay> fromDO(DeliveryWay _do) {
        if (_do == null) {
            return null;
        }
        
        this.name = _do.getName();
        
        // 将枚举转换为 code 存储
        if (_do.getNum() != null) {
            this.numCode = _do.getNum().getCode();
        }
        
        this.firstWeightPrice = _do.getFirstWeightPrice();
        this.continueWeightPrice = _do.getContinueWeightPrice();
        this.firstVolumePrice = _do.getFirstVolumePrice();
        this.continueVolumePrice = _do.getContinueVolumePrice();
        this.weightUnit = _do.getWeightUnit();
        this.volumeUnit = _do.getVolumeUnit();
        this.maxWeight = _do.getMaxWeight();
        this.maxVolume = _do.getMaxVolume();
        this.estimatedDays = _do.getEstimatedDays();
        this.description = _do.getDescription();
        this.available = _do.getAvailable();
        
        return this;
    }
}
