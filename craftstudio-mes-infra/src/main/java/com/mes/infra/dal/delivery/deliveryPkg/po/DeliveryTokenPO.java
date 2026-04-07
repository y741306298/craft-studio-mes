package com.mes.infra.dal.delivery.deliveryPkg.po;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 电子面单令牌配置持久化对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "delivery_token")
public class DeliveryTokenPO extends BasePO<DeliveryToken> {

    /**
     * 打印类型
     */
    private String printType;

    /**
     * 月结账号
     */
    private String partnerId;

    /**
     * 电子面单密码
     */
    private String partnerKey;

    /**
     * 电子面单密钥
     */
    private String partnerSecret;

    /**
     * 电子面单客户账户名称
     */
    private String partnerName;

    /**
     * 企业
     */
    private String eid;

    /**
     * 网点 ID
     */
    private String siid;

    /**
     * 收件网点名称
     */
    private String net;

    /**
     * 电子面单承载编号
     */
    private String code;

    /**
     * 电子面单承载快递员名
     */
    private String checkMan;

    /**
     * 网点名称/网点编号
     */
    private String tbNet;

    /**
     * 快递方式
     */
    private String kuaidiWay;

    /**
     * 快递公司的编码
     */
    private String kuaidicom;

    /**
     * 主单模板
     */
    private String tempId;

    /**
     * 子单模板
     */
    private String childTempId;

    /**
     * 回单模板
     */
    private String backTempId;

    /**
     * 运输方式
     */
    private String expType;

    /**
     * 支付方式
     */
    private String payType;

    @Override
    public DeliveryToken toDO() {
        DeliveryToken token = new DeliveryToken();
        copyBaseFieldsToDO(token);
        
        token.setPrintType(this.printType);
        token.setPartnerId(this.partnerId);
        token.setPartnerKey(this.partnerKey);
        token.setPartnerSecret(this.partnerSecret);
        token.setPartnerName(this.partnerName);
        token.setEid(this.eid);
        token.setSiid(this.siid);
        token.setNet(this.net);
        token.setCode(this.code);
        token.setCheckMan(this.checkMan);
        token.setTbNet(this.tbNet);
        token.setKuaidiWay(this.kuaidiWay);
        token.setKuaidicom(this.kuaidicom);
        token.setTempId(this.tempId);
        token.setChildTempId(this.childTempId);
        token.setBackTempId(this.backTempId);
        token.setExpType(this.expType);
        token.setPayType(this.payType);
        
        return token;
    }

    @Override
    protected BasePO<DeliveryToken> fromDO(DeliveryToken _do) {
        if (_do == null) {
            return null;
        }
        
        this.printType = _do.getPrintType();
        this.partnerId = _do.getPartnerId();
        this.partnerKey = _do.getPartnerKey();
        this.partnerSecret = _do.getPartnerSecret();
        this.partnerName = _do.getPartnerName();
        this.eid = _do.getEid();
        this.siid = _do.getSiid();
        this.net = _do.getNet();
        this.code = _do.getCode();
        this.checkMan = _do.getCheckMan();
        this.tbNet = _do.getTbNet();
        this.kuaidiWay = _do.getKuaidiWay();
        this.kuaidicom = _do.getKuaidicom();
        this.tempId = _do.getTempId();
        this.childTempId = _do.getChildTempId();
        this.backTempId = _do.getBackTempId();
        this.expType = _do.getExpType();
        this.payType = _do.getPayType();
        
        return this;
    }
}
