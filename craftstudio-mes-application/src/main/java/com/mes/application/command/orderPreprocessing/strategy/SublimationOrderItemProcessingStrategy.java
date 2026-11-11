package com.mes.application.command.orderPreprocessing.strategy;

import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.command.orderPreprocessing.sublimation.SublimationDecorationService;
import com.mes.application.command.orderPreprocessing.sublimation.SublimationDoubleSideJoin;
import com.mes.application.command.orderPreprocessing.sublimation.SublimationPlan;
import com.mes.application.command.orderPreprocessing.sublimation.SublimationTraceService;
import com.mes.application.command.orderPreprocessing.sublimation.SublimationProcessStrategy;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/** 矩形热升华组合工艺：在统一出血回调完成后生成零件及工艺标记。 */
@Service
public class SublimationOrderItemProcessingStrategy implements OrderItemProcessingStrategy {
    private static final Logger log = LoggerFactory.getLogger(SublimationOrderItemProcessingStrategy.class);
    private final SublimationDecorationService decorations;
    private final OrderInfoService orders;
    private final SublimationTraceService traces;
    private final SublimationDoubleSideJoin join = SublimationDoubleSideJoin.separateImages();

    public SublimationOrderItemProcessingStrategy(SublimationDecorationService decorations, OrderInfoService orders, SublimationTraceService traces) {
        this.decorations = decorations;
        this.orders = orders;
        this.traces = traces;
    }

    @Override public boolean matches(OrderItem item, ProcedureFlow flow) {
        return SublimationProcessStrategy.applies(flow);
    }

    @Override public List<ProductionPiece> process(OrderItem item, ProcedureFlow flow, AppOrderPreprocessingService service) {
        SublimationPlan plan = SublimationProcessStrategy.resolve(item);
        if (plan == null || !service.isSublimationReadyForStrategy(item, plan)) {
            throw new IllegalStateException("热升华矩形出血尚未完成，不能提前生成生产零件");
        }
        var size = service.resolveProductionImageSizeForStrategy(item);
        String mask = service.generateRectMaskSvgForStrategy(item);
        ProductionPiece piece = service.getProcedureService().createProductionPiece(item, "ORIGINAL",
                service.resolveProductionImageUrlForStrategy(item), flow, mask, size.widthMm(), size.heightMm());
        OrderInfo order = orders.findByOrderId(item.getOrderId());
        if (order != null && order.getRemark() != null && !order.getRemark().isBlank()) piece.setRemark(order.getRemark());
        String backUrl = plan.doubleSided() ? plan.differentBack() ? item.getSublimationBackDownloadUrl() : item.getMirrorFlagUrl() : null;
        if (plan.doubleSided() && (backUrl == null || backUrl.isBlank())) {
            throw new IllegalStateException("热升华双面工件缺少已处理反面图片");
        }
        var trace = plan.sleeveEdge() == null ? null : traces.resolve(item);
        String backMask = decorations.decorate(item, flow, piece, plan, backUrl, trace == null ? null : trace.code());
        if (trace != null) {
            log.info("热升华订单溯源mark已生成: orderId={}, orderItemId={}, traceCode={}, itemCount={}",
                    item.getOrderId(), item.getOrderItemId(), trace.code(), trace.itemCount());
        } else {
            log.info("热升华订单溯源mark跳过，无缝管套/缝布带管套: orderItemId={}", item.getOrderItemId());
        }
        if (plan.doubleSided()) {
            var result = join.prepare(new SublimationDoubleSideJoin.Input(item.getProductionImgFile().getRawFile(),
                    backUrl, plan.sleeveEdge(), plan.sleeveEdge(), plan.sleeveWidthMm(), plan.doubleSideMode()));
            // 当前只保留双面资源；拼合以及双面材料的反面真实缝筒边验证由后续实现。
            MirrorConfig mirror = new MirrorConfig();
            mirror.setImg(result.backUrl());
            mirror.setSvg(backMask);
            piece.setMirrorConfigs(List.of(mirror));
            log.info("热升华双面拼合入口: orderItemId={}, mode={}, joined={}", item.getOrderItemId(), plan.doubleSideMode(), result.joined());
        }
        log.info("热升华组合工艺处理完成: orderItemId={}, processes={}, widthMm={}, heightMm={}",
                item.getOrderItemId(), plan.processNames(), size.widthMm(), size.heightMm());
        return List.of(piece);
    }

    @Override public String getStrategyType() { return "SUBLIMATION"; }
    @Override public String getStrategyRemark() { return "热升华矩形旗组合缝套、锁边、打扣及绑带工艺"; }
}
