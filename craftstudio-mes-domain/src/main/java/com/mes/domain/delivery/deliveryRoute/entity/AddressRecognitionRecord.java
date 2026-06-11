package com.mes.domain.delivery.deliveryRoute.entity;

import com.mes.domain.base.BaseEntity;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AddressRecognitionRecord extends BaseEntity {

    private Address address;
    private String routeId;
    private String nodeId;
    private AddressRecognitionRecordStatus status;
}
