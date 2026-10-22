package com.mes.application.command.typesetting.proces.reshenghua;

import com.mes.application.command.typesetting.proces.liubai.AbstractFixedLiubaiProcessStrategy;
import com.mes.application.command.typesetting.proces.liubai.LiubaiProcessContext;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 热升华策略：从“缝管套/道旗缝边”后的直径和方向决定外扩量。
 * 独立于留白策略调度，支持异形轮廓；所有直径、方向、几何数据仅在当前调用保存。
 */
@Service
public class ReshenghuaProcessStrategy extends AbstractFixedLiubaiProcessStrategy {
    private final OssTagUploadService uploadService;

    public ReshenghuaProcessStrategy(RestTemplate restTemplate, OssTagUploadService uploadService) {
        super(restTemplate, uploadService);
        this.uploadService = uploadService;
    }

    @Override
    public boolean matches(LiubaiProcessContext context) {
        // 防止被 LiubaiProcessService 的首个命中规则抢先执行，热升华在预处理末尾独立调度。
        return context instanceof ReshenghuaProcessContext && context.getProcedureFlow() != null
                && context.getProcedureFlow().getNodes() != null
                && context.getProcedureFlow().getNodes().stream().anyMatch(n -> n != null && SublimationSewingSpec.isSewingNode(n.getNodeName()));
    }

    @Override
    public void process(LiubaiProcessContext context) {
        if (!matches(context)) return;
        ProductionPiece piece = context.getProductionPiece();
        if (piece == null || piece.getMaskImageFile() == null || piece.getMaskImageFile().getRawFile() == null) return;
        List<SublimationSewingSpec> specs = SublimationSewingSpec.parse(orderedNodeNames(context.getProcedureFlow()));
        Map<SewingDirection.Edge, Double> distances = SublimationSewingSpec.margins(specs);
        String ref = piece.getMaskImageFile().getRawFile();
        String originalSvg = resolveSvg(ref);
        if (originalSvg == null || originalSvg.isBlank() || originalSvg.contains("id=\"reshenghua-outline-")) return;
        for (SublimationSewingSpec spec : specs) processColor(context, spec.color());

        SublimationSvg svg = new SublimationSvg(originalSvg, piece.getWidth(), piece.getHeight());
        Rectangle2D originalBounds = svg.bounds();
        Area strips = svg.extension(distances);
        Area outline = SublimationSvg.exterior(svg.expanded(strips));
        Rectangle2D newBounds = outline.getBounds2D();
        String pieceId = ensureProductionPieceMongoId(piece);
        String businessId = ensureProductionPieceBusinessId(piece);
        String manufacturer = piece.getManufacturerId() == null ? "default" : piece.getManufacturerId();
        String markPath = "mark/" + manufacturer + "/" + businessId + "/";
        String outlineUrl = uploadService.uploadTagPng(businessId, outlinePng(outline), markPath);
        ExpandMargins margins = new ExpandMargins(distances.getOrDefault(SewingDirection.Edge.LEFT, 0D),
                distances.getOrDefault(SewingDirection.Edge.RIGHT, 0D), distances.getOrDefault(SewingDirection.Edge.TOP, 0D),
                distances.getOrDefault(SewingDirection.Edge.BOTTOM, 0D));
        double tagThickness = Math.min(10D, distances.values().stream().mapToDouble(Double::doubleValue).min().orElse(10D));
        LiubaiTagAssets tags = uploadLiubaiTagAssets(context, businessId, manufacturer, newBounds.getWidth(), newBounds.getHeight(), tagThickness);
        String tagGroups = "<g transform=\"translate(" + SublimationSvg.number(originalBounds.getX()) + " "
                + SublimationSvg.number(originalBounds.getY()) + ")\">"
                + buildLiubaiTagGroups(pieceId, tags, originalBounds.getWidth(), originalBounds.getHeight(), margins) + "</g>";
        String rewritten = svg.rewrite(pieceId, resolveOriginalContentImg(piece, ref), strips, outline, outlineUrl, tagGroups);
        String itemId = context.getOrderItem() == null ? "default" : context.getOrderItem().getOrderItemId();
        String newUrl = uploadService.uploadTagSvg(businessId, rewritten.getBytes(StandardCharsets.UTF_8),
                "mask/" + manufacturer + "/" + itemId + "/reshenghua/");
        updateMaskImageFile(piece, newUrl);
        piece.setWidth(newBounds.getWidth());
        piece.setHeight(newBounds.getHeight());
        if (piece.getMarks() == null) piece.setMarks(new LinkedHashMap<>());
        piece.getMarks().put("reshenghua-outline", outlineUrl);
        updateTagMarks(piece, tags, margins);
    }

    /** 预留颜色处理扩展点，当前不改变颜色。 */
    protected void processColor(LiubaiProcessContext context, String color) {
    }

    private List<String> orderedNodeNames(ProcedureFlow flow) {
        List<ProcedureFlowNode> nodes = flow.getNodes();
        // 完整 nodeOrder 时按工艺顺序排序；旧数据缺少顺序时保留列表顺序，避免打乱相邻关系。
        var ordered = nodes.stream();
        if (nodes.stream().allMatch(n -> n != null && n.getNodeOrder() != null)) {
            ordered = ordered.sorted(Comparator.comparing(ProcedureFlowNode::getNodeOrder));
        }
        return ordered.map(n -> n == null ? null : n.getNodeName()).toList();
    }

    private byte[] outlinePng(Area outline) {
        Rectangle2D b = outline.getBounds2D();
        double scale = 36D / 25.4D;
        BufferedImage image = new BufferedImage(Math.max(1, (int) Math.ceil(b.getWidth() * scale)),
                Math.max(1, (int) Math.ceil(b.getHeight() * scale)), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(scale, scale);
            g.translate(-b.getX(), -b.getY());
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(outline);
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("热升华轮廓 PNG 生成失败", e);
        }
    }

    @Override protected String tagMarkKeyPrefix() { return "reshenghua-tag"; }
    @Override protected boolean matchesLiubaiValue(ProcedureFlow flow) { return false; }
    @Override protected String specName() { return "reshenghua"; }
    @Override protected double expandMm() { return 1D; } // 动态距离由当前调用的直径决定；正值使零外扩边不生成标签。
    @Override protected String[] matchKeywords() { return new String[0]; }
}
