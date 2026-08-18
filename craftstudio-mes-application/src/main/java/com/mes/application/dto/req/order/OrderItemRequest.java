package com.mes.application.dto.req.order;

import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderItemPriceInfo;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import lombok.Data;

import java.util.Date;

@Data
public class OrderItemRequest {
    private Long id;
    private String key;
    private Long orderId;
    private Date createTime;
    private Date updateTime;
    private String state;
    private MTOProductSpecDTO productSpec;
    private MTOProductSpecDTO mtoProductSpec;
    private Integer count;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private SpecifyRmfInfo specifyRmfInfo;
    private OrderItemPriceInfo price;
    private OrderItemPriceInfo manufacturerPrice;


    @Data
    public static class SpecifyRmfInfo {
        private String rmfId;
        private String rmfName;
    }
}
