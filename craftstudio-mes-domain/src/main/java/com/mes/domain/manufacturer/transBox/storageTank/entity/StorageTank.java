package com.mes.domain.manufacturer.transBox.storageTank.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class StorageTank extends BaseEntity {
    
    private String storageTankId;           // 储存柜 ID
    private String storageTankName;         // 储存柜名称
    private String storageTankCode;         // 储存柜编码
    private String storageTankType;         // 储存柜类型
    private String manufacturerId;          // 制造商 ID
    private String location;                // 储存柜位置
    private String status;                  // 储存柜状态（ACTIVE-激活，INACTIVE-未激活，MAINTENANCE-维护中）
    private Boolean movable;                // 是否可移动（true-可移动，false-固定式）
    private Integer totalSlots;             // 总储位数量
    private Integer usedSlots;              // 已用储位数量
    private Integer remainingSlots;         // 剩余储位数量
    private Double maxCapacity;             // 最大容量
    private Double currentCapacity;         // 当前容量
    private String capacityUnit;            // 容量单位（kg/m³等）
    private String temperatureRange;        // 温度范围要求
    private String humidityRange;           // 湿度范围要求
    private String description;             // 储存柜描述
    private List<StorageSlot> storageSlots; // 储位列表
    private String managerId;               // 管理员 ID
    private String managerName;             // 管理员姓名

    public int getUsedSlotCount() {
        return storageSlots != null ? (int) storageSlots.stream()
            .filter(slot -> "OCCUPIED".equals(slot.getStatus()))
            .count() : 0;
    }

    public int getRemainingSlotCount() {
        return storageSlots != null ? (int) storageSlots.stream()
            .filter(slot -> "AVAILABLE".equals(slot.getStatus()))
            .count() : 0;
    }

    public boolean hasAvailableSlot() {
        return getRemainingSlotCount() > 0;
    }

    public StorageSlot findAvailableSlot() {
        if (storageSlots == null) {
            return null;
        }
        return storageSlots.stream()
            .filter(slot -> "AVAILABLE".equals(slot.getStatus()))
            .findFirst()
            .orElse(null);
    }
}
