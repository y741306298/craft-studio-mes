package com.mes.domain.manufacturer.productionPiece.entity;

import lombok.Data;

@Data
public class DeliveryPkgInfo {

    private String kuaidiNum;
    private String carrierId;
    private String carrierName;
    private Integer quantity;

}
