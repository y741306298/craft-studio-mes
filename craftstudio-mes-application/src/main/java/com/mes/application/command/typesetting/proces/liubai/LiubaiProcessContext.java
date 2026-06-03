package com.mes.application.command.typesetting.proces.liubai;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

/**
 * 留白工艺预处理上下文。
 *
 * <p>用途说明：</p>
 * <ul>
 *     <li>承载留白策略执行所需的订单项、已解析工艺流程与当前生产工件。</li>
 *     <li>直接生成路线：{@link #skipBloodEdges} 为 {@code false}，表示四边均按留白尺寸外扩。</li>
 *     <li>超幅拼接回调路线：{@link #skipBloodEdges} 为 {@code true}，表示策略需要根据生产工件的 blood 信息跳过出血边，仅外扩非出血边。</li>
 * </ul>
 *
 * <p>注意：该上下文只在订单预处理阶段短生命周期使用，不作为持久化对象。</p>
 */
@Data
public class LiubaiProcessContext {
    /**
     * 当前正在预处理的订单项，用于读取订单项 ID、厂商 ID 等上传路径与业务标识信息。
     */
    private OrderItem orderItem;

    /**
     * 已由预处理服务解析后的工艺流程，用于留白抽象策略判断是否命中留白工艺及具体留白规格。
     */
    private ProcedureFlow procedureFlow;

    /**
     * 当前已经创建但可能尚未持久化的生产工件，留白策略会回写其 maskImageFile、markImageFile 与宽高。
     */
    private ProductionPiece productionPiece;

    /**
     * 是否跳过出血边外扩。
     *
     * <p>直接路线没有切片出血边，固定为 {@code false}；超幅拼接 callback 路线已经带有 blood 信息，
     * 固定为 {@code true}，由实体策略根据 blood.x / blood.y 判断哪条边不外扩。</p>
     */
    private boolean skipBloodEdges;
}
