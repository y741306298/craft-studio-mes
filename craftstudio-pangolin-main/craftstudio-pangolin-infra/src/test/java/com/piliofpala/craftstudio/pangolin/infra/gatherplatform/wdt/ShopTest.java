package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.wdt;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import org.junit.jupiter.api.Test;

public class ShopTest {
    @Test
    public void queryShop(){
        var shop1 = GatherPlatform.getInstance(GatherPlatformType.WDT).queryShopByCode("123");
        var shop2 = GatherPlatform.getInstance(GatherPlatformType.WDT).queryShopByCode("TEST");
        System.out.println(shop2);
    }
}
