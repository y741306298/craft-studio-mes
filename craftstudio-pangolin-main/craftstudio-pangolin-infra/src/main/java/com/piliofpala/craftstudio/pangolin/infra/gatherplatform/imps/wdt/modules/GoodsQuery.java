package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules;

import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.entity.ProductSpec;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GoodsQuery {
    public ProductSpec queryProductSpecByCode(WdtClient client, String specCode) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("spec_no", specCode);
        try {
            String response = client.execute("goods_query.php", params);
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            var goodsList = respObj.getJSONArray("goods_list");
            if(goodsList == null || goodsList.size()!=1){
                return null;
            }
            var goodsObj = goodsList.getJSONObject(0);
            var specList = goodsObj.getJSONArray("spec_list");
            if(specList == null || specList.size()!=1){
                return null;
            }
            var specObj = specList.getJSONObject(0);
            return new ProductSpec(
                    specObj.getString("spec_name"), specObj.getString("spec_no"),
                    specObj.getString("img_url"), goodsObj.getString("brand_name")
            );
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
