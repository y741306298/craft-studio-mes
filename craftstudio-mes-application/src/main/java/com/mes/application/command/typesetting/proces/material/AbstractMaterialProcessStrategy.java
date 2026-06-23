package com.mes.application.command.typesetting.proces.material;

import com.mes.application.command.typesetting.proces.liubai.AbstractCentimeterLiubaiProcessStrategy;
import com.mes.application.command.typesetting.proces.liubai.LiubaiProcessContext;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import io.micrometer.common.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 特殊材料工艺策略基类。
 *
 * <p>特殊材料没有显式“留白”工艺节点时，也需要按固定规格执行与留白相同的
 * mask SVG 重构/续写、四边标签生成和生产工件宽高回写流程。</p>
 */
public abstract class AbstractMaterialProcessStrategy extends AbstractCentimeterLiubaiProcessStrategy {
    protected AbstractMaterialProcessStrategy(int expandCm, RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(expandCm, restTemplate, ossTagUploadService);
    }

    @Override
    public boolean matches(LiubaiProcessContext context) {
        if (context == null || context.getProcedureFlow() == null || context.getOrderItem() == null) {
            return false;
        }
        ProcedureFlow procedureFlow = context.getProcedureFlow();
        return !hasNode(procedureFlow, "异形切割")
                && !hasLiubaiNode(procedureFlow)
                && matchesMaterialName(resolveMaterialName(context.getOrderItem()));
    }

    @Override
    protected boolean matchesLiubaiValue(ProcedureFlow procedureFlow) {
        return true;
    }

    protected abstract boolean matchesMaterialName(String materialName);

    private String resolveMaterialName(OrderItem orderItem) {
        if (orderItem == null || orderItem.getMaterial() == null || orderItem.getMaterial().getMaterialSnapshot() == null) {
            return "";
        }
        String materialName = orderItem.getMaterial().getMaterialSnapshot().getName();
        return StringUtils.isBlank(materialName) ? "" : materialName;
    }
}
