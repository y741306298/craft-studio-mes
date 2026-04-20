package com.mes.infra.dal.delivery.deliveryPkg.po;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryRecord;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "deliveryRecord")
public class DeliveryRecordPO extends BasePO<DeliveryRecord> {

    private String kuaidiNum;

    private String deliveryRecordId;

    private String orderId;

    private String trackingNumber;

    private String carrierId;

    private String carrierName;

    private String deliveryManId;

    private String deliverySiidId;

    private String userId;

    private String manufacturerMetaId;

    private String remark;

    private Boolean isSuccess;

    private String errorMsg;

    private Date deliveryTime;

    private List<DeliveryRecord.ProductionPieceDTO> pieces;

    @Override
    public DeliveryRecord toDO() {
        DeliveryRecord deliveryRecord = new DeliveryRecord();
        copyBaseFieldsToDO(deliveryRecord);

        deliveryRecord.setDeliveryRecordId(this.deliveryRecordId);
        deliveryRecord.setKuaidiNum(this.kuaidiNum);
        deliveryRecord.setOrderId(this.orderId);
        deliveryRecord.setTrackingNumber(this.trackingNumber);
        deliveryRecord.setCarrierId(this.carrierId);
        deliveryRecord.setCarrierName(this.carrierName);
        deliveryRecord.setDeliveryManId(this.deliveryManId);
        deliveryRecord.setDeliverySiidId(this.deliverySiidId);
        deliveryRecord.setUserId(this.userId);
        deliveryRecord.setManufacturerMetaId(this.manufacturerMetaId);
        deliveryRecord.setRemark(this.remark);
        deliveryRecord.setDeliveryTime(this.deliveryTime);
        deliveryRecord.setIsSuccess(this.isSuccess);
        deliveryRecord.setErrorMsg(this.errorMsg);
        deliveryRecord.setPieces(this.pieces);

        return deliveryRecord;
    }

    @Override
    protected BasePO<DeliveryRecord> fromDO(DeliveryRecord _do) {
        if (_do == null) {
            return null;
        }

        this.deliveryRecordId = _do.getDeliveryRecordId();
        this.kuaidiNum = _do.getKuaidiNum();
        this.orderId = _do.getOrderId();
        this.trackingNumber = _do.getTrackingNumber();
        this.carrierId = _do.getCarrierId();
        this.carrierName = _do.getCarrierName();
        this.deliveryManId = _do.getDeliveryManId();
        this.deliverySiidId = _do.getDeliverySiidId();
        this.userId = _do.getUserId();
        this.manufacturerMetaId = _do.getManufacturerMetaId();
        this.remark = _do.getRemark();
        this.deliveryTime = _do.getDeliveryTime();
        this.isSuccess = _do.getIsSuccess();
        this.errorMsg = _do.getErrorMsg();
        this.pieces = _do.getPieces();

        return this;
    }
}
