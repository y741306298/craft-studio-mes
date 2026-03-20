package com.mes.infra.dal.delivery.deliveryNet.po;

import com.mes.domain.delivery.deliveryNet.entity.DeliveryNet;
import com.mes.domain.delivery.deliveryNet.entity.DeliveryWay;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "deliveryNet")
public class DeliveryNetPo extends BasePO<DeliveryNet> {
    
    private String deliveryNetId;
    private String deliveryNetName;
    private String routeId;
    private DeliveryRegionCfgPo deliveryRegionCfg;
    private List<DeliveryWayPo> deliveryWays;
    
    private String description;
    private String status;
    private String coverageType;
    private Integer minDeliveryDays;
    private Integer maxDeliveryDays;
    private Double weightLimit;
    private Double volumeLimit;
    private String serviceHours;
    private String contactPhone;
    private String contactEmail;
    private String remarks;

    @Override
    public DeliveryNet toDO() {
        DeliveryNet deliveryNet = new DeliveryNet();
        copyBaseFieldsToDO(deliveryNet);
        
        deliveryNet.setDeliveryNetId(this.deliveryNetId);
        deliveryNet.setDeliveryNetName(this.deliveryNetName);
        deliveryNet.setRouteId(this.routeId);
        
        // 转换区域配置
        if (this.deliveryRegionCfg != null) {
            deliveryNet.setDeliveryRegionCfg(this.deliveryRegionCfg.toDO());
        }
        
        // 转换配送方式列表
        if (this.deliveryWays != null && !this.deliveryWays.isEmpty()) {
            List<DeliveryWay> ways = this.deliveryWays.stream()
                    .map(DeliveryWayPo::toDO)
                    .collect(java.util.stream.Collectors.toList());
            deliveryNet.setDeliveryWays(ways);
        }
        
        deliveryNet.setDescription(this.description);
        deliveryNet.setStatus(this.status);
        deliveryNet.setCoverageType(this.coverageType);
        deliveryNet.setMinDeliveryDays(this.minDeliveryDays);
        deliveryNet.setMaxDeliveryDays(this.maxDeliveryDays);
        deliveryNet.setWeightLimit(this.weightLimit);
        deliveryNet.setVolumeLimit(this.volumeLimit);
        return deliveryNet;
    }

    @Override
    protected BasePO<DeliveryNet> fromDO(DeliveryNet _do) {
        if (_do == null) {
            return null;
        }
        
        this.deliveryNetId = _do.getDeliveryNetId();
        this.deliveryNetName = _do.getDeliveryNetName();
        this.routeId = _do.getRouteId();
        
        // 转换区域配置
        if (_do.getDeliveryRegionCfg() != null) {
            this.deliveryRegionCfg = DeliveryRegionCfgPo.fromDO(_do.getDeliveryRegionCfg(), DeliveryRegionCfgPo.class);
        }
        
        // 转换配送方式列表
        if (_do.getDeliveryWays() != null && !_do.getDeliveryWays().isEmpty()) {
            List<DeliveryWayPo> wayPos = _do.getDeliveryWays().stream()
                    .map(way -> DeliveryWayPo.fromDO(way, DeliveryWayPo.class))
                    .collect(java.util.stream.Collectors.toList());
            this.deliveryWays = wayPos;
        }
        
        this.description = _do.getDescription();
        this.status = _do.getStatus();
        this.coverageType = _do.getCoverageType();
        this.minDeliveryDays = _do.getMinDeliveryDays();
        this.maxDeliveryDays = _do.getMaxDeliveryDays();
        this.weightLimit = _do.getWeightLimit();
        this.volumeLimit = _do.getVolumeLimit();
        return this;
    }
}
