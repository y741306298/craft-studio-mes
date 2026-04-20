package com.mes.application.command.delivery.resp;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import lombok.Data;

import java.util.Date;

@Data
public class DeliveryManResponse {

    private String id;
    private String userId;
    private String name;
    private String mobile;
    private String tel;
    private String printAddr;
    private Date createTime;
    private Date updateTime;

    public static DeliveryManResponse from(DeliveryMan deliveryMan) {
        if (deliveryMan == null) {
            return null;
        }
        DeliveryManResponse response = new DeliveryManResponse();
        response.setId(deliveryMan.getId());
        response.setUserId(deliveryMan.getUserId());
        response.setName(deliveryMan.getName());
        response.setMobile(deliveryMan.getMobile());
        response.setTel(deliveryMan.getTel());
        response.setPrintAddr(deliveryMan.getPrintAddr());
        response.setCreateTime(deliveryMan.getCreateTime());
        response.setUpdateTime(deliveryMan.getUpdateTime());
        return response;
    }
}
