package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.wdt;

import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrder;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GatherOrderTest {

    @Test
    public void gatherOrders() throws Exception{
        Date startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2026-07-20 00:00:00");
        Date endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2026-07-22 00:00:00");
        List<EcommerceOrder> orders = GatherPlatform.getInstance(GatherPlatformType.WDT).gatherOrders(startTime, endTime);
        System.out.println("orders size:"+orders.size());
    }
}
