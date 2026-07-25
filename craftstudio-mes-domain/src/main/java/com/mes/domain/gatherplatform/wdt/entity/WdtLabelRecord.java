package com.mes.domain.gatherplatform.wdt.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.delivery.deliveryPkg.enums.PreOrderLabelConsumeStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 旺店通快递面单打印记录。
 *
 * <p>记录打印时使用的快递配置、旺店通返回的完整面单数据以及 MES 生成的打印备注。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WdtLabelRecord extends BaseEntity {
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

    /** 由订单号、生产文件名和订单备注组成的打印备注。 */
    private String remark;
}
