package com.mes.application.dto.req.order;


import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import com.mes.domain.order.orderInfo.vo.OrderPriceInfo;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 订单新增请求 DTO
 */
@Data
public class OrderAddRequest {
    private List<OrderItemRequest> orderItems;
    private ConsigneeRequest consignee;
    private OrgInfo orgInfo;
    private Long id;
    private String key;
    private Date createTime;
    private Date updateTime;
    private String state;
    private String note;
    private String platformCode;
    private Long orgId;
    private Long userId;
    private String externalOrderId;
    private String paymentState;
    private OrderChannelInfo channel;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private ManufacturerInfoRequest manufacturerInfo;
    private OrderPriceInfo price;


    public OrderInfo toOrderInfo() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(key);
        orderInfo.setCreateTime(createTime);
        orderInfo.setUpdateTime(updateTime);
        orderInfo.setOrderId(String.valueOf(id));
        orderInfo.setCustomer(consignee.toOrderCustomer());
        orderInfo.setOrgInfo(orgInfo);
        orderInfo.setDeliveryAddress(consignee.getDetailAddress());
        orderInfo.setRemark(note);
        orderInfo.setPlatformCode(platformCode);
        orderInfo.setPrice(price);
        orderInfo.setOrgId(orgId);
        orderInfo.setUserId(userId);
        orderInfo.setExternalOrderId(externalOrderId);
        orderInfo.setPaymentState(paymentState);
        orderInfo.setChannel(channel);
        orderInfo.setLogisticsCarrierInfo(logisticsCarrierInfo);
        if (manufacturerInfo != null) {
            orderInfo.setManufacturerId(manufacturerInfo.getId());
            orderInfo.setManufacturerName(manufacturerInfo.getName());
        }
        orderInfo.setStatus(OrderStatus.PENDING);
        orderInfo.setExpectedDeliveryDate(new Date());
        return orderInfo;
    }

    public List<OrderItem> toOrderItems() {
        List<OrderItem> orderItems = new ArrayList<OrderItem>();
        for (OrderItemRequest orderItemRequest : this.orderItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setId(orderItemRequest.getKey());
            orderItem.setCreateTime(orderItemRequest.getCreateTime());
            orderItem.setUpdateTime(orderItemRequest.getUpdateTime());
            orderItem.setOrderItemId(String.valueOf(orderItemRequest.getId()));
            MTOProductSpecDTO productSpec = orderItemRequest.getProductSpec();
            if (productSpec == null) productSpec = orderItemRequest.getMtoProductSpec();
            orderItem.setMtoProduct(productSpec);
            OrderItemRequest.SpecifyRmfInfo specifyRmfInfo = orderItemRequest.getSpecifyRmfInfo();
            if (specifyRmfInfo == null && manufacturerInfo != null) {
                specifyRmfInfo = new OrderItemRequest.SpecifyRmfInfo();
                specifyRmfInfo.setRmfId(manufacturerInfo.getId());
                specifyRmfInfo.setRmfName(manufacturerInfo.getName());
            }
            orderItem.setManufacturerId(specifyRmfInfo == null ? null : specifyRmfInfo.getRmfId());
            orderItem.setQuantity(orderItemRequest.getCount());
            orderItem.setStatus(OrderStatus.PENDING);
            orderItem.setIsUrgent(false);
            LogisticsCarrierInfo itemLogisticsCarrierInfo = orderItemRequest.getLogisticsCarrierInfo() != null
                    ? orderItemRequest.getLogisticsCarrierInfo()
                    : logisticsCarrierInfo;
            orderItem.setLogisticsCarrierInfo(itemLogisticsCarrierInfo);
            orderItem.setKuaidiWay(itemLogisticsCarrierInfo == null ? null : itemLogisticsCarrierInfo.getCarrierId());
            orderItem.setPrice(orderItemRequest.getPrice());
            orderItems.add(orderItem);
        }
        return orderItems;
    }

}


