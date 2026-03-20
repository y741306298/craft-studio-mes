package com.mes.domain.manufacturer.transBox.storageTank.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class StorageSlot extends BaseEntity {
    
    private String slotId;                  // 储位 ID
    private String slotCode;                // 储位编码
    private String storageTankId;           // 所属储存柜 ID
    private Integer slotOrder;              // 储位顺序号
    private String status;                  // 储位状态（AVAILABLE-可用，OCCUPIED-已占用）
    private String productionPieceId;       // 存放的生产工件 ID
    private String productionPieceType;     // 生产工件类型
    private Integer quantity;               // 存放数量
    private Date storageTime;               // 入库时间
    private Date outTime;                   // 出库时间
    private String remarks;                 // 备注信息
    private Double weight;                  // 存放物品重量
    private Double volume;                  // 存放物品体积
    private String temperature;             // 存储时温度
    private String humidity;                // 存储时湿度

    public boolean isOccupied() {
        return "OCCUPIED".equals(this.status);
    }

    public boolean isAvailable() {
        return "AVAILABLE".equals(this.status);
    }

    public void storeItem(String productionPieceId, String productionPieceType, Integer quantity) {
        this.productionPieceId = productionPieceId;
        this.productionPieceType = productionPieceType;
        this.quantity = quantity;
        this.status = "OCCUPIED";
        this.storageTime = new Date();
    }

    public void retrieveItem() {
        this.outTime = new Date();
        this.productionPieceId = null;
        this.productionPieceType = null;
        this.quantity = null;
        this.status = "AVAILABLE";
    }
}
