package com.mes.domain.delivery.deliveryPkg.entity;

import com.mes.domain.base.BaseEntity;
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
    
    private String carrierId;
    
    private String carrierName;
    
    private String deliveryManId;
    
    private String deliverySiidId;
    
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
