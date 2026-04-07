package com.mes.application.dto.req.delivery;

import com.mes.domain.delivery.deliveryNet.entity.DeliveryNet;
import com.mes.domain.delivery.deliveryNet.entity.DeliveryRegionCfg;
import com.mes.domain.delivery.deliveryNet.entity.DeliveryWay;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryNetRequest {

    private String id;

    @NotBlank(message = "配送网络 ID 不能为空")
    private String deliveryNetId;

    @NotBlank(message = "配送网络名称不能为空")
    private String deliveryNetName;

    private String routeId;

    private DeliveryRegionCfg deliveryRegionCfg;

    private List<DeliveryWay> deliveryWays;

    private String description;

    private String status;

    private String coverageType;

    private Integer minDeliveryDays;

    private Integer maxDeliveryDays;

    private Double weightLimit;

    private Double volumeLimit;

    public DeliveryNet toDomainEntity() {
        DeliveryNet deliveryNet = new DeliveryNet();
        deliveryNet.setId(this.id);
        deliveryNet.setDeliveryNetId(this.deliveryNetId);
        deliveryNet.setDeliveryNetName(this.deliveryNetName);
        deliveryNet.setRouteId(this.routeId);
        deliveryNet.setDeliveryRegionCfg(this.deliveryRegionCfg);
        deliveryNet.setDeliveryWays(this.deliveryWays);
        deliveryNet.setDescription(this.description);
        deliveryNet.setStatus(this.status);
        deliveryNet.setCoverageType(this.coverageType);
        deliveryNet.setMinDeliveryDays(this.minDeliveryDays);
        deliveryNet.setMaxDeliveryDays(this.maxDeliveryDays);
        deliveryNet.setWeightLimit(this.weightLimit);
        deliveryNet.setVolumeLimit(this.volumeLimit);
        return deliveryNet;
    }
}
