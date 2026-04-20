package com.mes.infra.dal.delivery.deliveryPkg.po;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "deliveryMan")
public class DeliveryManPO extends BasePO<DeliveryMan> {

    private String name;

    private String mobile;

    private String tel;

    private String printAddr;

    private String userId;

    private String manufacturerMetaId;

    @Override
    public DeliveryMan toDO() {
        DeliveryMan deliveryMan = new DeliveryMan();
        copyBaseFieldsToDO(deliveryMan);

        deliveryMan.setName(this.name);
        deliveryMan.setMobile(this.mobile);
        deliveryMan.setTel(this.tel);
        deliveryMan.setPrintAddr(this.printAddr);
        deliveryMan.setUserId(this.userId);
        deliveryMan.setManufacturerMetaId(this.manufacturerMetaId);

        return deliveryMan;
    }

    @Override
    protected BasePO<DeliveryMan> fromDO(DeliveryMan _do) {
        if (_do == null) {
            return null;
        }

        this.name = _do.getName();
        this.mobile = _do.getMobile();
        this.tel = _do.getTel();
        this.printAddr = _do.getPrintAddr();
        this.userId = _do.getUserId();
        this.manufacturerMetaId = _do.getManufacturerMetaId();

        return this;
    }
}
