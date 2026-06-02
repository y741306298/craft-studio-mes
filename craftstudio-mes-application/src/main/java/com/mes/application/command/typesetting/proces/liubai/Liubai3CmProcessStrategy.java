package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Liubai3CmProcessStrategy extends AbstractLiubaiProcessStrategy {
    private static final double EXPAND_MM = 30D;
    private static final Pattern SVG_WIDTH_PATTERN = Pattern.compile("width\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_HEIGHT_PATTERN = Pattern.compile("height\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_INNER_PATTERN = Pattern.compile("<svg\\b[^>]*>([\\s\\S]*?)</svg>", Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;
    private final OssTagUploadService ossTagUploadService;

    public Liubai3CmProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        this.restTemplate = restTemplate;
        this.ossTagUploadService = ossTagUploadService;
    }

    @Override
    protected boolean matchesLiubaiValue(ProcedureFlow procedureFlow) {
        return containsInNodeOrParams(procedureFlow, "留白3cm")
                || containsInNodeOrParams(procedureFlow, "留白3CM")
                || containsInNodeOrParams(procedureFlow, "留白3厘米")
                || containsInNodeOrParams(procedureFlow, "留白30mm")
                || containsInNodeOrParams(procedureFlow, "留白30毫米")
                || containsInNodeOrParams(procedureFlow, "3cm")
                || containsInNodeOrParams(procedureFlow, "30mm")
                || containsInNodeOrParams(procedureFlow, "30毫米");
    }

    @Override
    public void process(LiubaiProcessContext context) {
        ProductionPiece piece = context.getProductionPiece();
        if (piece == null || piece.getMaskImageFile() == null || StringUtils.isBlank(piece.getMaskImageFile().getRawFile())) {
            return;
        }
        String originalMaskUrl = piece.getMaskImageFile().getRawFile();
        String originalSvg = resolveSvg(originalMaskUrl);
        if (StringUtils.isBlank(originalSvg)) {
            return;
        }
        double originalWidth = resolveDimension(originalSvg, SVG_WIDTH_PATTERN, piece.getWidth());
        double originalHeight = resolveDimension(originalSvg, SVG_HEIGHT_PATTERN, piece.getHeight());
        if (originalWidth <= 0 || originalHeight <= 0) {
            return;
        }

        ExpandMargins margins = resolveMargins(piece.getBlood(), context.isSkipBloodEdges());
        String expandedSvg = buildExpandedSvg(originalSvg, originalMaskUrl, piece, originalWidth, originalHeight, margins);
        String manufacturerMetaId = StringUtils.isBlank(piece.getManufacturerId()) ? "default" : piece.getManufacturerId();
        String businessId = StringUtils.isNotBlank(piece.getProductionPieceId()) ? piece.getProductionPieceId() : context.getOrderItem().getOrderItemId();
        String uploadPath = "mask/" + manufacturerMetaId + "/" + context.getOrderItem().getOrderItemId() + "/liubai/";
        String newMaskUrl = ossTagUploadService.uploadTagSvg(businessId, expandedSvg.getBytes(StandardCharsets.UTF_8), uploadPath);
        updateMaskImageFile(piece, newMaskUrl);
        piece.setWidth(originalWidth + margins.left + margins.right);
        piece.setHeight(originalHeight + margins.top + margins.bottom);
    }

    private ExpandMargins resolveMargins(Blood blood, boolean skipBloodEdges) {
        ExpandMargins margins = new ExpandMargins(EXPAND_MM, EXPAND_MM, EXPAND_MM, EXPAND_MM);
        if (!skipBloodEdges || blood == null) {
            return margins;
        }
        Integer x = blood.getX();
        Integer y = blood.getY();
        if (x != null && x > 0) {
            margins.right = 0D;
        } else if (x != null && x < 0) {
            margins.left = 0D;
        }
        if (y != null && y > 0) {
            margins.top = 0D;
        } else if (y != null && y < 0) {
            margins.bottom = 0D;
        }
        return margins;
    }

    private String resolveSvg(String svgRef) {
        String trimmed = svgRef.trim();
        if (trimmed.startsWith("<svg") || trimmed.startsWith("<?xml")) {
            return trimmed;
        }
        return restTemplate.getForObject(trimmed, String.class);
    }

    private double resolveDimension(String svg, Pattern pattern, Double fallback) {
        Matcher matcher = pattern.matcher(svg);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return fallback == null ? 0D : fallback;
    }

    private String buildExpandedSvg(String originalSvg,
                                    String originalMaskUrl,
                                    ProductionPiece piece,
                                    double originalWidth,
                                    double originalHeight,
                                    ExpandMargins margins) {
        double newWidth = originalWidth + margins.left + margins.right;
        double newHeight = originalHeight + margins.top + margins.bottom;
        String inner = extractInnerSvg(originalSvg);
        String productImg = piece.getProductImageFile() == null ? "" : piece.getProductImageFile().getRawFile();
        String sourceName = sourceName(originalMaskUrl);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + format(newWidth) + "\" height=\"" + format(newHeight)
                + "\" viewBox=\"0 0 " + format(newWidth) + " " + format(newHeight) + "\" version=\"1.1\" require-plt=\"true\">\n"
                + "<g id=\"liubai-" + escapeAttr(piece.getProductionPieceId()) + "\" img=\"" + escapeAttr(productImg)
                + "\" data-source-name=\"" + escapeAttr(sourceName) + "\" data-forme=\"false\" data-rotation=\"0\">\n"
                + "<path d=\"M0 0 H" + format(newWidth) + " V" + format(newHeight) + " H0 Z\" fill=\"#d1495b\" fill-opacity=\"0.82\" stroke=\"#111111\" stroke-width=\"1.23\" fill-rule=\"evenodd\" />\n"
                + "<g id=\"liubai-original\" transform=\"translate(" + format(margins.left) + " " + format(margins.top) + ")\">\n"
                + inner + "\n"
                + "</g>\n"
                + "</g></svg>";
    }

    private String extractInnerSvg(String svg) {
        String withoutXml = svg.replaceFirst("(?is)^\\s*<\\?xml[^>]*>\\s*", "");
        Matcher matcher = SVG_INNER_PATTERN.matcher(withoutXml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return withoutXml.trim();
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

    private String escapeAttr(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
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

    private static class ExpandMargins {
        private double left;
        private double right;
        private double top;
        private double bottom;

        private ExpandMargins(double left, double right, double top, double bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }
}
