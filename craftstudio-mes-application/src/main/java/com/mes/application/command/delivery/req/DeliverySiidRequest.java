package com.mes.application.command.delivery.req;

import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliverySiidRequest {

    private String id;

    @NotBlank(message = "SIID不能为空")
    private String siid;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    private String manufacturerMetaId;

    public DeliverySiid toDomainEntity() {
        DeliverySiid deliverySiid = new DeliverySiid();
        deliverySiid.setId(this.id);
        deliverySiid.setSiid(this.siid);
        deliverySiid.setName(this.name);
        deliverySiid.setUserId(this.userId);
        deliverySiid.setManufacturerMetaId(this.manufacturerMetaId);
        return deliverySiid;
    }
}
