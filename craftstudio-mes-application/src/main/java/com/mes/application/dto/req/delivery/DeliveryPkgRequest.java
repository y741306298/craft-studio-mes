package com.mes.application.dto.req.delivery;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DeliveryPkgRequest {

    private List<ProductionPiece> productionPieces;
    private String orderId;
    private String carrierId;//快递id
    private String deliveryManId;
    private String deliverySiidId;
    private String userId;
    private String manufacturerMetaId;
    private String remark;
    private String customerPhone;
    private Date startTime;
    private Date endTime;
    private String carrierName;
    private String materialName;
    private String processName;
    private Double width;


}
