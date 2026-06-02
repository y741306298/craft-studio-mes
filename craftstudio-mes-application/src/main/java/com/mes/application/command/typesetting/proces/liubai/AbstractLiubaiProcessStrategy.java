package com.mes.application.command.typesetting.proces.liubai;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import io.micrometer.common.util.StringUtils;

/**
 * 留白工艺抽象策略。
 *
 * <p>职责边界：</p>
 * <ul>
 *     <li>统一处理留白工艺与“异形切割”互斥的前置判断。</li>
 *     <li>统一限定只有工艺节点名称包含“留白”时才进入实体策略判断。</li>
 *     <li>提供节点名称和参数文本的模糊匹配工具，供不同留白规格实体策略复用。</li>
 * </ul>
 *
 * <p>扩展方式：</p>
 * <ol>
 *     <li>新增实体策略继承本类。</li>
 *     <li>在 {@link #matchesLiubaiValue(ProcedureFlow)} 中判断具体留白规格。</li>
 *     <li>在 {@link #process(LiubaiProcessContext)} 中实现该规格的 mask SVG 改写和工件字段回写。</li>
 * </ol>
 */
public abstract class AbstractLiubaiProcessStrategy {

    /**
     * 判断当前上下文是否命中该留白实体策略。
     *
     * <p>公共规则：</p>
     * <ul>
     *     <li>上下文或工艺流程为空：不命中。</li>
     *     <li>存在“异形切割”：不命中，因为留白与异形切割互斥，留白只处理矩形 mask。</li>
     *     <li>不存在名称包含“留白”的节点：不命中。</li>
     *     <li>通过以上公共规则后，再交给实体策略判断具体留白规格。</li>
     * </ul>
     *
     * @param context 留白处理上下文
     * @return {@code true} 表示当前实体策略可以处理该上下文
     */
    public boolean matches(LiubaiProcessContext context) {
        if (context == null || context.getProcedureFlow() == null) {
            return false;
        }
        ProcedureFlow procedureFlow = context.getProcedureFlow();
        if (hasNode(procedureFlow, "异形切割") || !hasLiubaiNode(procedureFlow)) {
            return false;
        }
        return matchesLiubaiValue(procedureFlow);
    }

    /**
     * 实体策略留白规格匹配入口。
     *
     * <p>例如“留白3cm”策略会在这里匹配节点名或参数文本中的 3cm / 30mm 等写法。</p>
     *
     * @param procedureFlow 已解析工艺流程
     * @return {@code true} 表示当前工艺流程命中该实体留白规格
     */
    protected abstract boolean matchesLiubaiValue(ProcedureFlow procedureFlow);

    /**
     * 执行实体留白策略。
     *
     * <p>实现类通常需要读取 {@link LiubaiProcessContext#getProductionPiece()} 的原始 mask SVG，
     * 生成新的外扩 SVG，上传后回写 productionPiece.maskImageFile，并同步修正工件宽高。</p>
     *
     * @param context 留白处理上下文
     */
    public abstract void process(LiubaiProcessContext context);

    /**
     * 判断工艺流程中是否存在留白节点。
     *
     * @param procedureFlow 已解析工艺流程
     * @return {@code true} 表示至少存在一个节点名包含“留白”的节点
     */
    protected boolean hasLiubaiNode(ProcedureFlow procedureFlow) {
        return procedureFlow.getNodes() != null && procedureFlow.getNodes().stream()
                .anyMatch(node -> node != null && StringUtils.isNotBlank(node.getNodeName()) && node.getNodeName().contains("留白"));
    }

    /**
     * 判断工艺流程中是否存在指定名称的节点。
     *
     * @param procedureFlow 已解析工艺流程
     * @param nodeName 精确匹配的节点名称
     * @return {@code true} 表示存在该节点
     */
    protected boolean hasNode(ProcedureFlow procedureFlow, String nodeName) {
        return procedureFlow.getNodes() != null && procedureFlow.getNodes().stream()
                .anyMatch(node -> node != null && nodeName.equals(node.getNodeName()));
    }

    /**
     * 在留白节点名称或其参数配置文本中查找关键字。
     *
     * <p>这里会忽略大小写与空白字符，原因是工艺参数可能来自产品中心 DTO、Map 或反序列化对象，
     * 当前预处理阶段只需要识别“留白3cm”等规格文本，因此使用字符串兼容匹配。</p>
     *
     * @param procedureFlow 已解析工艺流程
     * @param keyword 需要查找的关键字
     * @return {@code true} 表示节点名称或参数文本中包含该关键字
     */
    protected boolean containsInNodeOrParams(ProcedureFlow procedureFlow, String keyword) {
        if (procedureFlow.getNodes() == null || StringUtils.isBlank(keyword)) {
            return false;
        }
        String normalizedKeyword = normalize(keyword);
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node == null || StringUtils.isBlank(node.getNodeName()) || !node.getNodeName().contains("留白")) {
                continue;
            }
            if (normalize(node.getNodeName()).contains(normalizedKeyword)) {
                return true;
            }
            if (node.getParamConfigs() != null && node.getParamConfigs().stream()
                    .anyMatch(config -> config != null && normalize(String.valueOf(config)).contains(normalizedKeyword))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将匹配文本标准化为小写并移除所有空白字符。
     *
     * @param value 原始匹配文本
     * @return 标准化后的文本，空值返回空字符串
     */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("\\s+", "");
    }
}
