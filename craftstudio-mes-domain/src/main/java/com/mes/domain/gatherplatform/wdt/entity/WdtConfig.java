package com.mes.domain.gatherplatform.wdt.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聚单平台快递（保留 WDT 类型名兼容历史调用）配置。
 *
 * <p>用于建立工厂、MES 物流预设类型与旺店通仓库、物流方式之间的映射。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WdtConfig extends BaseEntity {
    /** 工厂元数据 ID。 */
    private String manufacturerMetaId;

    /** 聚单平台编码 WDT/JST；历史空值按 WDT 处理。 */
    private String platformType;

    /** 聚单平台仓库 ID。 */
    private String warehouseId;

    /** 聚单平台物流 ID。 */
    private String logisticsId;

    /** 物流名称。 */
    private String logisticsName;

    /** MES 订单物流预设类型。 */
    private String presetType;
}
