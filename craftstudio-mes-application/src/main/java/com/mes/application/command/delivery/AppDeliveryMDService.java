package com.mes.application.command.delivery;


import com.alibaba.fastjson.JSON;
import com.mes.application.shared.utils.MD5Util;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import com.mes.domain.delivery.deliveryPkg.service.DeliveryManService;
import com.mes.domain.delivery.deliveryPkg.service.DeliveryTokenService;
import com.mes.domain.delivery.deliveryPkg.vo.AuthOrderRequest;
import com.mes.domain.delivery.deliveryPkg.vo.AuthOrderResponse;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;

@Service
public class AppDeliveryMDService {

    private static Logger logger = LoggerFactory.getLogger(AppDeliveryMDService.class);

    private final static String DELIVERYKEY = "lCnjtXBY2496";
    private final static String DELIVERYCUSTOMER = "DAAB0437EF6D9C03B8B4FC96C165FFB1";
    private final static String DELIVERYSECRET = "8868a7ce6733416b844d964b56bb716f";
    private final static String MAPTRACKURL = "https://poll.kuaidi100.com/poll/maptrack.do";

    @Autowired
    private DeliveryTokenService deliveryTokenService;

    @Autowired
    private DeliveryManService deliveryManService;

    @Autowired
    private OrderItemService orderItemService;

    public void getDZMDPreview(AuthOrderRequest request) {
        String url = "https://api.kuaidi100.com/label/order";
        
        // 1. 根据 manufacturerId 和 kuaidiWay 查询对应的 token
        List<DeliveryToken> tokens = deliveryTokenService.list(1, 100);
        DeliveryToken token = tokens.stream()
                .filter(t -> request.getManufacturerId().equals(t.getPartnerId()) 
                        && request.getKuaidiWay().equals(t.getKuaidicom()))
                .findFirst()
                .orElse(null);
        
        if (token == null) {
            logger.warn("未找到对应的电子面单令牌配置，manufacturerId: {}, kuaidiWay: {}", 
                    request.getManufacturerId(), request.getKuaidiWay());
            throw new RuntimeException("未找到对应的电子面单令牌配置");
        }
        
        // 2. 根据 deliveryManid 查询快递员信息
        DeliveryMan deliveryMan = null;
        if (StringUtils.isNotBlank(request.getDeliveryManid())) {
            deliveryMan = deliveryManService.findById(request.getDeliveryManid());
            if (deliveryMan == null) {
                logger.warn("未找到对应的快递员信息，deliveryManid: {}", request.getDeliveryManid());
                throw new RuntimeException("未找到对应的快递员信息");
            }
        }
        
        // 3. 构建请求参数
        AuthOrderRequest.OrderParam orderParam = request.createOrderParam(request, token);
        orderParam.setSendMan(deliveryMan);
        String paramStr = JSON.toJSONString(request);
        // 5. 调用快递100 API
        String result = callPost(url, paramStr, "label.order");
        logger.info("电子面单预览结果: {}", result);
        AuthOrderResponse response = JSON.parseObject(result, AuthOrderResponse.class);
        
        // 6. 获取快递单号并更新订单项
        String kuaidinum = response.getData().getKuaidinum();
        if (StringUtils.isNotBlank(kuaidinum) && request.getOrderItems() != null) {
            for (AuthOrderRequest.orderItemVo orderItemVo : request.getOrderItems()) {
                String orderItemId = orderItemVo.getOrderItemId();
                if (StringUtils.isNotBlank(orderItemId)) {
                    OrderItem orderItem = orderItemService.findByOrderItemId(orderItemId);
                    if (orderItem != null) {
                        orderItem.setKuaidiNum(kuaidinum);
                        orderItemService.updateOrderItem(orderItem);
                        logger.info("已更新订单项 {} 的快递单号: {}", orderItemId, kuaidinum);
                    } else {
                        logger.warn("订单项不存在: {}", orderItemId);
                    }
                }
            }
        }
    }


    private String callPost(String url,String paramStr,String method){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type","application/x-www-form-urlencoded");
        MultiValueMap param = new LinkedMultiValueMap();
        long t = new Date().getTime();
        String sign = this.getSign(paramStr, t);
        param.add("method",method);
        param.add("key",DELIVERYKEY);
        param.add("sign",sign);
        param.add("t",t);
        param.add("param",paramStr);
        HttpEntity httpEntity = new HttpEntity<>(param,headers);
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.postForObject(url,httpEntity, String.class);
        return result;
    }

    private String getSign(String paramStr,long t){
        String timeStr = String.valueOf(t);
        String signStr = paramStr+timeStr+DELIVERYKEY+DELIVERYSECRET;
        String sign = MD5Util.stringToMD5(signStr);
        return sign;
    }

    private String getMapSign(String paramStr){
        String key = DELIVERYKEY;
        String customer = DELIVERYCUSTOMER;
        String signStr = paramStr+key+customer;
        String sign = MD5Util.stringToMD5(signStr);
        return sign;
    }

}
