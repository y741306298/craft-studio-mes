package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.wdt;

import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules.TradePush;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules.TradeQuery;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OrderTest {
    @Test
    @Disabled("影响真实数据，打包不予执行")
    public void auditOrder () {
        WdtClient client = new WdtClient("haijun", "ywyl-test", "jW1D)WmUq!ve", "https://openapi.ali.huice.cc/openapi/");

        Map<String, String> params = new HashMap<String, String>();
        params.put("order_type", "1");
        params.put("type", "2");
        params.put("order_no", "JY2607100005");

        try {
            String response = client.execute("order_audit.php", params);
            System.out.println(response);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    @Disabled("影响真实数据，打包不予执行")
    public void createOriginalOrder () {
        new TradePush().pushTrade();
    }
}
