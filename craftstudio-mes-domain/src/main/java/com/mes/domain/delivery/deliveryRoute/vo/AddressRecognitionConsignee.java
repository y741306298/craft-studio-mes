package com.mes.domain.delivery.deliveryRoute.vo;

import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import lombok.Data;

@Data
public class AddressRecognitionConsignee {
    private String name;
    private String phone;
    private Address address;
}
