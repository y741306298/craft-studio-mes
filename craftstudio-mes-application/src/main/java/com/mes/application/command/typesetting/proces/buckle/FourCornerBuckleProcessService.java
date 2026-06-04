package com.mes.application.command.typesetting.proces.buckle;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单预处理阶段的打扣工艺策略调度服务。
 *
 * <p>保留历史类名，避免订单预处理主流程改动注入点；内部结构调整为与留白预处理一致的策略调度模式。</p>
 * <ul>
 *     <li>所有继承 {@link AbstractBuckleProcessStrategy} 的 Spring Bean 都会注入到 {@link #strategies}。</li>
 *     <li>当前支持四角打扣、四周打扣、上下打扣、左右打扣、下边打扣、左边打扣、右边打扣。</li>
 *     <li>只执行第一个命中的实体策略，避免同一个工件因多个兼容节点重复写入同类扣点。</li>
 * </ul>
 */
@Service
public class FourCornerBuckleProcessService {
    /** Spring 自动收集到的打扣实体策略列表。 */
    private final List<AbstractBuckleProcessStrategy> strategies;

    public FourCornerBuckleProcessService(List<AbstractBuckleProcessStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 按顺序查找并执行命中的打扣策略。
     *
     * @param orderItem 订单明细
     * @param procedureFlow 已解析工艺流程
     * @param piece 需要写入扣点的生产工件
     */
    public void process(OrderItem orderItem, ProcedureFlow procedureFlow, ProductionPiece piece) {
        if (strategies == null || strategies.isEmpty()) {
            return;
        }
        BuckleProcessContext context = new BuckleProcessContext();
        context.setOrderItem(orderItem);
        context.setProcedureFlow(procedureFlow);
        context.setProductionPiece(piece);
        for (AbstractBuckleProcessStrategy strategy : strategies) {
            if (strategy.matches(context)) {
                strategy.process(context);
                return;
            }
        }
    }
}
