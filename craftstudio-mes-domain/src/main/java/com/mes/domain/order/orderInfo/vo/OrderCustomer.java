package com.mes.domain.order.orderInfo.vo;

import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import lombok.Data;

@Data
public class OrderCustomer {

    private String customerId;
    private String customerName;
    private String customerPhone;
    private Address address;


}
