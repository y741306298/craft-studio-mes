package com.mes.application.command.delivery.resp;

import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;
import lombok.Data;

import java.util.Date;

@Data
public class DeliverySiidResponse {

    private String id;
    private String siid;
    private String name;
    private String userId;
    private Date createTime;
    private Date updateTime;

    public static DeliverySiidResponse from(DeliverySiid deliverySiid) {
        if (deliverySiid == null) {
            return null;
        }
        DeliverySiidResponse response = new DeliverySiidResponse();
        response.setId(deliverySiid.getId());
        response.setSiid(deliverySiid.getSiid());
        response.setName(deliverySiid.getName());
        response.setUserId(deliverySiid.getUserId());
        response.setCreateTime(deliverySiid.getCreateTime());
        response.setUpdateTime(deliverySiid.getUpdateTime());
        return response;
    }
}
