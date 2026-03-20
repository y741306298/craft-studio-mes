package com.mes.domain.manufacturer.transBox.storageTank.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class StorageInventory extends BaseEntity {
    
    private String inventoryId;             // 库存记录 ID
    private String storageTankId;           // 储存柜 ID
    private String storageTankName;         // 储存柜名称
    private String productionPieceId;       // 生产工件 ID
    private String productionPieceType;     // 生产工件类型
    private Integer totalQuantity;          // 总库存数量
    private Integer availableQuantity;      // 可用库存数量
    private Integer lockedQuantity;         // 锁定库存数量
    private String unit;                    // 计量单位
    private Date lastCheckTime;             // 最后盘点时间
    private String lastCheckOperator;       // 最后盘点操作人
    private String status;                  // 库存状态
}
