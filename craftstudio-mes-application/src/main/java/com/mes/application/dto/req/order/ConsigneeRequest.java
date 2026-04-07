package com.mes.application.dto.req.order;

import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import lombok.Data;

@Data
public class ConsigneeRequest {
    private String name;
    private String phone;
    private Address address;

    public OrderCustomer toOrderCustomer() {
        OrderCustomer customer = new OrderCustomer();
        customer.setCustomerName(name);
        customer.setCustomerPhone(phone);
        customer.setAddress(address);
        return customer;
    }

    public String getDetailAddress(){
        return address.getDetailAddress();
    }
}
