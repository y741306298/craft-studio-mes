package com.mes.application.command.delivery.req;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryManRequest {

    private String id;

    private String manufacturerMetaId;

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String mobile;

    private String tel;

    private String printAddr;

    public DeliveryMan toDomainEntity() {
        DeliveryMan deliveryMan = new DeliveryMan();
        deliveryMan.setId(this.id);
        deliveryMan.setUserId(this.userId);
        deliveryMan.setName(this.name);
        deliveryMan.setMobile(this.mobile);
        deliveryMan.setTel(this.tel);
        deliveryMan.setManufacturerMetaId(this.manufacturerMetaId);
        deliveryMan.setPrintAddr(this.printAddr);
        return deliveryMan;
    }
}
