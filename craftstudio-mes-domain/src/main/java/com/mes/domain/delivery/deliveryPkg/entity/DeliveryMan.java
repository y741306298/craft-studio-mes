package com.mes.domain.delivery.deliveryPkg.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryMan extends BaseEntity {

    private String name;

    private String mobile;

    private String tel;

    private String printAddr;

    private String company;
}
