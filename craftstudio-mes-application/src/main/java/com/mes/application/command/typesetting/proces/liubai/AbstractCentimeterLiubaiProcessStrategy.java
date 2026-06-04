package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import io.micrometer.common.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 厘米规格留白实体策略基类。
 *
 * <p>留白 2cm / 5cm / 10cm / 15cm 的 SVG 外扩、mark 生成和工件回写流程完全一致，
 * 唯一差异是外扩距离。该基类统一根据厘米数生成规格名称、毫米外扩值和精确匹配规则，
 * 避免“留白5cm”误命中“留白15cm”这类包含关系。</p>
 */
public abstract class AbstractCentimeterLiubaiProcessStrategy extends AbstractFixedLiubaiProcessStrategy {
    private final int expandCm;
    private final double expandMm;
    private final Pattern centimeterPattern;
    private final Pattern millimeterPattern;

    protected AbstractCentimeterLiubaiProcessStrategy(int expandCm, RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(restTemplate, ossTagUploadService);
        this.expandCm = expandCm;
        this.expandMm = expandCm * 10D;
        String cm = decimalText(BigDecimal.valueOf(expandCm));
        String mm = decimalText(BigDecimal.valueOf(expandCm).multiply(BigDecimal.TEN));
        this.centimeterPattern = Pattern.compile("(?<![0-9.])" + Pattern.quote(cm) + "(?:\\.0+)?(?:cm|厘米)(?![a-z0-9])");
        this.millimeterPattern = Pattern.compile("(?<![0-9.])" + Pattern.quote(mm) + "(?:\\.0+)?(?:mm|毫米)(?![a-z0-9])");
    }

    @Override
    protected boolean matchesLiubaiValue(ProcedureFlow procedureFlow) {
        if (procedureFlow.getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node == null || StringUtils.isBlank(node.getNodeName()) || !node.getNodeName().contains("留白")) {
                continue;
            }
            for (String value : candidateTexts(node)) {
                String normalized = normalize(value);
                if (centimeterPattern.matcher(normalized).find() || millimeterPattern.matcher(normalized).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected String specName() {
        return expandCm + "cm";
    }

    @Override
    protected double expandMm() {
        return expandMm;
    }

    @Override
    protected String[] matchKeywords() {
        return new String[0];
    }

    private List<String> candidateTexts(ProcedureFlowNode node) {
        List<String> candidates = new ArrayList<>();
        candidates.add(node.getNodeName());
        if (node.getParamConfigs() != null) {
            node.getParamConfigs().stream()
                    .filter(config -> config != null)
                    .map(String::valueOf)
                    .forEach(candidates::add);
        }
        return candidates;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("\\s+", "");
    }

    private static String decimalText(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
