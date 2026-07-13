package com.mes.application.command.typesetting.proces.material;

import com.mes.application.command.typesetting.proces.liubai.AbstractCentimeterLiubaiProcessStrategy;
import com.mes.application.command.typesetting.proces.liubai.LiubaiProcessContext;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.service.ManufacturerMaterialLayoutSpecCfgService;
import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpecStep;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 特殊材料工艺策略基类。
 *
 * <p>特殊材料没有显式“留白”工艺节点时，也需要按固定规格执行与留白相同的
 * mask SVG 重构/续写、四边标签生成和生产工件宽高回写流程。</p>
 */
public abstract class AbstractMaterialProcessStrategy extends AbstractCentimeterLiubaiProcessStrategy {
    private static final Pattern SVG_OPEN_PATTERN = Pattern.compile("<svg\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_WIDTH_PATTERN = Pattern.compile("width\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_HEIGHT_PATTERN = Pattern.compile("height\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);

    private final OssTagUploadService ossTagUploadService;
    private final ManufacturerMaterialLayoutSpecCfgService layoutSpecCfgService;

    protected AbstractMaterialProcessStrategy(int expandCm,
                                              RestTemplate restTemplate,
                                              OssTagUploadService ossTagUploadService,
                                              ManufacturerMaterialLayoutSpecCfgService layoutSpecCfgService) {
        super(expandCm, restTemplate, ossTagUploadService);
        this.ossTagUploadService = ossTagUploadService;
        this.layoutSpecCfgService = layoutSpecCfgService;
    }

    @Override
    public boolean matches(LiubaiProcessContext context) {
        if (!(context instanceof MaterialProcessContext)
                || context.getProcedureFlow() == null
                || context.getOrderItem() == null) {
            return false;
        }
        ProcedureFlow procedureFlow = context.getProcedureFlow();
        return !hasNode(procedureFlow, "异形切割")
                && !hasLiubaiNode(procedureFlow)
                && matchesMaterialName(resolveMaterialName(context.getOrderItem()));
    }

    @Override
    public void process(LiubaiProcessContext context) {
        super.process(context);
        applyMaterialLayoutInset(context);
    }

    @Override
    protected boolean matchesLiubaiValue(ProcedureFlow procedureFlow) {
        return true;
    }

    protected abstract boolean matchesMaterialName(String materialName);

    private void applyMaterialLayoutInset(LiubaiProcessContext context) {
        ProductionPiece piece = context == null ? null : context.getProductionPiece();
        OrderItem orderItem = context == null ? null : context.getOrderItem();
        if (piece == null || piece.getMaskImageFile() == null || StringUtils.isBlank(piece.getMaskImageFile().getRawFile())
                || orderItem == null || orderItem.getMaterial() == null || StringUtils.isBlank(orderItem.getMaterial().getMaterialId())
                || StringUtils.isBlank(piece.getManufacturerId())) {
            return;
        }
        ManufacturerMaterialLayoutSpecCfg cfg = layoutSpecCfgService.findByManufacturerMetaIdAndMaterialId(
                piece.getManufacturerId(), orderItem.getMaterial().getMaterialId());
        if (cfg == null || cfg.getInsetSteps() == null || cfg.getInsetSteps().isEmpty()) {
            return;
        }
        String currentMaskUrl = piece.getMaskImageFile().getRawFile();
        String svg = resolveSvg(currentMaskUrl);
        if (StringUtils.isBlank(svg)) {
            return;
        }
        double width = resolveDimension(svg, SVG_WIDTH_PATTERN, piece.getWidth());
        double height = resolveDimension(svg, SVG_HEIGHT_PATTERN, piece.getHeight());
        if (width <= 0D || height <= 0D) {
            return;
        }
        Double widthInsetMm = resolveInsetMm(cfg.getInsetSteps(), width);
        Double heightInsetMm = resolveInsetMm(cfg.getInsetSteps(), height);
        if (widthInsetMm == null && heightInsetMm == null) {
            return;
        }
        double newWidth = Math.max(0D, width + nullToZero(widthInsetMm));
        double newHeight = Math.max(0D, height + nullToZero(heightInsetMm));
        String updatedSvg = updateRootSize(svg, newWidth, newHeight);
        String manufacturerMetaId = piece.getManufacturerId();
        String businessId = StringUtils.isNotBlank(piece.getProductionPieceId()) ? piece.getProductionPieceId() : piece.getId();
        String orderItemId = StringUtils.isNotBlank(orderItem.getOrderItemId()) ? orderItem.getOrderItemId() : "unknown";
        String uploadPath = "mask/" + manufacturerMetaId + "/" + orderItemId + "/material-layout-inset/";
        String newMaskUrl = ossTagUploadService.uploadTagSvg(businessId, updatedSvg.getBytes(StandardCharsets.UTF_8), uploadPath);
        updateMaskImageFile(piece, newMaskUrl);
        piece.setTrueWidth(newWidth);
        piece.setTrueHeight(newHeight);
    }

    private Double resolveInsetMm(List<MaterialLayoutSpecStep> steps, double lengthMm) {
        double lengthMeter = lengthMm / 1000D;
        List<MaterialLayoutSpecStep> sortedSteps = steps.stream()
                .filter(step -> step != null && step.getMaxLengthMeter() != null && step.getInsetCm() != null)
                .sorted(Comparator.comparing(MaterialLayoutSpecStep::getMaxLengthMeter))
                .toList();
        MaterialLayoutSpecStep previousStep = null;
        for (MaterialLayoutSpecStep currentStep : sortedSteps) {
            if (currentStep.getMaxLengthMeter().compareTo(BigDecimal.valueOf(lengthMeter)) >= 0) {
                BigDecimal insetCm = interpolateInsetCm(previousStep, currentStep, lengthMeter);
                return cmToMm(insetCm);
            }
            previousStep = currentStep;
        }
        return previousStep == null ? null : cmToMm(previousStep.getInsetCm());
    }

    private BigDecimal interpolateInsetCm(MaterialLayoutSpecStep previousStep, MaterialLayoutSpecStep currentStep, double lengthMeter) {
        if (previousStep == null
                || currentStep.getMaxLengthMeter().equals(previousStep.getMaxLengthMeter())) {
            return currentStep.getInsetCm();
        }
        double stepLengthRange = currentStep.getMaxLengthMeter().subtract(previousStep.getMaxLengthMeter()).doubleValue();
        double lengthOffset = BigDecimal.valueOf(lengthMeter).subtract(previousStep.getMaxLengthMeter()).doubleValue();
        BigDecimal insetRange = currentStep.getInsetCm().subtract(previousStep.getInsetCm());
        return previousStep.getInsetCm().add(insetRange.multiply(BigDecimal.valueOf(lengthOffset / stepLengthRange)));
    }

    private double cmToMm(BigDecimal insetCm) {
        return insetCm == null ? 0D : insetCm.multiply(BigDecimal.TEN).doubleValue();
    }

    private double nullToZero(Double value) {
        return value == null ? 0D : value;
    }

    private String updateRootSize(String svg, double width, double height) {
        Matcher matcher = SVG_OPEN_PATTERN.matcher(svg);
        if (!matcher.find()) {
            return svg;
        }
        String openTag = matcher.group();
        String updatedOpenTag = upsertSvgAttribute(openTag, "width", format(width));
        updatedOpenTag = upsertSvgAttribute(updatedOpenTag, "height", format(height));
        return svg.substring(0, matcher.start()) + updatedOpenTag + svg.substring(matcher.end());
    }

    private String upsertSvgAttribute(String tag, String name, String value) {
        Pattern attributePattern = Pattern.compile("\\s" + Pattern.quote(name) + "\\s*=\\s*([\"']).*?\\1", Pattern.CASE_INSENSITIVE);
        Matcher matcher = attributePattern.matcher(tag);
        String attribute = " " + name + "=\"" + escapeAttr(value) + "\"";
        if (matcher.find()) {
            return matcher.replaceFirst(Matcher.quoteReplacement(attribute));
        }
        int insertIndex = tag.endsWith("/>") ? tag.length() - 2 : tag.length() - 1;
        return tag.substring(0, insertIndex) + attribute + tag.substring(insertIndex);
    }

    private void updateMaskImageFile(ProductionPiece piece, String maskUrl) {
        ImageFile maskFile = piece.getMaskImageFile();
        maskFile.setRawFile(maskUrl);
        FilePreview preview = maskFile.getFilePreview();
        if (preview == null) {
            preview = new FilePreview();
            maskFile.setFilePreview(preview);
        }
        preview.setRaw(maskUrl);
        preview.setPreview(maskUrl);
        preview.setThumbnail(maskUrl);
    }

    private String resolveMaterialName(OrderItem orderItem) {
        if (orderItem == null || orderItem.getMaterial() == null || orderItem.getMaterial().getMaterialSnapshot() == null) {
            return "";
        }
        String materialName = orderItem.getMaterial().getMaterialSnapshot().getName();
        return StringUtils.isBlank(materialName) ? "" : materialName;
    }
}
