package com.mes.application.command.wdt.req;

import com.mes.domain.gatherplatform.wdt.entity.WdtConfig;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 旺店通快递配置新增、更新请求。
 */
@Data
public class WdtConfigRequest {
    /** 更新时使用的配置 ID；新增时为空。 */
    private String id;

    /** 工厂元数据 ID。 */
    @NotBlank(message = "manufacturerMetaId不能为空")
    private String manufacturerMetaId;

    /** 旺店通仓库 ID。 */
    @NotBlank(message = "warehouseId不能为空")
    private String warehouseId;

    /** 旺店通物流 ID。 */
    @NotBlank(message = "logisticsId不能为空")
    private String logisticsId;

    /** 物流名称。 */
    @NotBlank(message = "logisticsName不能为空")
    private String logisticsName;

    /** MES 订单物流预设类型。 */
    @NotBlank(message = "presetType不能为空")
    private String presetType;

    /**
     * 将接口请求转换为领域实体。
     *
     * @return 旺店通快递配置领域实体
     */
    public WdtConfig toDomainEntity() {
        WdtConfig config = new WdtConfig();
        config.setId(id);
        config.setManufacturerMetaId(manufacturerMetaId);
        config.setWarehouseId(warehouseId);
        config.setLogisticsId(logisticsId);
        config.setLogisticsName(logisticsName);
        config.setPresetType(presetType);
        return config;
    }
}
