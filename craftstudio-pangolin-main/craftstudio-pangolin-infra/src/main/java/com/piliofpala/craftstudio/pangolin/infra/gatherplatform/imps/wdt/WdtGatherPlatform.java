package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt;

import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrder;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.entity.ProductSpec;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.BrandNotExistException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.ProductSpecPushException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.shop.entity.Shop;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsWarehouseConfigException;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsLabel;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules.*;

import java.util.Date;
import java.util.List;

public class WdtGatherPlatform extends GatherPlatform {
    private final WdtClient client = new WdtClient("ywyl", "ywyl-ot", "2b0d48a9a7ee7a23a3ba97948aa8ce6b", "https://openapi.huice.com/openapi/");

    private static final WdtGatherPlatform instance = new WdtGatherPlatform();
    public static WdtGatherPlatform getInstance(){
        return instance;
    }

    private WdtGatherPlatform() {
    }

    @Override
    public Shop queryShopByCode(String shopCode) {
        return new ShopQuery().queryShopByCode(client, shopCode);
    }

    @Override
    public void pushProductSpec(ProductSpec productSpec) throws BrandNotExistException, ProductSpecPushException {
        new GoodsPush().push(client, productSpec);
    }

    @Override
    public ProductSpec queryProductSpecByCode(String specCode) {
        return new GoodsQuery().queryProductSpecByCode(client, specCode);
    }

    @Override
    public List<EcommerceOrder> gatherOrders(Date startTime, Date endTime) {
        return new TradeQuery().queryByTimePeriod(client, startTime, endTime);
    }

    @Override
    public void configLogisticsWarehouse(String logisticsId, String warehouseId, String uniCode) throws LogisticsWarehouseConfigException {
        new LogisticsWarehouseModify().modify(client, logisticsId, warehouseId, uniCode);
    }

    @Override
    public LogisticsLabel printLogisticsLabel(String uniCode) {
        return new LogisticsPrint().print(client, uniCode);
    }
}
