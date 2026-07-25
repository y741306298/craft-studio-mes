package com.mes.domain.gatherplatform.wdt.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 旺店通快递配置。
 *
 * <p>用于建立工厂、MES 物流预设类型与旺店通仓库、物流方式之间的映射。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WdtConfig extends BaseEntity {
    /** 工厂元数据 ID。 */
    private String manufacturerMetaId;

    /** 旺店通仓库 ID。 */
    private String warehouseId;

    /** 旺店通物流 ID。 */
    private String logisticsId;

    /** 物流名称。 */
    private String logisticsName;

    /** MES 订单物流预设类型。 */
    private String presetType;
}
