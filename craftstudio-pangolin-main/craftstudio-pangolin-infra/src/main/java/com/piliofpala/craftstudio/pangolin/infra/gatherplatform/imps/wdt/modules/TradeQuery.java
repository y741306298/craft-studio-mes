package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules;

import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrder;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommercePlatformType;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Consignee;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TradeQuery {

    public String queryTradeIdByTradeNo(WdtClient client, String tradeNo) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("trade_no", tradeNo);
        try {
            String response = client.execute("trade_query.php", params);
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            if(!respObj.containsKey("trades")){
                return null;
            }
            var trades = respObj.getJSONArray("trades");
            if(trades == null || trades.size()!=1){
                return null;
            }
            return trades.getJSONObject(0).getString("trade_id");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public List<EcommerceOrder> queryByTimePeriod(WdtClient client, Date startTime, Date endTime) {
        return queryByTimePeriodPageByPage(0, client, startTime, endTime);
    }

    private List<EcommerceOrder> queryByTimePeriodPageByPage(int current, WdtClient client, Date startTime, Date endTime) {
        int pageSize = 100;
        Map<String, String> params = new HashMap<String, String>();
        params.put("page_no", String.valueOf(current));
        params.put("page_size", String.valueOf(pageSize));
        params.put("start_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime));
        params.put("end_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime));
        List<EcommerceOrder> orders = new ArrayList<>();
        try {
            String response = client.execute("trade_query.php", params);
            var respObj = JSONObject.parseObject(response);
            System.out.println(response);
            if(!respObj.containsKey("trades")){
                return orders;
            }
            var trades = respObj.getJSONArray("trades");
            if(trades == null || trades.isEmpty()){
                return orders;
            }
            for (int i=0; i<trades.size(); i++) {
                EcommerceOrder order = new EcommerceOrder();
                var trade = trades.getJSONObject(i);
                var goodsList = trade.getJSONArray("goods_list");
                if(goodsList == null || goodsList.size() != 1){
                    System.out.println("Bad trade:"+response);
                    continue;
                }
                var goods = goodsList.getJSONObject(0);
                order.setGatherPlatformType(GatherPlatformType.WDT);
                order.setGatherPlatformOrderId(trade.getString("trade_no"));
                order.setShopId(trade.getString("shop_id"));
                order.setShopCode(trade.getString("shop_no"));
                order.setSpecCode(goods.getString("spec_no"));
                order.setSpecName(goods.getString("goods_name"));
                order.setEcommercePlatformType(findEcommercePlatformType(trade.getString("fenxiao_platform_id")));
                order.setEcommerceOrderId(trade.getString("fenxiao_tid"));
                order.setEcommerceShopName(trade.getString("fenxiao_shop_name"));
                order.setBuyerNick(trade.getString("buyer_nick"));
                try {
                    order.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(trade.getString("modified")));
                    order.setPayTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(trade.getString("pay_time")));
                    order.setPlanDeliverTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(trade.getString("plan_deliver_time")));
                }catch (ParseException e){
                    e.printStackTrace();
                }
                Consignee consignee = new Consignee();
                consignee.setName(trade.getString("receiver_name"));
                consignee.setPhone(trade.getString("receiver_mobile"));
                Address address = new Address();
                consignee.setAddress(address);
                address.setDetailAddress(trade.getString("receiver_address"));
                address.setTerminalRegionCode("CN-"+trade.getString("receiver_district"));
                order.setConsignee(consignee);
                orders.add(order);
            }

            int total = respObj.getInteger("total_count");
            if((current+1)*pageSize < total) {
                var nextOrders = queryByTimePeriodPageByPage(current+1,client,startTime, endTime);
                orders.addAll(nextOrders);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * 参考：https://open.wangdian.cn/qjb/open/guide?path=qjbguide_ptdmbb
     * @param fenXiaoPlatformId
     * @return
     */
    public EcommercePlatformType findEcommercePlatformType(String fenXiaoPlatformId) {
        if(fenXiaoPlatformId == null || fenXiaoPlatformId.isBlank()) {
            return EcommercePlatformType.UNKNOWN;
        }
        if("1".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.TB;
        }
        if("3".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.JD;
        }
        if("56".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.XHS;
        }
        if("39".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.PDD;
        }
        if("139".equals(fenXiaoPlatformId)){
            return EcommercePlatformType.DY;
        }

        return EcommercePlatformType.UNKNOWN;
    }
}
