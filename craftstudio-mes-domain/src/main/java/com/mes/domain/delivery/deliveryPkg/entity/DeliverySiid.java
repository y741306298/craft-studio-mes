package com.mes.domain.delivery.deliveryPkg.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliverySiid extends BaseEntity {

    private String siid;

    private String name;

    private String userId;

    private String manufacturerMetaId;
}
