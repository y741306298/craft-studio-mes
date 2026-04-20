package com.mes.application.command.delivery.req;

import com.mes.application.dto.req.delivery.DeliveryPkgRequest;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import com.mes.domain.delivery.deliveryPkg.vo.DeliveryManInfo;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import lombok.Data;

import java.util.Map;

@Data
public class Kuaidi100OrderParam {

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

    public static Kuaidi100OrderParam createKuaidi100OrderParam(DeliveryPkgRequest request,DeliveryToken deliveryToken, DeliveryMan sendMan, OrderCustomer customer){
        Kuaidi100OrderParam Kuaidi100OrderParam = new Kuaidi100OrderParam();
        Kuaidi100OrderParam.setPrintType("CLOUD");
        Kuaidi100OrderParam.setPartnerId(deliveryToken.getPartnerId());
        Kuaidi100OrderParam.setTempId(deliveryToken.getTempId());
        Kuaidi100OrderParam.setKuaidicom(deliveryToken.getKuaidicom());
        Kuaidi100OrderParam.setPartnerKey(deliveryToken.getPartnerKey());
        Kuaidi100OrderParam.setPartnerSecret(deliveryToken.getPartnerSecret());
        Kuaidi100OrderParam.setNet(deliveryToken.getNet());
        Kuaidi100OrderParam.setCode(deliveryToken.getCode());
        if (deliveryToken.getSiid() != null) {
            Kuaidi100OrderParam.setSiid(deliveryToken.getSiid());
        }else{
            //预打包默认
            Kuaidi100OrderParam.setSiid("KX100L3AD65411C274");
        }
        Kuaidi100OrderParam.setCount(1);//包裹数量暂时默认为1

        Kuaidi100OrderParam.setCargo("KT版");//快递类型
        Kuaidi100OrderParam.setNeedLogo(true);
        String expType = deliveryToken.getExpType();
        Kuaidi100OrderParam.setExpType(expType);
        String payType = deliveryToken.getPayType();
        Kuaidi100OrderParam.setPayType(payType);
        //特殊处理名字，去掉备注
        DeliveryManInfo recMan = DeliveryManInfo.fromDeliveryMan(sendMan);
        DeliveryManInfo recManInfo  = DeliveryManInfo.fromOrderCustomer(customer);
        Kuaidi100OrderParam.setRecMan(recManInfo);
        Kuaidi100OrderParam.setRemark(request.getRemark());
        return Kuaidi100OrderParam;
    }

}
