package com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Consignee;

import java.util.Date;

public class EcommerceOrder {
    // 聚单平台相关数据
    private GatherPlatformType gatherPlatformType;
    private String gatherPlatformOrderId;
    private String shopId;
    private String shopCode;//聚单平台和我们系统店铺连接的字段，即：通过此字段判断该订单所属的店铺，并确定所属企业。e.g.YWXC
    private String specCode;//聚单平台和我们系统产品规格连接的字段，即：通过此字段判断该订单对应的产品规格。e.g.MPS-21
    private String specName;
    private Date createTime;

    // 电商平台相关数据
    private EcommercePlatformType ecommercePlatformType;
    private String ecommerceOrderId;
    private String ecommerceShopName;
    private String buyerNick;
    private Date payTime;
    private Date planDeliverTime;

    // 通用数据
    private Consignee consignee;

    public GatherPlatformType getGatherPlatformType() {
        return gatherPlatformType;
    }

    public void setGatherPlatformType(GatherPlatformType gatherPlatformType) {
        this.gatherPlatformType = gatherPlatformType;
    }

    public String getGatherPlatformOrderId() {
        return gatherPlatformOrderId;
    }

    public void setGatherPlatformOrderId(String gatherPlatformOrderId) {
        this.gatherPlatformOrderId = gatherPlatformOrderId;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getShopCode() {
        return shopCode;
    }

    public void setShopCode(String shopCode) {
        this.shopCode = shopCode;
    }

    public String getSpecCode() {
        return specCode;
    }

    public void setSpecCode(String specCode) {
        this.specCode = specCode;
    }

    public String getSpecName() {
        return specName;
    }

    public void setSpecName(String specName) {
        this.specName = specName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public EcommercePlatformType getEcommercePlatformType() {
        return ecommercePlatformType;
    }

    public void setEcommercePlatformType(EcommercePlatformType ecommercePlatformType) {
        this.ecommercePlatformType = ecommercePlatformType;
    }

    public String getEcommerceOrderId() {
        return ecommerceOrderId;
    }

    public void setEcommerceOrderId(String ecommerceOrderId) {
        this.ecommerceOrderId = ecommerceOrderId;
    }

    public String getEcommerceShopName() {
        return ecommerceShopName;
    }

    public void setEcommerceShopName(String ecommerceShopName) {
        this.ecommerceShopName = ecommerceShopName;
    }

    public String getBuyerNick() {
        return buyerNick;
    }

    public void setBuyerNick(String buyerNick) {
        this.buyerNick = buyerNick;
    }

    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    public Date getPlanDeliverTime() {
        return planDeliverTime;
    }

    public void setPlanDeliverTime(Date planDeliverTime) {
        this.planDeliverTime = planDeliverTime;
    }

    public Consignee getConsignee() {
        return consignee;
    }

    public void setConsignee(Consignee consignee) {
        this.consignee = consignee;
    }
}
