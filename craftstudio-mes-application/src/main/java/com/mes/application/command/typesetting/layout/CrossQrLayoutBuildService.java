package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.enums.FormeGenerationElementType;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.util.ProcedureFlowNodeMatcher;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CrossQrLayoutBuildService extends AbstractLayoutModeBuildService {

    private static final int TAG_DPI = 300;
    private static final double MM_PER_INCH = 25.4D;
    private static final int QR_LEFT_MM = 10;
    private static final int ELEMENT_F_SHIFT_MM = 30;
    private static final int QR_SIZE_MM = 15;
    private static final int QR_BOTTOM_GAP_MM = 2;
    private static final int ELEMENT_GAP_MM = 30;
    private static final int EXTRA_INFO_GAP_MM = 5;
    private static final int WRAPPED_EXT_INFO_TEXT_HEIGHT_MM = 4;
    private static final double WRAPPED_EXT_INFO_ROW_GAP_MM = 1.5D;
    private static final int ANCHOR_SIZE_MM = 4;
    private static final int ANCHOR_GAP_TO_MARGIN_BOTTOM_MM = 2;
    private static final int TOP_ANCHOR_LEFT_MM = QR_LEFT_MM + QR_SIZE_MM + 15;
    private static final int TOP_ANCHOR_RIGHT_MM = 40;
    private static final int BOTTOM_ANCHOR_LEFT_MM = 40;
    private static final int BOTTOM_ANCHOR_RIGHT_MM = 40;
    private static final int NESTED_HEIGHT_EXPAND_THRESHOLD_MM = 2400;
    private static final int SIDE_EXPAND_MM = 6;
    private static final int SIDE_ANCHOR_INTERVAL_MM = 1150;
    private static final int ANCHOR_INNER_SHIFT_MM = 30;
    private static final String TAG_TEXT_FONT = "Source Han Sans SC VF";
    private static final String RIGHT_ARROW_URL = "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/basetag-rightarrow.png";

    private final OssTagUploadService ossTagUploadService;
    private final QrLayoutOrderIdResolver qrLayoutOrderIdResolver;
    private final QrLayoutRedoResolver qrLayoutRedoResolver;

    @Autowired
    public CrossQrLayoutBuildService(OssTagUploadService ossTagUploadService,
                                       QrLayoutOrderIdResolver qrLayoutOrderIdResolver,
                                       QrLayoutRedoResolver qrLayoutRedoResolver) {
        this.ossTagUploadService = ossTagUploadService;
        this.qrLayoutOrderIdResolver = qrLayoutOrderIdResolver;
        this.qrLayoutRedoResolver = qrLayoutRedoResolver;
    }

    /**
     * 保留给不关心重做标记的旧调用方；Spring 运行时使用三参数构造器。
     */
    public CrossQrLayoutBuildService(OssTagUploadService ossTagUploadService,
                                      QrLayoutOrderIdResolver qrLayoutOrderIdResolver) {
        this(ossTagUploadService, qrLayoutOrderIdResolver, null);
    }

    /**
     * 十字二维码排版模式构建器：
     * - 上下 margin 固定 30mm；
     * - marks 使用 C+B+A 拼接生成的标签条；
     * - 定位点使用 basetag/basetag-cross.svg。
     */
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CROSS;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        int anchorSizeMm = resolveAnchorSizeMm(context, ANCHOR_SIZE_MM);
        BigDecimal anchorSize = BigDecimal.valueOf(anchorSizeMm);
        // 1) 基于 mode 规则确定 margin 与元素原点（扩展矩形左上角为坐标原点）
        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        BigDecimal marginHeight = context.getMarginHeight();
        int nestedHeight = context.getNestedHeight().intValue();
        boolean needSideExpand = nestedHeight > NESTED_HEIGHT_EXPAND_THRESHOLD_MM;
        int marginLeft = needSideExpand ? SIDE_EXPAND_MM : 0;
        int marginTop = marginHeight.intValue();
        int marginRight = needSideExpand ? SIDE_EXPAND_MM : 0;
        int marginBottom = marginHeight.intValue();
        int elementOriginX = 0;
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
        log.info("Cross QR tag text resolved, elementA={}, elementAExtInfos={}, commonOrderId={}", elementA, elementAExtInfos, commonOrderId);
        String elementB = context.getPlateNameSupplier().get();
        String elementBB = context.getPlateNameBBSupplier().get();
        boolean hasDoubleSideMounting = allCellsAreProductionPieces(context.getTypesettingInfo())
                && hasDoubleSideMounting(context.getTypesettingInfo());
        String elementC = context.getQrDataUriGenerator().apply(elementB);
        String elementCC = context.getQrDataUriGenerator().apply(elementBB);
        String manufacturerMetaId = context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getManufacturerMetaId();
        String typesettingId = context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getTypesettingId();
        boolean hasRedoProductionPiece = qrLayoutRedoResolver != null
                && qrLayoutRedoResolver.hasRedoProductionPiece(context.getTypesettingInfo());
        BigDecimal tagContentMaxWidth = context.getNestedWidth().add(BigDecimal.valueOf(marginRight));
        String elementF = buildTagStripDataUri(context.getBusinessId(), manufacturerMetaId, typesettingId, elementA, elementAExtInfos, commonOrderId, elementB, elementC, context.getNestedWidth(), tagContentMaxWidth, marginHeight, false, hasRedoProductionPiece);
        String elementFRotated = buildTagStripDataUri(context.getBusinessId(), manufacturerMetaId, typesettingId, elementA, elementAExtInfos, commonOrderId, elementBB, elementCC, context.getNestedWidth(), tagContentMaxWidth, marginHeight, true, hasRedoProductionPiece);
        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> marks = new LinkedHashMap<>();
            marks.put("elementF", elementF);
            marks.put("elementFRotated", elementFRotated);
            if (hasDoubleSideMounting) {
                marks.put("elementG", RIGHT_ARROW_URL);
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
        List<FormeGenerationRequest.Mark> marks = new ArrayList<>(Arrays.asList(top, bottom));
        if (hasDoubleSideMounting) {
            FormeGenerationRequest.Mark rightArrow = new FormeGenerationRequest.Mark();
            rightArrow.setImg(RIGHT_ARROW_URL);
            rightArrow.setSize(createSize(BigDecimal.valueOf(QR_SIZE_MM), BigDecimal.valueOf(QR_SIZE_MM)));
            int arrowX = context.getNestedWidth().intValue() - QR_LEFT_MM - QR_SIZE_MM;
            int arrowY = (marginHeight.intValue() - QR_SIZE_MM) / 2;
            rightArrow.setPosition(createPosition(arrowX, arrowY));
            marks.add(rightArrow);
        }
        result.setMarks(marks);

        // 4) 在上/下 margin 区域插入 4 个十字定位点（左右各 70mm）
        int topY = marginTop - ANCHOR_GAP_TO_MARGIN_BOTTOM_MM - anchorSizeMm;
        int bottomY = elementOriginY + nestedHeight + ANCHOR_GAP_TO_MARGIN_BOTTOM_MM;
        int width = context.getNestedWidth().intValue();
        int expandedWidth = width + marginLeft + marginRight;
        int topLeftX = elementOriginX + TOP_ANCHOR_LEFT_MM + ANCHOR_INNER_SHIFT_MM;
        int topRightX = Math.max(elementOriginX + expandedWidth - TOP_ANCHOR_RIGHT_MM - anchorSizeMm - ANCHOR_INNER_SHIFT_MM, topLeftX);
        int bottomLeftX = elementOriginX + BOTTOM_ANCHOR_LEFT_MM + ANCHOR_INNER_SHIFT_MM;
        int bottomRightX = Math.max(elementOriginX + expandedWidth - BOTTOM_ANCHOR_RIGHT_MM - anchorSizeMm - ANCHOR_INNER_SHIFT_MM, bottomLeftX);
        String crossSvgUrl = "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/basetag-cross.svg";

        FormeGenerationRequest.AnchorPoint tl = new FormeGenerationRequest.AnchorPoint();
        tl.setImg("basetag-cross.png");
        tl.setType(FormeGenerationElementType.ANCHOR_POINT_CROSS.getCode());
        tl.setSvg(crossSvgUrl);
        tl.setSize(createSize(anchorSize, anchorSize));
        tl.setPosition(createPosition(topLeftX, topY));

        FormeGenerationRequest.AnchorPoint tr = new FormeGenerationRequest.AnchorPoint();
        tr.setImg("basetag-cross.png");
        tr.setType(FormeGenerationElementType.ANCHOR_POINT_CROSS.getCode());
        tr.setSvg(crossSvgUrl);
        tr.setSize(createSize(anchorSize, anchorSize));
        tr.setPosition(createPosition(topRightX, topY));

        FormeGenerationRequest.AnchorPoint bl = new FormeGenerationRequest.AnchorPoint();
        bl.setImg("basetag-cross.png");
        bl.setType(FormeGenerationElementType.ANCHOR_POINT_CROSS.getCode());
        bl.setSvg(crossSvgUrl);
        bl.setSize(createSize(anchorSize, anchorSize));
        bl.setPosition(createPosition(bottomLeftX, bottomY));

        FormeGenerationRequest.AnchorPoint br = new FormeGenerationRequest.AnchorPoint();
        br.setImg("basetag-cross.png");
        br.setType(FormeGenerationElementType.ANCHOR_POINT_CROSS.getCode());
        br.setSvg(crossSvgUrl);
        br.setSize(createSize(anchorSize, anchorSize));
        br.setPosition(createPosition(bottomRightX, bottomY));
        List<FormeGenerationRequest.AnchorPoint> anchorPoints = new ArrayList<>(Arrays.asList(tl, tr, bl, br));
        if (needSideExpand) {
            int leftExpandCenterX = elementOriginX + (marginLeft / 2) - anchorSizeMm / 2;
            int rightExpandCenterX = elementOriginX + expandedWidth - (marginRight / 2) - anchorSizeMm / 2;
            for (int offsetY = SIDE_ANCHOR_INTERVAL_MM; offsetY <= nestedHeight; offsetY += SIDE_ANCHOR_INTERVAL_MM) {
                int pointY = elementOriginY + offsetY - anchorSizeMm / 2;

                FormeGenerationRequest.AnchorPoint leftPoint = new FormeGenerationRequest.AnchorPoint();
                leftPoint.setImg("basetag-cross.png");
                leftPoint.setType(FormeGenerationElementType.ANCHOR_POINT_CROSS.getCode());
                leftPoint.setSvg(crossSvgUrl);
                leftPoint.setSize(createSize(anchorSize, anchorSize));
                leftPoint.setPosition(createPosition(leftExpandCenterX, pointY));

                FormeGenerationRequest.AnchorPoint rightPoint = new FormeGenerationRequest.AnchorPoint();
                rightPoint.setImg("basetag-cross.png");
                rightPoint.setType(FormeGenerationElementType.ANCHOR_POINT_CROSS.getCode());
                rightPoint.setSvg(crossSvgUrl);
                rightPoint.setSize(createSize(anchorSize, anchorSize));
                rightPoint.setPosition(createPosition(rightExpandCenterX, pointY));

                anchorPoints.add(leftPoint);
                anchorPoints.add(rightPoint);
            }
        }
        result.setAnchorPoints(anchorPoints);

        // 5) 输出配置与上传目录
        result.setOutputs(buildDefaultOutputs(supportMode(), context, elementB, elementBB));
        result.setUploadPath("printingplate/");
        return result;
    }

    private boolean allCellsAreProductionPieces(TypesettingInfo info) {
        if (info == null || info.getTypesettingCells() == null || info.getTypesettingCells().isEmpty()) {
            return false;
        }
        return info.getTypesettingCells().stream()
                .allMatch(cell -> cell != null && TypesettingSourceType.PART.getCode().equals(cell.getSourceType()));
    }

    private boolean hasDoubleSideMounting(TypesettingInfo info) {
        return info != null
                && ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(info.getProcedureFlow());
    }

    private List<String> buildElementAExtInfos(TypesettingInfo info) {
        List<String> extInfos = new ArrayList<>();
        if (info == null) {
            return extInfos;
        }
        String plateSize = formatPlateSize(info);
        if (StringUtils.isNotBlank(plateSize)) {
            extInfos.add(plateSize);
        }
        if (StringUtils.isNotBlank(info.getTemplateCode()) && !"1/1".equals(info.getTemplateCode())) {
            extInfos.add(info.getTemplateCode());
        }
        extInfos.addAll(extractAccessoryLabels(info));
        return extInfos;
    }

    private String formatPlateSize(TypesettingInfo info) {
        if (info == null || info.getElement() == null
                || info.getElement().getWidth() == null || info.getElement().getHeight() == null) {
            return null;
        }
        return formatTwoDecimal(info.getElement().getWidth()) + "*" + formatTwoDecimal(info.getElement().getHeight());
    }

    private String formatTwoDecimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private List<String> extractAccessoryLabels(TypesettingInfo info) {
        if (info.getProcedureFlow() == null || info.getProcedureFlow().getNodes() == null) {
            return new ArrayList<>();
        }
        List<ProcedureFlowNode> nodes = info.getProcedureFlow().getNodes().stream()
                .filter(Objects::nonNull)
                .filter(node -> shouldExtractAccessoryLabel(node.getNodeName()))
                .collect(Collectors.toList());
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            labels.add(extractAccessoryLabelFromNode(nodes.get(i), i == 0 ? resolveFirstNodeLabel(info) : null));
        }
        return labels.stream()
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

    private String extractAccessoryLabelFromNode(ProcedureFlowNode node, String overrideNodeName) {
        String displayName = StringUtils.isNotBlank(overrideNodeName) ? overrideNodeName : node.getNodeName();
        if (node.getParamConfigs() == null) {
            return displayName == null ? "" : displayName;
        }
        for (Object config : node.getParamConfigs()) {
            Object param = invokeGetter(config, "getParam");
            Object accessorySnapshot = param instanceof Map ? ((Map<?, ?>) param).get("accessorySnapshot") : invokeGetter(param, "getAccessorySnapshot");
            Object name = accessorySnapshot instanceof Map ? ((Map<?, ?>) accessorySnapshot).get("name") : invokeGetter(accessorySnapshot, "getName");
            if (name != null && StringUtils.isNotBlank(name.toString())) {
                return displayName + "：<font color='red'>" + name + "</font>";
            }
        }
        return displayName == null ? "" : displayName;
    }

    private String resolveFirstNodeLabel(TypesettingInfo info) {
        if (info.getMaterialConfig() == null || info.getMaterialConfig().getMaterialSnapshot() == null) {
            return null;
        }
        String materialName = info.getMaterialConfig().getMaterialSnapshot().getName();
        return StringUtils.isBlank(materialName) ? null : "<font color='red'>" + materialName + "</font>";
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
                                        BigDecimal tagContentMaxWidth,
                                        BigDecimal stripHeight,
                                        boolean rotate180,
                                        boolean hasRedoProductionPiece) {
        return com.mes.application.command.graphics.RemoteGraphics.png("CROSS_TAG", com.mes.application.command.graphics.RemoteGraphics.params("businessId", businessId, "manufacturerMetaId", manufacturerMetaId, "typesettingId", typesettingId, "elementA", elementA, "elementAExtInfos", elementAExtInfos, "commonOrderId", commonOrderId, "elementB", elementB, "qrDataUri", qrDataUri, "stripWidth", stripWidth, "tagContentMaxWidth", tagContentMaxWidth, "stripHeight", stripHeight, "rotate180", rotate180, "hasRedoProductionPiece", hasRedoProductionPiece)).url();
    }
}
