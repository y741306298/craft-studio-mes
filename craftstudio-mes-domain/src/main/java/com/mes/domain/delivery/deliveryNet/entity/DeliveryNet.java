package com.mes.domain.delivery.deliveryNet.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryNet extends BaseEntity {
    
    private String deliveryNetId;           // 配送网络 ID
    private String deliveryNetName;         // 配送网络名称
    private String routeId;                 // 关联的路线 ID
    private DeliveryRegionCfg deliveryRegionCfg;  // 配送区域配置
    private List<DeliveryWay> deliveryWays;       // 支持的配送方式列表
    
    private String description;             // 配送网络描述
    private String status;                  // 状态：ACTIVE-激活，INACTIVE-未激活，SUSPENDED-暂停
    private String coverageType;            // 覆盖类型：NATIONWIDE-全国，REGIONAL-区域，LOCAL-本地
    private Integer minDeliveryDays;        // 最小配送天数
    private Integer maxDeliveryDays;        // 最大配送天数
    private Double weightLimit;             // 重量限制（kg）
    private Double volumeLimit;             // 体积限制（m³）

}
