package com.mes.domain.delivery.deliveryPkg.vo;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AuthOrderRequest {

    private String method;

    private String key;

    private String sign;

    private String t;

    private String manufacturerId;

    private String deliveryManid;

    private List<orderItemVo> orderItems;

    private String kuaidiWay;

    private String remark;

    private String cargo;

    private Integer count;

    private OrderParam param;

    private PrintParam printParam;

    private FdParam fdParam;

    private String expType;

    private String payType;

    private String siid;

    private String kuaidinum;

    @Data
    public class orderItemVo{
        private String orderItemId;
        private Integer quantity;
    }


    @Data
    public class OrderParam {

        //打印类型，NON：只下单不打印（默认）； IMAGE:生成图片短链；HTML:生成html短链； CLOUD:使用快递100云打印机打印，使用CLOUD时siid必填
        private String printType;

        //电子面单客户账户或月结账号，需贵司向当地快递公司网点申请（参考电子面单申请指南
        private String partnerId;

        private String partnerKey;

        private String partnerSecret;

        private String partnerName;

        private String net;

        private String code;

        private String checkMan;

        private String kuaidicom;

        private DeliveryManInfo recMan;//收件人信息

        private DeliveryMan sendMan; //寄件人信息

        private String cargo;

        private int count;

        private Double weight;

        private String payType;

        private String expType;

        private String remark;

        private String siid;

        private String direction;

        private String tempId;

        private String childTempld;

        private String backTempld;

        private Double valinsPay;

        private Double collection;

        private String needChild;

        private String needBack;

        private String orderId;

        private boolean reorder;

        private String callBackUrl;

        private String salt;

        private boolean needSubscribe;

        private String pollCallBackUrl;

        private String resultv2;

        private boolean needDesensitization;

        private boolean needLogo;

        private String thirdOrderId;

        private String oaid;

        private String caid;

        private String thirdTemplateURL;

        private String thirdCustomTemplateUrl;

        private Map<String,String> customParam;

        private boolean needOcr;

        private String[] ocrInclude;

        private String height;

        private String width;

    }

    @Data
    public class PrintParam {

        private String tempid;

        private String printType;

        private String siid;

        private String direction;

        private String callBackUrl;

        private Map<String,Object> customParam;

    }

    @Data
    public class FdParam {

        private String taskId;

        private String siid;

    }

    public OrderParam createOrderParam(AuthOrderRequest request, DeliveryToken deliveryToken){
        OrderParam orderParam = new OrderParam();
        orderParam.setPrintType("CLOUD");
        orderParam.setPartnerId(deliveryToken.getPartnerId());
        orderParam.setTempId(deliveryToken.getTempId());
        orderParam.setKuaidicom(deliveryToken.getKuaidicom());
        orderParam.setPartnerKey(deliveryToken.getPartnerKey());
        orderParam.setPartnerSecret(deliveryToken.getPartnerSecret());
        orderParam.setNet(deliveryToken.getNet());
        orderParam.setCode(deliveryToken.getCode());
        if (deliveryToken.getSiid() != null) {
            orderParam.setSiid(deliveryToken.getSiid());
        }else{
            //预打包默认
            orderParam.setSiid("KX100L3AD65411C274");
        }
        orderParam.setCount(request.getCount());

        orderParam.setCargo(request.getCargo());
        orderParam.setNeedLogo(true);
        String expType = request.getExpType();
        if (expType == null) {
//            expType = "顺丰标快";
            expType = deliveryToken.getExpType();
        }
        orderParam.setExpType(expType);

        String payType = request.getPayType();
        if (payType == null) {
            payType = deliveryToken.getPayType();
        }

        orderParam.setPayType(payType);
        //特殊处理名字，去掉备注
        DeliveryManInfo recMan = request.getParam().getRecMan();
        DeliveryManInfo recManInfo  = new DeliveryManInfo();
        String recname = recMan.getName();
        if (recname.contains("#_")) {
            String[] parts = recname.split("#_");
            String lastPart = parts[parts.length - 1];
            String firstPart = parts[0];
            recname= lastPart;
            recManInfo.setName(lastPart);
        } else {
            recManInfo.setName(recMan.getName());
        }
        String remark = request.getRemark();
        orderParam.setRemark(remark);
        recManInfo.setMobile(recMan.getMobile());
        recManInfo.setPrintAddr(recMan.getPrintAddr());
        orderParam.setRecMan(recManInfo);
        return orderParam;
    }


}


