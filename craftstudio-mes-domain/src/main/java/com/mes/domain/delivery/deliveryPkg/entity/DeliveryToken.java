package com.mes.domain.delivery.deliveryPkg.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 电子面单令牌配置
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryToken extends BaseEntity {

    private String carrierId;

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
    private String manufacturerMetaId;
    
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
}
