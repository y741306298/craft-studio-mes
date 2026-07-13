package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * “配易拉宝”工艺策略。
 *
 * <p>易拉宝通常不会配置“异形切割”节点，无法复用异形切割产出的 mask SVG；
 * 因此该策略只精确匹配“配易拉宝”工艺节点，并优先根据订单物料 usageSize3D 的厘米宽高生成初始矩形 SVG，
 * 缺失时再回退到该节点参数中的配件规格；随后复用固定留白基类的 path 化留白、processingFlow 标签、mark PNG 上传和生产工件回写能力。</p>
 */
@Service
public class YilabaoProcessStrategy extends AbstractCentimeterLiubaiProcessStrategy {
    private static final String PROCESS_NAME = "配易拉宝";
    private static final double CM_TO_MM = 10D;
    private static final double HORIZONTAL_INSET_MM = 10D;
    /** 上下各外扩 20mm。 */
    private static final double VERTICAL_EXPAND_PER_SIDE_MM = 20D;
    private static final Pattern ACCESSORY_SIZE_PATTERN = Pattern.compile("^[\\p{IsHan}]+\\s*([0-9]+(?:\\.[0-9]+)?)\\s*[×xX*]\\s*([0-9]+(?:\\.[0-9]+)?)\\s*[\\p{IsHan}]+$");

    public YilabaoProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(0, restTemplate, ossTagUploadService);
    }

    @Override
    public boolean matches(LiubaiProcessContext context) {
        return context != null
                && findYilabaoNode(context.getProcedureFlow()) != null
                && resolveYilabaoSize(context) != null;
    }

    @Override
    protected boolean matchesLiubaiValue(ProcedureFlow procedureFlow) {
        return findYilabaoNode(procedureFlow) != null && findAccessoryYilabaoSize(procedureFlow) != null;
    }

    @Override
    protected String specName() {
        return "yilabao";
    }

    @Override
    protected double expandMm() {
        return 0D;
    }

    @Override
    protected String[] matchKeywords() {
        return new String[]{PROCESS_NAME};
    }

    @Override
    protected ExpandMargins resolveMargins(ProductionPiece piece, LiubaiProcessContext context) {
        return new ExpandMargins(-HORIZONTAL_INSET_MM, -HORIZONTAL_INSET_MM, VERTICAL_EXPAND_PER_SIDE_MM, VERTICAL_EXPAND_PER_SIDE_MM);
    }

    @Override
    protected String resolveOriginalMaskRef(LiubaiProcessContext context, ProductionPiece piece) {
        YilabaoSize size = resolveYilabaoSize(context);
        if (size == null) {
            return null;
        }
        String imageUrl = resolveImageFileRaw(piece == null ? null : piece.getProductImageFile());
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + format(size.widthMm)
                + "\" height=\"" + format(size.heightMm)
                + "\" viewBox=\"0 0 " + format(size.widthMm) + " " + format(size.heightMm)
                + "\" version=\"1.1\">\n"
                + "<g id=\"yilabao-original\" img=\"" + escapeAttr(imageUrl)
                + "\" data-source-name=\"" + escapeAttr(sourceName(imageUrl))
                + "\" data-rotation=\"0\">\n"
                + "<path d=\"M0 0 H" + format(size.widthMm) + " V" + format(size.heightMm)
                + " H0 Z\" fill=\"#ffffff\" />\n"
                + "</g>\n"
                + "</svg>";
    }

    @Override
    protected double resolveOriginalWidth(LiubaiProcessContext context, String originalSvg, ProductionPiece piece) {
        YilabaoSize size = resolveYilabaoSize(context);
        return size == null ? 0D : size.widthMm;
    }

    @Override
    protected double resolveOriginalHeight(LiubaiProcessContext context, String originalSvg, ProductionPiece piece) {
        YilabaoSize size = resolveYilabaoSize(context);
        return size == null ? 0D : size.heightMm;
    }

    private YilabaoSize resolveYilabaoSize(LiubaiProcessContext context) {
        YilabaoSize usageSize = resolveUsageSize(context == null ? null : context.getOrderItem());
        if (usageSize != null) {
            return usageSize;
        }
        return findAccessoryYilabaoSize(context == null ? null : context.getProcedureFlow());
    }

    private YilabaoSize resolveUsageSize(OrderItem orderItem) {
        Object usageSize3D = orderItem == null || orderItem.getMaterial() == null ? null : orderItem.getMaterial().getUsageSize3D();
        if (usageSize3D == null) {
            return null;
        }
        Number widthCm = invokeNumberGetter(usageSize3D, "getWidth", "getW", "getX");
        Number heightCm = invokeNumberGetter(usageSize3D, "getHeight", "getH", "getY");
        if (widthCm == null || heightCm == null || widthCm.doubleValue() <= 0D || heightCm.doubleValue() <= 0D) {
            return null;
        }
        return YilabaoSize.fromCentimeter(widthCm.doubleValue(), heightCm.doubleValue());
    }

    private Number invokeNumberGetter(Object target, String... getterNames) {
        if (target == null || getterNames == null) {
            return null;
        }
        for (String getterName : getterNames) {
            try {
                Object value = target.getClass().getMethod(getterName).invoke(target);
                if (value instanceof Number number) {
                    return number;
                }
            } catch (ReflectiveOperationException ignored) {
                // 兼容不同 usageSize3D 值对象的 getter 命名。
            }
        }
        return null;
    }

    private YilabaoSize findAccessoryYilabaoSize(ProcedureFlow procedureFlow) {
        String accessoryName = findYilabaoAccessoryName(procedureFlow);
        if (StringUtils.isBlank(accessoryName)) {
            return null;
        }
        Matcher matcher = ACCESSORY_SIZE_PATTERN.matcher(accessoryName.trim());
        if (!matcher.matches()) {
            return null;
        }
        return YilabaoSize.fromCentimeter(Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2)));
    }

    private String findYilabaoAccessoryName(ProcedureFlow procedureFlow) {
        ProcedureFlowNode node = findYilabaoNode(procedureFlow);
        if (node == null || node.getParamConfigs() == null) {
            return null;
        }
        for (MTOProductSpecDTO.ProcessParamConfigDTO config : node.getParamConfigs()) {
            Object param = extractFieldValue(config, "param");
            Object accessorySnapshot = extractFieldValue(param, "accessorySnapshot");
            Object accessoryName = extractFieldValue(accessorySnapshot, "name");
            if (accessoryName != null && StringUtils.isNotBlank(String.valueOf(accessoryName))) {
                return String.valueOf(accessoryName).trim();
            }
        }
        return null;
    }

    private ProcedureFlowNode findYilabaoNode(ProcedureFlow procedureFlow) {
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            return null;
        }
        return procedureFlow.getNodes().stream()
                .filter(node -> node != null && StringUtils.isNotBlank(node.getNodeName()) && PROCESS_NAME.equals(node.getNodeName().trim()))
                .findFirst()
                .orElse(null);
    }

    private Object extractFieldValue(Object target, String fieldName) {
        if (target == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        if (target instanceof Map<?, ?> map) {
            return map.get(fieldName);
        }
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            return target.getClass().getMethod(getterName).invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String sourceName(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        int queryIndex = url.indexOf('?');
        String clean = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        int slashIndex = clean.lastIndexOf('/');
        return slashIndex >= 0 ? clean.substring(slashIndex + 1) : clean;
    }

    private static class YilabaoSize {
        private final double widthMm;
        private final double heightMm;

        private YilabaoSize(double widthMm, double heightMm) {
            this.widthMm = widthMm;
            this.heightMm = heightMm;
        }

        private static YilabaoSize fromCentimeter(double widthCm, double heightCm) {
            return new YilabaoSize(widthCm * CM_TO_MM, heightCm * CM_TO_MM);
        }
    }
}
