package com.mes.domain.order.orderInfo.vo;

public class OrderCustomer {

    private String customerId;
    private String customerName;
    private String customerPhone;
    
    /**
     * 省级行政区（如：广东省）
     */
    private String province;
    
    /**
     * 市级行政区（如：深圳市）
     */
    private String city;
    
    /**
     * 区级行政区（如：南山区）
     */
    private String district;
    
    /**
     * 详细街道地址（如：粤海街道 xxx 号）
     */
    private String detailAddress;

}
