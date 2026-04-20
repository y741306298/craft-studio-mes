package com.mes.infra.dal.delivery.deliveryPkg.po;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.delivery.deliveryPkg.enums.DeliveryPkgStatus;
import com.mes.domain.delivery.deliveryPkg.vo.DeliveryPkgItem;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * 包裹持久化对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "deliveryPkg")
public class DeliveryPkgPo extends BasePO<DeliveryPkg> {

    private String deliveryPkgId;
    private String deliveryPkgCode;
    private List<DeliveryPkgItem> deliveryPkgItems;
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

    @Override
    public DeliveryPkg toDO() {
        DeliveryPkg deliveryPkg = new DeliveryPkg();
        copyBaseFieldsToDO(deliveryPkg);

        deliveryPkg.setDeliveryPkgId(this.deliveryPkgId);
        deliveryPkg.setDeliveryPkgCode(this.deliveryPkgCode);
        deliveryPkg.setDeliveryPkgItems(this.deliveryPkgItems);
        deliveryPkg.setDeliveryPkgStatus(this.deliveryPkgStatus);

        deliveryPkg.setRecipientName(this.recipientName);
        deliveryPkg.setRecipientPhone(this.recipientPhone);
        deliveryPkg.setRecipientAddress(this.recipientAddress);
        deliveryPkg.setProvince(this.province);
        deliveryPkg.setCity(this.city);
        deliveryPkg.setDistrict(this.district);

        deliveryPkg.setSenderName(this.senderName);
        deliveryPkg.setSenderPhone(this.senderPhone);
        deliveryPkg.setSenderAddress(this.senderAddress);

        deliveryPkg.setWeight(this.weight);
        deliveryPkg.setVolume(this.volume);

        deliveryPkg.setDeliveryWay(this.deliveryWay);
        deliveryPkg.setTrackingNumber(this.trackingNumber);

        deliveryPkg.setPackingStartTime(this.packingStartTime);
        deliveryPkg.setPackingEndTime(this.packingEndTime);
        deliveryPkg.setDeliveryTime(this.deliveryTime);

        deliveryPkg.setRemarks(this.remarks);

        return deliveryPkg;
    }

    @Override
    protected BasePO<DeliveryPkg> fromDO(DeliveryPkg _do) {
        if (_do == null) {
            return null;
        }

        this.deliveryPkgId = _do.getDeliveryPkgId();
        this.deliveryPkgCode = _do.getDeliveryPkgCode();
        this.deliveryPkgItems = _do.getDeliveryPkgItems();
        this.deliveryPkgStatus = _do.getDeliveryPkgStatus();

        this.recipientName = _do.getRecipientName();
        this.recipientPhone = _do.getRecipientPhone();
        this.recipientAddress = _do.getRecipientAddress();
        this.province = _do.getProvince();
        this.city = _do.getCity();
        this.district = _do.getDistrict();

        this.senderName = _do.getSenderName();
        this.senderPhone = _do.getSenderPhone();
        this.senderAddress = _do.getSenderAddress();

        this.weight = _do.getWeight();
        this.volume = _do.getVolume();

        this.deliveryWay = _do.getDeliveryWay();
        this.trackingNumber = _do.getTrackingNumber();

        this.packingStartTime = _do.getPackingStartTime();
        this.packingEndTime = _do.getPackingEndTime();
        this.deliveryTime = _do.getDeliveryTime();

        this.remarks = _do.getRemarks();

        return this;
    }
}
