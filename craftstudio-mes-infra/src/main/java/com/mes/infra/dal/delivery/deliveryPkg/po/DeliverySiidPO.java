package com.mes.infra.dal.delivery.deliveryPkg.po;

import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "deliverySiid")
public class DeliverySiidPO extends BasePO<DeliverySiid> {

    private String siid;

    private String name;

    private String userId;

    private String manufacturerMetaId;

    @Override
    public DeliverySiid toDO() {
        DeliverySiid deliverySiid = new DeliverySiid();
        copyBaseFieldsToDO(deliverySiid);

        deliverySiid.setSiid(this.siid);
        deliverySiid.setName(this.name);
        deliverySiid.setManufacturerMetaId(this.manufacturerMetaId);
        deliverySiid.setUserId(this.userId);

        return deliverySiid;
    }

    @Override
    protected BasePO<DeliverySiid> fromDO(DeliverySiid _do) {
        if (_do == null) {
            return null;
        }

        this.siid = _do.getSiid();
        this.name = _do.getName();
        this.manufacturerMetaId = _do.getManufacturerMetaId();
        this.userId = _do.getUserId();

        return this;
    }
}
