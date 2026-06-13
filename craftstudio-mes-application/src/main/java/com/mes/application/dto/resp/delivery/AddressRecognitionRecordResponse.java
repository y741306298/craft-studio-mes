package com.mes.application.dto.resp.delivery;

import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecordStatus;
import com.mes.domain.delivery.deliveryRoute.vo.AddressRecognitionConsignee;
import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.world.vo.World;
import lombok.Data;

import java.util.Date;

@Data
public class AddressRecognitionRecordResponse {

    private String id;
    private Address address;
    private OrgInfo orgInfo;
    private AddressRecognitionConsignee consignee;
    private String fullAddress;
    private String orderId;
    private String routeId;
    private String nodeId;
    private AddressRecognitionRecordStatus status;
    private String statusName;
    private Integer order;
    private Date createTime;
    private Date updateTime;

    public static AddressRecognitionRecordResponse from(AddressRecognitionRecord record, World world) {
        if (record == null) {
            return null;
        }
        AddressRecognitionRecordResponse response = new AddressRecognitionRecordResponse();
        response.setId(record.getId());
        response.setAddress(record.getAddress());
        response.setOrgInfo(record.getOrgInfo());
        response.setConsignee(record.getConsignee());
        if (record.getAddress() != null) {
            response.setFullAddress(record.getAddress().buildFullAddressString(world));
        }
        response.setOrderId(record.getOrderId());
        response.setRouteId(record.getRouteId());
        response.setNodeId(record.getNodeId());
        response.setStatus(record.getStatus());
        response.setStatusName(record.getStatus() == null ? null : record.getStatus().getValue());
        response.setOrder(record.getOrder());
        response.setCreateTime(record.getCreateTime());
        response.setUpdateTime(record.getUpdateTime());
        return response;
    }
}
