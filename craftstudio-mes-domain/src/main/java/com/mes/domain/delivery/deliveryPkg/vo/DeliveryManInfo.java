package com.mes.domain.delivery.deliveryPkg.vo;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import lombok.Data;

@Data
public class DeliveryManInfo {

    private String name;

    private String mobile;

    private String tel;

    private String printAddr;

    private String company;

    public static DeliveryManInfo fromDeliveryMan(DeliveryMan deliveryMan){
        DeliveryManInfo deliveryManInfo = new DeliveryManInfo();
        deliveryManInfo.setName(deliveryMan.getName());
        deliveryManInfo.setMobile(deliveryMan.getMobile());
        deliveryManInfo.setTel(deliveryMan.getTel());
        deliveryManInfo.setPrintAddr(deliveryMan.getPrintAddr());
        deliveryManInfo.setCompany(deliveryMan.getManufacturerMetaId());
        return deliveryManInfo;
    }

    public static DeliveryManInfo fromOrderCustomer(OrderCustomer orderCustomer){
        DeliveryManInfo deliveryManInfo = new DeliveryManInfo();
        deliveryManInfo.setName(orderCustomer.getCustomerName());
        deliveryManInfo.setMobile(orderCustomer.getCustomerPhone());
        deliveryManInfo.setTel(orderCustomer.getCustomerPhone());
        deliveryManInfo.setPrintAddr(orderCustomer.getAddress().getDetailAddress());
        deliveryManInfo.setCompany(null);
        return deliveryManInfo;
    }

}
