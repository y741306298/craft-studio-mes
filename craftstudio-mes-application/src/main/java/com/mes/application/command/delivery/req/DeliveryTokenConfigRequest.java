package com.mes.application.command.delivery.req;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 电子面单完整配置；不接受数据库 ID 和审计字段。 */
@Getter
@Setter
public class DeliveryTokenConfigRequest {
    @NotBlank(message = "carrierId不能为空")
    private String carrierId;

    @NotBlank(message = "manufacturerMetaId不能为空")
    private String manufacturerMetaId;

    @NotBlank(message = "partnerId不能为空")
    private String partnerId;

    @NotBlank(message = "kuaidicom不能为空")
    private String kuaidicom;

    private String printType;

    private String partnerKey;

    private String partnerSecret;

    private String partnerName;

    private String siid;

    private String net;

    private String code;

    private String checkMan;

    private String tbNet;

    private String kuaidiWay;

    private String tempId;

    private String childTempId;

    private String backTempId;

    private String expType;

    private String payType;

    public DeliveryToken toDomainEntity() {
        DeliveryToken token = new DeliveryToken();
        token.setCarrierId(this.carrierId);
        token.setManufacturerMetaId(this.manufacturerMetaId);
        token.setPartnerId(this.partnerId);
        token.setKuaidicom(this.kuaidicom);
        token.setPrintType(this.printType);
        token.setPartnerKey(this.partnerKey);
        token.setPartnerSecret(this.partnerSecret);
        token.setPartnerName(this.partnerName);
        token.setSiid(this.siid);
        token.setNet(this.net);
        token.setCode(this.code);
        token.setCheckMan(this.checkMan);
        token.setTbNet(this.tbNet);
        token.setKuaidiWay(this.kuaidiWay);
        token.setTempId(this.tempId);
        token.setChildTempId(this.childTempId);
        token.setBackTempId(this.backTempId);
        token.setExpType(this.expType);
        token.setPayType(this.payType);
        return token;
    }
}
