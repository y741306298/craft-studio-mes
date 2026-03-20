package com.mes.application.command.typesetting.vo;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

/**
 * 排版与生产工件统一返回对象
 */
@Data
public class TypesettingProductionPieceVO {

    /**
     * 订单项信息
     */
    private OrderItem orderItemInfo;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 已完成数量
     */
    private Integer completedQuantity;

    /**
     * 材质
     */
    private String material;

    /**
     * 工艺流程
     */
    private ProcedureFlow procedureFlow;

    /**
     * 预览 URL
     */
    private String previewUrl;

    /**
     * 备注
     */
    private String remark;

    /**
     * 来源类型（TYPESetting-排版，PRODUCTION_PIECE-生产工件）
     */
    private String sourceType;

    /**
     * 来源 ID
     */
    private String sourceId;
}
