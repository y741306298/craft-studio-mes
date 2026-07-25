package com.mes.infra.dal.gatherplatform.wdt.po;

import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;
import com.mes.domain.delivery.deliveryPkg.enums.PreOrderLabelConsumeStatus;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 旺店通快递面单记录 MongoDB 持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "wdtLabelRecord")
public class WdtLabelRecordPO extends BasePO<WdtLabelRecord> {
    /** 工厂元数据 ID。 */
    private String manufacturerMetaId;
    private String orderId;
    private String channelOrderId;
    private PreOrderLabelConsumeStatus consumeStatus;
    private String deliveryPkgId;
    /** 旺店通仓库 ID。 */
    private String warehouseId;
    /** 旺店通物流 ID。 */
    private String logisticsId;
    /** 物流名称。 */
    private String logisticsName;
    /** MES 订单物流预设类型。 */
    private String presetType;
    /** 旺店通返回的物流单号。 */
    private String logisticsOrderId;
    /** 旺店通面单中的收件人数据。 */
    private Object consignee;
    /** 旺店通面单中的云打印数据。 */
    private Object logisticsCloudPrintData;
    /** MES 生成的打印备注。 */
    private String remark;

    /**
     * 将持久化对象转换为领域实体。
     */
    @Override
    public WdtLabelRecord toDO() {
        WdtLabelRecord value = new WdtLabelRecord();
        copyBaseFieldsToDO(value);
        value.setManufacturerMetaId(manufacturerMetaId);
        value.setOrderId(orderId);
        value.setChannelOrderId(channelOrderId);
        value.setConsumeStatus(consumeStatus);
        value.setDeliveryPkgId(deliveryPkgId);
        value.setWarehouseId(warehouseId);
        value.setLogisticsId(logisticsId);
        value.setLogisticsName(logisticsName);
        value.setPresetType(presetType);
        value.setLogisticsOrderId(logisticsOrderId);
        value.setConsignee(consignee);
        value.setLogisticsCloudPrintData(logisticsCloudPrintData);
        value.setRemark(remark);
        return value;
    }

    /**
     * 使用领域实体填充持久化对象。
     */
    @Override
    protected BasePO<WdtLabelRecord> fromDO(WdtLabelRecord value) {
        manufacturerMetaId = value.getManufacturerMetaId();
        orderId = value.getOrderId();
        channelOrderId = value.getChannelOrderId();
        consumeStatus = value.getConsumeStatus();
        deliveryPkgId = value.getDeliveryPkgId();
        warehouseId = value.getWarehouseId();
        logisticsId = value.getLogisticsId();
        logisticsName = value.getLogisticsName();
        presetType = value.getPresetType();
        logisticsOrderId = value.getLogisticsOrderId();
        consignee = value.getConsignee();
        logisticsCloudPrintData = value.getLogisticsCloudPrintData();
        remark = value.getRemark();
        return this;
    }
}
