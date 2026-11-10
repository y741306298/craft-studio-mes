package com.mes.application.command.orderPreprocessing.sublimation;

import com.mes.application.command.typesetting.proces.liubai.AbstractFixedLiubaiProcessStrategy;
import com.mes.application.command.typesetting.proces.liubai.LiubaiProcessContext;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** 仅装饰已外扩矩形：旗帜扣点/绑带点、灰色工艺标签，最后叠加2mm黑边。 */
@Service
public class SublimationDecorationService extends AbstractFixedLiubaiProcessStrategy {
    private final OssTagUploadService uploads;

    public SublimationDecorationService(RestTemplate restTemplate, OssTagUploadService uploads) {
        super(restTemplate, uploads);
        this.uploads = uploads;
    }

    /** 不参与留白的首个命中调度，由热升华策略独立调用。 */
    @Override public boolean matches(LiubaiProcessContext context) { return false; }
    @Override protected String specName() { return "sublimation"; }
    @Override protected double expandMm() { return 1D; }
    @Override protected String[] matchKeywords() { return new String[0]; }
    @Override protected double liubaiTagFontScale() { return 0.5D; }
    // 原#808080再向白色调浅50%，得到#C0C0C0。
    @Override protected List<Color> liubaiTagTextColors() { return List.of(new Color(192, 192, 192)); }
    @Override protected String tagMarkKeyPrefix() { return "sublimation-tag"; }
    @Override protected String buildLiubaiTagText(LiubaiProcessContext context) {
        String text = context.getProductionPiece().getProcessingFlow();
        if (text != null && !text.isBlank()) return text;
        return context.getProcedureFlow().getNodes().stream().filter(java.util.Objects::nonNull)
                .map(com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode::getNodeName)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.joining(" → "));
    }

    public String decorate(OrderItem item, ProcedureFlow flow, ProductionPiece piece, SublimationPlan plan, String backUrl) {
        if (piece.getWidth() == null || piece.getHeight() == null) {
            throw new IllegalArgumentException("热升华工件缺少扩展后的宽高");
        }
        double width = piece.getWidth();
        double height = piece.getHeight();
        String pieceId = ensureProductionPieceBusinessId(piece);
        String mongoId = ensureProductionPieceMongoId(piece);
        String dir = "mask/" + item.getManufacturerId() + "/" + item.getOrderItemId() + "/sublimation/";
        if (piece.getMarks() == null) piece.setMarks(new LinkedHashMap<>());
        StringBuilder svg = new StringBuilder(SublimationSvg.open(width, height));
        svg.append(SublimationSvg.original(mongoId, item.getProductionImgFile().getRawFile(), width, height));
        appendLabels(svg, item, flow, piece, plan, width, height);
        List<SublimationGeometry.Point> points = new ArrayList<>();
        if (plan.cornerBuckles()) points.addAll(SublimationGeometry.corners(width, height));
        for (var edge : SublimationGeometry.Edge.values()) {
            if (plan.buckleEdges().contains(edge)) {
                points.addAll(SublimationGeometry.edgePoints(edge, SublimationGeometry.MarkKind.BUCKLE, width, height));
            }
            if (plan.tieEdges().contains(edge)) {
                points.addAll(SublimationGeometry.edgePoints(edge, SublimationGeometry.MarkKind.TIE, width, height));
            }
        }
        for (var point : points) {
            svg.append(SublimationSvg.point(mongoId, point));
            piece.getMarks().put(SublimationSvg.pointKey(point), SublimationSvg.POINT_IMG);
        }
        String borderUrl = uploads.uploadTagPng(pieceId, SublimationSvg.borderPng(width, height), dir);
        piece.getMarks().put("sublimation-border", borderUrl);
        svg.append(SublimationSvg.border(mongoId, borderUrl, width, height)).append("</svg>");
        String mask = uploads.uploadTagSvg(pieceId, svg.toString().getBytes(StandardCharsets.UTF_8), dir);
        updateMaskImageFile(piece, mask);
        if (backUrl == null || backUrl.isBlank()) return null;
        // 反面遮罩复用相同尺寸/点位，但原图分组必须引用真实反面图，不能引用正面图。
        String backSvg = SublimationSvg.forBack(svg.toString(), mongoId, item.getProductionImgFile().getRawFile(), backUrl);
        return uploads.uploadTagSvg(pieceId, backSvg.getBytes(StandardCharsets.UTF_8), dir);
    }

    private void appendLabels(StringBuilder svg, OrderItem item, ProcedureFlow flow, ProductionPiece piece,
                              SublimationPlan plan, double width, double height) {
        var m = plan.labelMargins();
        double available = Math.max(Math.max(m.left(), m.right()), Math.max(m.top(), m.bottom()));
        if (available <= 0) return;
        LiubaiProcessContext context = new LiubaiProcessContext();
        context.setOrderItem(item);
        context.setProcedureFlow(flow);
        context.setProductionPiece(piece);
        double thickness = Math.min(10, available);
        var assets = uploadLiubaiTagAssets(context, piece.getProductionPieceId(), item.getManufacturerId(),
                width - 4, height - 4, thickness);
        var margins = new ExpandMargins(m.left(), m.right(), m.top(), m.bottom());
        // 先平移至黑边内侧，标签靠近外扩后矩形的最外侧边，但不覆盖2mm黑线。
        svg.append("<g transform=\"translate(").append(format(m.left() + 2)).append(" ")
                .append(format(m.top() + 2)).append(")\">")
                .append(buildLiubaiTagGroups(piece.getId(), assets,
                        width - 4 - m.left() - m.right(), height - 4 - m.top() - m.bottom(), margins))
                .append("</g>");
        updateTagMarks(piece, assets, margins);
    }

}
