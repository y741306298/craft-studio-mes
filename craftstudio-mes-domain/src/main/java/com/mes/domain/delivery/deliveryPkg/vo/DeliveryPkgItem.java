package com.mes.domain.delivery.deliveryPkg.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeliveryPkgItem {

    private String orderItemId;
    private List<String> productionPieceId;
    private Integer quantity;

}
