package com.mes.application.command.typesetting.proces.material;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 特殊材料策略调度服务。
 *
 * <p>所有特殊材料策略统一在这里匹配和执行；命中第一个策略后立即返回，
 * 避免同一个生产工件被重复外扩。</p>
 */
@Service
public class MaterialProcessService {
    private final List<AbstractMaterialProcessStrategy> strategies;

    public MaterialProcessService(List<AbstractMaterialProcessStrategy> strategies) {
        this.strategies = strategies;
    }

    public void process(MaterialProcessContext context) {
        if (strategies == null || strategies.isEmpty()) {
            return;
        }
        for (AbstractMaterialProcessStrategy strategy : strategies) {
            if (strategy.matches(context)) {
                strategy.process(context);
                return;
            }
        }
    }
}
