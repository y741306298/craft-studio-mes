package com.mes.application.dto.resp.delivery;

import com.mes.domain.delivery.deliveryNet.entity.DeliveryNet;
import com.mes.domain.delivery.deliveryNet.entity.DeliveryRegionCfg;
import com.mes.domain.delivery.deliveryNet.entity.DeliveryWay;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DeliveryNetListResponse {

    private String id;
    private String deliveryNetId;
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
    private Date createTime;
    private Date updateTime;

    public static DeliveryNetListResponse from(DeliveryNet deliveryNet) {
        if (deliveryNet == null) {
            return null;
        }

        DeliveryNetListResponse response = new DeliveryNetListResponse();
        response.setId(deliveryNet.getId());
        response.setDeliveryNetId(deliveryNet.getDeliveryNetId());
        response.setDeliveryNetName(deliveryNet.getDeliveryNetName());
        response.setRouteId(deliveryNet.getRouteId());
        response.setDeliveryRegionCfg(deliveryNet.getDeliveryRegionCfg());
        response.setDeliveryWays(deliveryNet.getDeliveryWays());
        response.setDescription(deliveryNet.getDescription());
        response.setStatus(deliveryNet.getStatus());
        response.setCoverageType(deliveryNet.getCoverageType());
        response.setMinDeliveryDays(deliveryNet.getMinDeliveryDays());
        response.setMaxDeliveryDays(deliveryNet.getMaxDeliveryDays());
        response.setWeightLimit(deliveryNet.getWeightLimit());
        response.setVolumeLimit(deliveryNet.getVolumeLimit());
        response.setCreateTime(deliveryNet.getCreateTime());
        response.setUpdateTime(deliveryNet.getUpdateTime());

        return response;
    }
}
