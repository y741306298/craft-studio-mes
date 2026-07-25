package com.mes.domain.delivery.deliveryPkg.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.delivery.deliveryPkg.enums.PreOrderLabelConsumeStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryRecord extends BaseEntity {

    private String kuaidiNum;

    private String deliveryRecordId;
    
    private String orderId;
    
    private String trackingNumber;

    /** 快递100云打印任务ID，用于电子面单复打 */
    private String taskId;

    /** 快递100面单复打次数 */
    private Integer reprintCount;

    /** Consumption state for a pre-ordered label. */
    private PreOrderLabelConsumeStatus consumeStatus;
    private Date packedAt;
    private String deliveryPkgId;
    private String formalPrintSiid;
    
    private String carrierId;
    
    private String carrierName;
    
    private String deliveryManId;
    
    private String deliverySiidId;

    /** 快递100云打印设备编码 */
    private String siid;
    
    private String userId;
    
    private String manufacturerMetaId;
    
    private String remark;
    
    private Date deliveryTime;

    private Boolean isSuccess;

    private String errorMsg;
    
    private List<ProductionPieceDTO> pieces;
    
    @Data
    public static class ProductionPieceDTO {
        private String productionPieceId;
        private String productionPieceName;
        private Integer quantity;
    }

}
