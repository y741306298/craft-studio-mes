package com.mes.application.command.delivery.req;

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

    private String sendManid;

    private List<orderItemVo> orderItems;

    private String kuaidiWay;

    private String remark;

    private String cargo;

    private Integer count;

    private Kuaidi100OrderParam param;

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
    public class FdParam {

        private String taskId;

        private String siid;

    }

    @Data
    public class PrintParam {

        private String tempid;

        private String printType;

        private String siid;

        private String direction;

        private String callBackUrl;

        private Map<String, Object> customParam;
    }



    }


