package com.mes.application.command.typesetting.proces.buckle;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

/**
 * 打扣预处理上下文。
 *
 * <p>与留白预处理上下文保持一致的职责边界：调用方只负责组装订单项、工艺流程和生产工件，
 * 具体打扣节点匹配、mask SVG 改写、上传与工件字段回写由实体策略完成。</p>
 */
@Data
public class BuckleProcessContext {
    /** 订单明细，用于解析上传路径中的厂家与订单明细标识。 */
    private OrderItem orderItem;

    /** 当前订单明细解析出的工艺流程，用于命中具体打扣策略。 */
    private ProcedureFlow procedureFlow;

    /** 需要在持久化前写入扣点的生产工件。 */
    private ProductionPiece productionPiece;
}
