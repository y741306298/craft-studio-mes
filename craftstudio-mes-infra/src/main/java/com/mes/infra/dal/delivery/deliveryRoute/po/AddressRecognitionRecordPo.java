package com.mes.infra.dal.delivery.deliveryRoute.po;

import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecordStatus;
import com.mes.domain.delivery.deliveryRoute.vo.AddressRecognitionConsignee;
import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import com.mes.infra.base.BasePO;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "addressRecognitionRecord")
public class AddressRecognitionRecordPo extends BasePO<AddressRecognitionRecord> {

    private String manufacturerMetaId;
    private Address address;
    private OrgInfo orgInfo;
    private AddressRecognitionConsignee consignee;
    private String orderId;
    private String routeId;
    private String nodeId;
    private String status;
    private Integer order;

    @Override
    public AddressRecognitionRecord toDO() {
        AddressRecognitionRecord record = new AddressRecognitionRecord();
        copyBaseFieldsToDO(record);
        record.setManufacturerMetaId(this.manufacturerMetaId);
        record.setAddress(this.address);
        record.setOrgInfo(this.orgInfo);
        record.setConsignee(this.consignee);
        record.setOrderId(this.orderId);
        record.setRouteId(this.routeId);
        record.setNodeId(this.nodeId);
        record.setOrder(this.order);
        if (this.status != null) {
            record.setStatus(AddressRecognitionRecordStatus.fromValue(this.status));
        }
        return record;
    }

    @Override
    protected BasePO<AddressRecognitionRecord> fromDO(AddressRecognitionRecord _do) {
        if (_do == null) {
            return null;
        }
        this.manufacturerMetaId = _do.getManufacturerMetaId();
        this.address = _do.getAddress();
        this.orgInfo = _do.getOrgInfo();
        this.consignee = _do.getConsignee();
        this.orderId = _do.getOrderId();
        this.routeId = _do.getRouteId();
        this.nodeId = _do.getNodeId();
        this.order = _do.getOrder();
        this.status = _do.getStatus() == null ? null : _do.getStatus().getValue();
        return this;
    }
}
