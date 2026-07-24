package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.wdt;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsWarehouseConfigException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules.LogisticsPrint;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LogisticsTest {
    @Test
    public void queryLogistics()  throws IOException {
        //WdtClient client = new WdtClient("haijun", "ywyl-test", "jW1D)WmUq!ve", "https://openapi.ali.huice.cc/openapi/");
        WdtClient client = new WdtClient("ywyl", "ywyl-ot", "2b0d48a9a7ee7a23a3ba97948aa8ce6b", "https://openapi.huice.com/openapi/");

        Map<String, String> params = new HashMap<String, String>();
        params.put("logistics_no", "11");

        try {
            String response = client.execute("logistics.php", params);
            System.out.println(response);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 快递换仓
     * @throws IOException
     */
    @Test
    @Disabled("影响真实数据，打包不予执行")
    void configLogisticsWarehouse() throws IOException {
        try {
            GatherPlatform.getInstance(GatherPlatformType.WDT).configLogisticsWarehouse(
                    "2848610592363643041","3","JY2607210016"
            );
        } catch (LogisticsWarehouseConfigException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印快递面单
     */
    @Test
    @Disabled()
    public void printLogistics(){
        GatherPlatform.getInstance(GatherPlatformType.WDT).printLogisticsLabel("JY2607210007");
    }
}
