package com.mes.infra.dal.gatherplatform.wdt.po;

import com.mes.domain.gatherplatform.wdt.entity.WdtConfig;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 聚单平台快递（保留 WDT 类型名兼容历史调用）配置 MongoDB 持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "wdtConfig")
public class WdtConfigPO extends BasePO<WdtConfig> {
    /** 工厂元数据 ID。 */
    private String manufacturerMetaId;
    private String platformType;
    /** 聚单平台仓库 ID。 */
    private String warehouseId;
    /** 聚单平台物流 ID。 */
    private String logisticsId;
    /** 物流名称。 */
    private String logisticsName;
    /** MES 订单物流预设类型。 */
    private String presetType;

    /**
     * 将持久化对象转换为领域实体。
     */
    @Override
    public WdtConfig toDO() {
        WdtConfig value = new WdtConfig();
        copyBaseFieldsToDO(value);
        value.setManufacturerMetaId(manufacturerMetaId);
        value.setPlatformType(platformType);
        value.setWarehouseId(warehouseId);
        value.setLogisticsId(logisticsId);
        value.setLogisticsName(logisticsName);
        value.setPresetType(presetType);
        return value;
    }

    /**
     * 使用领域实体填充持久化对象。
     */
    @Override
    protected BasePO<WdtConfig> fromDO(WdtConfig value) {
        manufacturerMetaId = value.getManufacturerMetaId();
        platformType = value.getPlatformType();
        warehouseId = value.getWarehouseId();
        logisticsId = value.getLogisticsId();
        logisticsName = value.getLogisticsName();
        presetType = value.getPresetType();
        return this;
    }
}
