package com.mes.application.dto.resp.delivery;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.delivery.deliveryPkg.enums.DeliveryPkgStatus;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryPkgListItemResponse extends BaseEntity {

    private String deliveryPkgId;
    private String deliveryPkgCode;
    private List<DeliveryPkgItemDetail> deliveryPkgItems;
    private DeliveryPkgStatus deliveryPkgStatus;

    private String recipientName;
    private String recipientPhone;
    private String recipientAddress;
    private String province;
    private String city;
    private String district;

    private String senderName;
    private String senderPhone;
    private String senderAddress;

    private Double weight;
    private Double volume;

    private String deliveryWay;
    private String trackingNumber;

    private Date packingStartTime;
    private Date packingEndTime;
    private Date deliveryTime;

    private String remarks;

    private String orderId;
    private String carrierId;
    private String carrierName;
    private String deliveryManId;
    private String deliverySiidId;
    private String manufacturerMetaId;
    private String routeId;
    private String routeNodeId;
    private String routeDesc;

    @Data
    public static class DeliveryPkgItemDetail {
        private String orderItemId;
        private String orderId;
        private List<String> productionPieceId;
        private Integer quantity;
        private String previewUrl;
        private MaterialConfig materialConfig;
        private String processingFlow;
        private Double width;
        private Double height;
    }
}
