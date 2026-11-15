package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DoubleSideMountingLayoutBuildService extends AbstractLayoutModeBuildService {
    private static final int TAG_DPI = 300;
    private static final double MM_PER_INCH = 25.4D;
    private static final int QR_LEFT_MM = 10;
    private static final int QR_SIZE_MM = 15;
    private static final int QR_BOTTOM_GAP_MM = 2;
    private static final int ELEMENT_GAP_MM = 30;
    private static final int EXTRA_INFO_GAP_MM = 5;
    private static final int NESTED_HEIGHT_EXPAND_THRESHOLD_MM = 2400;
    private static final int SIDE_EXPAND_MM = 6;
    private static final String TAG_TEXT_FONT = "Source Han Sans SC VF";
    private static final String LEFT_ARROW_URL = "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/basetag-leftarrow.png";
    private static final String RIGHT_ARROW_URL = "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/basetag-rightarrow.png";

    private final OssTagUploadService ossTagUploadService;
    private final QrLayoutOrderIdResolver qrLayoutOrderIdResolver;

    public DoubleSideMountingLayoutBuildService(OssTagUploadService ossTagUploadService,
                                                QrLayoutOrderIdResolver qrLayoutOrderIdResolver) {
        this.ossTagUploadService = ossTagUploadService;
        this.qrLayoutOrderIdResolver = qrLayoutOrderIdResolver;
    }
    /**
     * 圆形二维码排版模式构建器：
     * - 上下 margin 固定 30mm；
     * - marks 使用 C+B+A 拼接生成的标签条；
     * - 定位点使用 basetag/basetag-circle.svg。
     */
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.DOUBLE_SIDE_MOUNTING_LAYOUT;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        // 1) 基于 mode 规则确定 margin 与元素原点（扩展矩形左上角为坐标原点）
        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        BigDecimal marginHeight = context.getMarginHeight();
        int nestedHeight = context.getNestedHeight().intValue();
        boolean needSideExpand = nestedHeight > NESTED_HEIGHT_EXPAND_THRESHOLD_MM;
        int marginLeft = needSideExpand ? SIDE_EXPAND_MM : 0;
        int marginTop = marginHeight.intValue();
        int marginRight = needSideExpand ? SIDE_EXPAND_MM : 0;
        int marginBottom = marginHeight.intValue();
        int elementOriginY = marginTop;
        int nestedStartX = marginLeft;

        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(marginLeft);
        margin.setTop(marginTop);
        margin.setRight(marginRight);
        margin.setBottom(marginBottom);
        result.setMargin(margin);

        // 2) 构建 A/B/C/F：A=typesetting引用标识，B=队列plt名，C=二维码，F=标签条
        String elementA = context.getElementAResolver().apply(context.getTypesettingInfo());
        List<String> elementAExtInfos = buildElementAExtInfos(context.getTypesettingInfo());
        String commonOrderId = qrLayoutOrderIdResolver.resolveCommonOrderId(context.getTypesettingInfo());
        log.info("Circle QR tag text resolved, elementA={}, elementAExtInfos={}, commonOrderId={}", elementA, elementAExtInfos, commonOrderId);
        String elementB = context.getPlateNameSupplier().get();
        String elementBB = context.getPlateNameBBSupplier().get();
        boolean allCellsAreParts = allCellsAreProductionPieces(context.getTypesettingInfo());
        String elementC = allCellsAreParts ? LEFT_ARROW_URL : null;
        String elementCC = allCellsAreParts ? LEFT_ARROW_URL : null;
        String manufacturerMetaId = context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getManufacturerMetaId();
        String typesettingId = normalizeMirrorTypesettingId(context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getTypesettingId());
        String elementF = buildTagStripDataUri(context.getBusinessId(), manufacturerMetaId, typesettingId, elementA, elementAExtInfos, commonOrderId, elementB, elementC, context.getNestedWidth(), marginHeight, false);
        String elementFRotated = buildTagStripDataUri(context.getBusinessId(), manufacturerMetaId, typesettingId, elementA, elementAExtInfos, commonOrderId, elementBB, elementCC, context.getNestedWidth(), marginHeight, true);
        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> marks = new LinkedHashMap<>();
            marks.put("elementF", elementF);
            marks.put("elementFRotated", elementFRotated);
            if (allCellsAreParts) {
                marks.put("elementG", LEFT_ARROW_URL);
            }
            context.getTypesettingInfo().setMarks(marks);
        }

        // 3) 将标签条放置在上/下 margin 区域
        FormeGenerationRequest.Mark top = new FormeGenerationRequest.Mark();
        top.setImg(elementF);
        top.setSize(createSize(context.getNestedWidth(), marginHeight));
        top.setPosition(createPosition(nestedStartX, 0));

        FormeGenerationRequest.Mark bottom = new FormeGenerationRequest.Mark();
        bottom.setImg(elementFRotated);
        bottom.setSize(createSize(context.getNestedWidth(), marginHeight));
        bottom.setPosition(createPosition(nestedStartX, elementOriginY + context.getNestedHeight().intValue()));
        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();
        marks.add(top);
        marks.add(bottom);
        if (allCellsAreParts) {
            FormeGenerationRequest.Mark leftArrow = new FormeGenerationRequest.Mark();
            leftArrow.setImg(LEFT_ARROW_URL);
            leftArrow.setSize(createSize(BigDecimal.valueOf(QR_SIZE_MM), BigDecimal.valueOf(QR_SIZE_MM)));
            int arrowX = QR_LEFT_MM;
            int arrowY = (marginHeight.intValue() - QR_SIZE_MM) / 2;
            leftArrow.setPosition(createPosition(arrowX, arrowY));
            marks.add(leftArrow);
        }
        result.setMarks(marks);

        // 双面对裱镜像印版不需要生成定位点
        result.setAnchorPoints(new ArrayList<>());

        // 5) 输出配置与上传目录
        result.setOutputs(buildDefaultOutputs(supportMode(), context, elementB, elementBB));
        result.setUploadPath("printingplate/");
        return result;
    }


    /**
     * 镜像印版与正面印版属于同一次排版业务，标签与 mark 上传目录继续使用原始 typesettingId。
     */
    private String normalizeMirrorTypesettingId(String typesettingId) {
        if (StringUtils.isBlank(typesettingId)) {
            return typesettingId;
        }
        String mirrorSuffix = "-Mirror";
        return typesettingId.toLowerCase(Locale.ROOT).endsWith(mirrorSuffix.toLowerCase(Locale.ROOT))
                ? typesettingId.substring(0, typesettingId.length() - mirrorSuffix.length())
                : typesettingId;
    }

    private boolean allCellsAreProductionPieces(TypesettingInfo info) {
        if (info == null || info.getTypesettingCells() == null || info.getTypesettingCells().isEmpty()) {
            return false;
        }
        return info.getTypesettingCells().stream()
                .allMatch(cell -> cell != null && TypesettingSourceType.PART.getCode().equals(cell.getSourceType()));
    }

    private List<String> buildElementAExtInfos(TypesettingInfo info) {
        List<String> extInfos = new ArrayList<>();
        if (info == null) {
            return extInfos;
        }
        String plateArea = formatPlateArea(info);
        if (StringUtils.isNotBlank(plateArea)) {
            extInfos.add(plateArea);
        }
        if (StringUtils.isNotBlank(info.getTemplateCode()) && !"1/1".equals(info.getTemplateCode())) {
            extInfos.add(info.getTemplateCode());
        }
        extInfos.addAll(extractAccessoryLabels(info));
        return extInfos;
    }

    private String formatPlateArea(TypesettingInfo info) {
        if (info == null || info.getElement() == null
                || info.getElement().getWidth() == null || info.getElement().getHeight() == null) {
            return null;
        }
        return formatTwoDecimal(info.getElement().getWidth().multiply(info.getElement().getHeight()));
    }

    private String formatTwoDecimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private List<String> extractAccessoryLabels(TypesettingInfo info) {
        if (info.getProcedureFlow() == null || info.getProcedureFlow().getNodes() == null) {
            return new ArrayList<>();
        }
        return info.getProcedureFlow().getNodes().stream()
                .filter(Objects::nonNull)
                .filter(node -> shouldExtractAccessoryLabel(node.getNodeName()))
                .map(this::extractAccessoryLabelFromNode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean shouldExtractAccessoryLabel(String nodeName) {
        if (StringUtils.isBlank(nodeName)) {
            return false;
        }
        return !"预处理".equals(nodeName)
                && !"待排版".equals(nodeName)
                && !"排版中".equals(nodeName)
                && !"打印中".equals(nodeName)
                && !"待打包".equals(nodeName)
                && !"已打包".equals(nodeName);
    }

    private String extractAccessoryLabelFromNode(ProcedureFlowNode node) {
        if (node.getParamConfigs() == null) {
            return "";
        }
        for (Object config : node.getParamConfigs()) {
            Object param = invokeGetter(config, "getParam");
            Object accessorySnapshot = param instanceof Map ? ((Map<?, ?>) param).get("accessorySnapshot") : invokeGetter(param, "getAccessorySnapshot");
            Object name = accessorySnapshot instanceof Map ? ((Map<?, ?>) accessorySnapshot).get("name") : invokeGetter(accessorySnapshot, "getName");
            if (name != null && StringUtils.isNotBlank(name.toString())) {
                return node.getNodeName() + "：<font color='red'>" + name + "</font>";
            }
        }
        return node.getNodeName() == null ? "" : node.getNodeName();
    }

    private Object invokeGetter(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String buildTagStripDataUri(String businessId,
                                        String manufacturerMetaId,
                                        String typesettingId,
                                        String elementA,
                                        List<String> elementAExtInfos,
                                        String commonOrderId,
                                        String elementB,
                                        String qrDataUri,
                                        BigDecimal stripWidth,
                                        BigDecimal stripHeight,
                                        boolean rotate180) {
        return com.mes.application.command.graphics.RemoteGraphics.png("DOUBLE_TAG", com.mes.application.command.graphics.RemoteGraphics.params("businessId", businessId, "manufacturerMetaId", manufacturerMetaId, "typesettingId", typesettingId, "elementA", elementA, "elementAExtInfos", elementAExtInfos, "commonOrderId", commonOrderId, "elementB", elementB, "qrDataUri", qrDataUri, "stripWidth", stripWidth, "stripHeight", stripHeight, "rotate180", rotate180)).url();
    }
}
