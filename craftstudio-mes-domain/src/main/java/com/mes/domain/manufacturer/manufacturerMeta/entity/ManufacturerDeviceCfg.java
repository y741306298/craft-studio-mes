package com.mes.domain.manufacturer.manufacturerMeta.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.device.enums.DeviceType;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.shared.enums.ProductUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerDeviceCfg extends BaseEntity {

    private String manufacturerMetaId;        // 所属制造商 ID（关联关系）
    private String deviceInfoId;                  // 设备 ID（同一类设备共享）
    private String deviceName;                // 设备名称
    private DeviceType deviceType;            // 设备类型
    private String deviceCode;                // 设备编号（与制造商关联时生成）
    private Double capacity;                  // 产能
    private ProductUnit capacityUnit;         // 产能单位
    private CfgStatus status;
    private boolean bound;                    // 是否已绑定
    private Integer boundVersion;             // 绑定版本

}
