package com.mes.infra.dal.order.orderTransferRecord.po;

import com.mes.domain.order.orderTransferRecord.entity.OrderTransferRecord;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "orderTransferRecord")
public class OrderTransferRecordPo extends BasePO<OrderTransferRecord> {

    private String orderId;
    private String sourceId;
    private String sourceName;
    private String targetId;
    private String targetName;
    private String orderItemId;
    private String previewUrl;
    private Integer quantity;

    @Override
    public OrderTransferRecord toDO() {
        OrderTransferRecord record = new OrderTransferRecord();
        record.setId(getId());
        record.setCreateTime(getCreateTime());
        record.setUpdateTime(getUpdateTime());
        record.setOrderId(this.orderId);
        record.setSourceId(this.sourceId);
        record.setSourceName(this.sourceName);
        record.setTargetId(this.targetId);
        record.setTargetName(this.targetName);
        record.setOrderItemId(this.orderItemId);
        record.setPreviewUrl(this.previewUrl);
        record.setQuantity(this.quantity);
        return record;
    }

    @Override
    protected BasePO<OrderTransferRecord> fromDO(OrderTransferRecord _do) {
        if (_do == null) {
            return null;
        }
        this.orderId = _do.getOrderId();
        this.sourceId = _do.getSourceId();
        this.sourceName = _do.getSourceName();
        this.targetId = _do.getTargetId();
        this.targetName = _do.getTargetName();
        this.orderItemId = _do.getOrderItemId();
        this.previewUrl = _do.getPreviewUrl();
        this.quantity = _do.getQuantity();
        return this;
    }
}
