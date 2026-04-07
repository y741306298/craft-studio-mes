package com.mes.domain.delivery.deliveryPkg.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryRecord extends BaseEntity {

    private List<String> orderItemId;
    private String tokenId;

}
