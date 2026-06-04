package com.mes.application.command.typesetting.proces.liubai;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 留白工艺策略调度服务。
 *
 * <p>设计说明：</p>
 * <ul>
 *     <li>该服务是订单预处理模块调用留白策略的唯一入口，避免预处理主流程直接依赖具体留白规格。</li>
 *     <li>所有继承 {@link AbstractLiubaiProcessStrategy} 的 Spring Bean 都会注入到 {@link #strategies}。</li>
 *     <li>当前已支持“留白2cm”“留白5cm”“留白10cm”“留白15cm”等实体策略，后续新增更多规格时无需改动调用方。</li>
 *     <li>只执行第一个命中的实体策略，避免同一个订单项因为参数文本兼容匹配而重复外扩。</li>
 * </ul>
 */
@Service
public class LiubaiProcessService {
    /**
     * Spring 自动收集到的留白实体策略列表。
     */
    private final List<AbstractLiubaiProcessStrategy> strategies;

    /**
     * 构造留白策略调度服务。
     *
     * @param strategies 当前应用上下文中注册的留白实体策略列表
     */
    public LiubaiProcessService(List<AbstractLiubaiProcessStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 按顺序查找并执行命中的留白策略。
     *
     * <p>处理流程：</p>
     * <ol>
     *     <li>策略列表为空时直接返回，保证调用方无需做空判断。</li>
     *     <li>逐个调用策略的 {@link AbstractLiubaiProcessStrategy#matches(LiubaiProcessContext)}。</li>
     *     <li>遇到首个命中策略后执行 {@link AbstractLiubaiProcessStrategy#process(LiubaiProcessContext)} 并立即结束。</li>
     * </ol>
     *
     * @param context 留白处理上下文，包含订单项、工艺流程、生产工件与出血边处理标记
     */
    public void process(LiubaiProcessContext context) {
        if (strategies == null || strategies.isEmpty()) {
            return;
        }
        for (AbstractLiubaiProcessStrategy strategy : strategies) {
            if (strategy.matches(context)) {
                strategy.process(context);
                return;
            }
        }
    }
}
