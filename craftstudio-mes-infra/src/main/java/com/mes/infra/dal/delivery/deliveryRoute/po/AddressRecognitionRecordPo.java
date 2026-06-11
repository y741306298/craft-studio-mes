package com.mes.infra.dal.delivery.deliveryRoute.po;

import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecordStatus;
import com.mes.infra.base.BasePO;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "addressRecognitionRecord")
public class AddressRecognitionRecordPo extends BasePO<AddressRecognitionRecord> {

    private Address address;
    private String orderId;
    private String routeId;
    private String nodeId;
    private String status;

    @Override
    public AddressRecognitionRecord toDO() {
        AddressRecognitionRecord record = new AddressRecognitionRecord();
        copyBaseFieldsToDO(record);
        record.setAddress(this.address);
        record.setOrderId(this.orderId);
        record.setRouteId(this.routeId);
        record.setNodeId(this.nodeId);
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
        this.address = _do.getAddress();
        this.orderId = _do.getOrderId();
        this.routeId = _do.getRouteId();
        this.nodeId = _do.getNodeId();
        this.status = _do.getStatus() == null ? null : _do.getStatus().getValue();
        return this;
    }
}
