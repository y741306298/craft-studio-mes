package com.mes.domain.manufacturer.transBox.storageTank.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class StorageOperationRecord extends BaseEntity {
    
    private String recordId;                // 操作记录 ID
    private String storageTankId;           // 储存柜 ID
    private String storageTankName;         // 储存柜名称
    private String slotId;                  // 储位 ID
    private String operationType;           // 操作类型（IN-入库，OUT-出库，TRANSFER-转移，CHECK-盘点）
    private String productionPieceId;       // 生产工件 ID
    private String productionPieceType;     // 生产工件类型
    private Integer quantity;               // 操作数量
    private String operatorId;              // 操作人 ID
    private String operatorName;            // 操作人姓名
    private Date operationTime;             // 操作时间
    private String remarks;                 // 备注信息
    private String status;                  // 操作状态

    public static final String OPERATION_TYPE_IN = "IN";
    public static final String OPERATION_TYPE_OUT = "OUT";
    public static final String OPERATION_TYPE_TRANSFER = "TRANSFER";
    public static final String OPERATION_TYPE_CHECK = "CHECK";

    public boolean isInOperation() {
        return OPERATION_TYPE_IN.equals(this.operationType);
    }

    public boolean isOutOperation() {
        return OPERATION_TYPE_OUT.equals(this.operationType);
    }
}
