package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.wdt;

import com.alibaba.fastjson2.JSON;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WdtClientTest {

    @Test
    void signRequestShouldSortParametersAndIgnoreExistingSign() throws IOException {
        WdtClient client = new WdtClient("ywyl", "ywyl-ot", "2b0d48a9a7ee7a23a3ba97948aa8ce6b", "https://openapi.huice.com/openapi/");

        List<Map<String, Object>> trade_list = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> order_list = new ArrayList<Map<String, Object>>();

        Map<String, Object> order_1 = new HashMap<String, Object>();
        order_1.put("oid", "oid-"+System.currentTimeMillis());
        order_1.put("num", 1);
        order_1.put("price", 12);
        order_1.put("status", 30);
        order_1.put("refund_status", 0);
        order_1.put("goods_id", "temp_goods_id");
        order_1.put("spec_id", "temp_spec_id");
        order_1.put("goods_no", "temp_goods_no");
        order_1.put("spec_no", "temp_spec_no");
        order_1.put("goods_name", "临时单通用标品");
        order_1.put("discount", 0);		//子订单折扣
        order_1.put("adjust_amount", 0);	//手工调整,特别注意:正的表示加价,负的表示减价
        order_1.put("share_discount", 0);


        order_list.add(order_1);

        Map<String, Object> trade_1 = new HashMap<String, Object>();
        trade_1.put("tid", "tid-"+System.currentTimeMillis());
        trade_1.put("trade_status", 30);
        trade_1.put("pay_status", "1");
        trade_1.put("delivery_term", 1);
        trade_1.put("trade_time", "2025-12-11 14:21:00");
        trade_1.put("buyer_nick", "完全定制产品模版");
        trade_1.put("receiver_province", "河南省");
        trade_1.put("receiver_city", "周口市");
        trade_1.put("receiver_district", "川汇区");
        trade_1.put("receiver_address", "123");
        trade_1.put("receiver_mobile", "13898176276");
        trade_1.put("receiver_name", "蔡徐坤");

        trade_1.put("logistics_type", 54);
        trade_1.put("post_amount", 12);
        trade_1.put("cod_amount", 2);
        trade_1.put("ext_cod_fee", 0);
        trade_1.put("other_amount", 1);
        trade_1.put("paid", 0);
        trade_1.put("order_list", order_list);
//        trade_1.put("is_auto_wms",1);
//        trade_1.put("wms_type",0);


        trade_list.add(trade_1);

        String trade_list_json = JSON.toJSONString(trade_list);
        //System.out.println(purchase_info_json);

        Map<String, String> params = new HashMap<String, String>();
        params.put("shop_id", "2");
        params.put("trade_list", trade_list_json);
        try {
            String response = client.execute("trade_push.php", params);
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
