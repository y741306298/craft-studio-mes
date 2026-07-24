package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.entity.ProductSpec;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.BrandNotExistException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.ProductSpecPushException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GoodsPush {
    public void push(WdtClient client, ProductSpec productSpec) throws BrandNotExistException, ProductSpecPushException {

        Map<String, Object>[] goods_list = new Map[1];
        Map<String, Object>[] spec_list = new Map[1];

        spec_list[0] = new HashMap<>();
        spec_list[0].put("spec_no", productSpec.getCode());
        spec_list[0].put("spec_code", productSpec.getCode());
        spec_list[0].put("spec_name", productSpec.getName());
        spec_list[0].put("img_url", productSpec.getCover());

        goods_list[0] = new HashMap<String,Object>();
        goods_list[0].put("goods_no", "SPU-"+productSpec.getCode());
        goods_list[0].put("goods_type","1");
        goods_list[0].put("goods_name", productSpec.getName());
        goods_list[0].put("spu_img_url",  productSpec.getCover());

        goods_list[0].put("spec_list", spec_list);

        goods_list[0].put("brand_name", productSpec.getBrand());

        //通过第三方json解析工具类fastjson将map解析成json
        String goodsListJson = JSON.toJSONString(goods_list);

        Map<String, String> params = new HashMap<String, String>();
        params.put("goods_list", goodsListJson);
        try {
            String response = client.execute("goods_push.php", params);
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            int code = respObj.getInteger("code");
            if(code!=0){
                if(code == 7210){
                    throw new BrandNotExistException(respObj.getString("message"));
                }else {
                    throw new ProductSpecPushException(respObj.getString("message"));
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new ProductSpecPushException(e.getMessage());
        }
    }
}
