package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules;

import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsWarehouseConfigException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LogisticsWarehouseModify {
    public void modify(WdtClient client,  String logisticsId, String warehouseId, String tradeNo) throws LogisticsWarehouseConfigException{
        Map<String, String> params = new HashMap<String, String>();

        params.put("trade_no", tradeNo);
        params.put("warehouse_id", warehouseId);
        params.put("logistics_id", logisticsId);

        try {
            String response = client.execute("sales_trade_modify.php", params);
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            int code = respObj.getInteger("code");
            if(code!=0){
                throw new LogisticsWarehouseConfigException(respObj.getString("message"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new LogisticsWarehouseConfigException(e.getMessage());
        }
    }
}
