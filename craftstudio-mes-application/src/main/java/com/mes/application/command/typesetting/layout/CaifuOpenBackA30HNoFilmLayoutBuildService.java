package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CaifuOpenBackA30HNoFilmLayoutBuildService extends CaifuLayoutBuildService {
    private static final int EXPAND_TOP_MM = 3;
    private static final int EXPAND_LEFT_MM = 11;

    private static final int ELEMENT_A_WIDTH_MM = 3;
    private static final int ELEMENT_B_WIDTH_MM = 8;
    private static final int ELEMENT_B_HEIGHT_MM = 3;
    private static final int ELEMENT_B_OFFSET_Y_MM = 295;
    private static final int ELEMENT_B_X_MM = 3;
    private static final int ELEMENT_D_WIDTH_TENTH_MM = 3;
    private static final int ELEMENT_D_HEIGHT_MM = 5;
    private static final int ELEMENT_D_OFFSET_Y_MM = 8;
    private static final int ELEMENT_D_OFFSET_RIGHT_ZERO_TENTH_MM = 5;
    private static final int ELEMENT_D_OFFSET_RIGHT_ONE_TENTH_MM = 203;
    private static final int ELEMENT_D_OFFSET_RIGHT_TWO_TENTH_MM = 303;

    private final TypesettingService typesettingService;

    public CaifuOpenBackA30HNoFilmLayoutBuildService(OssTagUploadService ossTagUploadService,
                                                     TypesettingService typesettingService) {
        super(ossTagUploadService);
        this.typesettingService = typesettingService;
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_NO_FILM;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        int originalWidth = context.getNestedWidth().intValue();
        int originalHeight = context.getNestedHeight().intValue();
        int expandedHeight = originalHeight + EXPAND_TOP_MM;
        int expandedWidth = originalWidth + EXPAND_LEFT_MM;

        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(EXPAND_LEFT_MM);
        margin.setTop(EXPAND_TOP_MM);
        margin.setRight(0);
        margin.setBottom(0);
        result.setMargin(margin);

        String tagUploadSubDir = buildTagUploadSubDir(context);
        String elementA = ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_A_WIDTH_MM, expandedHeight), tagUploadSubDir);
        String elementB = ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM), tagUploadSubDir);
        String elementD = ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_D_WIDTH_TENTH_MM / 10.0, ELEMENT_D_HEIGHT_MM), tagUploadSubDir);

        List<MarkerBand> bands = extractMarkerBands(context);
        List<MarkerBand> orderedBands = new ArrayList<>();
        MarkerBand zeroBand = extractZeroBand(context);
        if (zeroBand != null) {
            orderedBands.add(zeroBand);
        }
        orderedBands.addAll(bands);
        MarkerBand bottomBand = new MarkerBand("ysBottom", expandedHeight, ELEMENT_D_HEIGHT_MM, null);
        orderedBands.add(bottomBand);
        boolean hasBloodBand = orderedBands.stream().anyMatch(this::isBloodBand);

        Map<Integer, String> elementEByHeight = new HashMap<>();
        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();
        marks.add(createMark(elementA, ELEMENT_A_WIDTH_MM, expandedHeight, 0, 0));

        LinkedHashSet<Integer> placedElementBYs = new LinkedHashSet<>();
        for (MarkerBand band : orderedBands) {
            if (band == null) {
                continue;
            }
            int elementBY = (int) Math.round(band.centerY - ELEMENT_B_OFFSET_Y_MM);
            if (elementBY < 0 || elementBY > expandedHeight || !placedElementBYs.add(elementBY)) {
                continue;
            }
            marks.add(createMark(elementB, ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM, ELEMENT_B_X_MM, elementBY));
        }

        for (MarkerBand band : orderedBands) {
            if (band == null || "ysBottom".equals(band.id)) {
                continue;
            }
            double lineY = band.centerY + ELEMENT_D_OFFSET_Y_MM;
            if (lineY > expandedHeight) {
                continue;
            }
            if (!hasBloodBand) {
                continue;
            }
            boolean isBlood = isBloodBand(band);
            String lineImg = elementD;
            double lineHeight = ELEMENT_D_HEIGHT_MM;
            if (!isBlood) {
                int svgHeight = (int) Math.max(1, Math.round(band.height) - 30);
                lineImg = elementEByHeight.computeIfAbsent(svgHeight,
                        h -> ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_D_WIDTH_TENTH_MM / 10.0, h), tagUploadSubDir));
                lineHeight = svgHeight;
            }
            if (!isBlood) {
                marks.add(createMark(lineImg, ELEMENT_D_WIDTH_TENTH_MM / 10.0, lineHeight,
                        expandedWidth - (ELEMENT_D_OFFSET_RIGHT_ZERO_TENTH_MM / 10.0), lineY));
            }
            marks.add(createMark(lineImg, ELEMENT_D_WIDTH_TENTH_MM / 10.0, lineHeight,
                    expandedWidth - (ELEMENT_D_OFFSET_RIGHT_ONE_TENTH_MM / 10.0), lineY));
            marks.add(createMark(lineImg, ELEMENT_D_WIDTH_TENTH_MM / 10.0, lineHeight,
                    expandedWidth - (ELEMENT_D_OFFSET_RIGHT_TWO_TENTH_MM / 10.0), lineY));
        }

        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> markFiles = new LinkedHashMap<>();
            markFiles.put("elementA", elementA);
            markFiles.put("elementB", elementB);
            markFiles.put("elementD", elementD);
            if (!elementEByHeight.isEmpty()) {
                markFiles.put("elementE", elementEByHeight.values().iterator().next());
            }
            context.getTypesettingInfo().setMarks(markFiles);
        }

        result.setMarks(marks);
        result.setAnchorPoints(Collections.emptyList());
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }

    private List<MarkerBand> extractMarkerBands(FormeBuildContext context) {
        List<MarkerBand> bands = new ArrayList<>();
        if (context.getTypesettingInfo() == null || context.getTypesettingInfo().getMarks() == null) {
            return bands;
        }
        Set<String> markerIds = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : context.getTypesettingInfo().getMarks().entrySet()) {
            if (entry == null || !StringUtils.startsWith(entry.getKey(), "caifuMarker_")) {
                continue;
            }
            String fileName = extractFileName(entry.getValue());
            int dotIdx = StringUtils.defaultString(fileName).lastIndexOf('.');
            String id = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
            if (StringUtils.isNotBlank(id)) {
                markerIds.add(id);
            }
        }
        if (markerIds.isEmpty()) {
            return bands;
        }
        String nestedSvg = context.getTypesettingInfo().getElement() == null ? null : context.getTypesettingInfo().getElement().getNestedSvg();
        if (StringUtils.isBlank(nestedSvg)) {
            return bands;
        }
        try {
            byte[] bytes = URI.create(nestedSvg).toURL().openStream().readAllBytes();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
            Element root = document.getDocumentElement();
            if (root == null) {
                return bands;
            }
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (!(children.item(i) instanceof Element)) {
                    continue;
                }
                Element element = (Element) children.item(i);
                if (!"g".equalsIgnoreCase(element.getTagName())) {
                    continue;
                }
                String markerId = element.getAttribute("id");
                if (!markerIds.contains(markerId)) {
                    continue;
                }
                double y = firstValid(parseDouble(element.getAttribute("data-cell-y")), parseTranslateY(element.getAttribute("transform")), parseDouble(element.getAttribute("y")));
                double markerHeight = firstValid(parseDouble(element.getAttribute("data-cell-height")), parseDouble(element.getAttribute("height")));
                if (Double.isNaN(y) || Double.isNaN(markerHeight)) {
                    continue;
                }
                String relatedId = resolveRelatedTypesettingId(element, markerId);
                double relatedHeight = resolveGroupHeightById(root, relatedId);
                double bandHeight = Double.isNaN(relatedHeight) ? markerHeight : relatedHeight;
                bands.add(new MarkerBand(markerId, y + markerHeight / 2.0, bandHeight, relatedId));
            }
        } catch (Exception ignored) {
            return bands;
        }
        return bands;
    }

    private MarkerBand extractZeroBand(FormeBuildContext context) {
        if (context.getTypesettingInfo() == null || context.getTypesettingInfo().getElement() == null || StringUtils.isBlank(context.getTypesettingInfo().getElement().getNestedSvg())) {
            return null;
        }
        try {
            byte[] bytes = URI.create(context.getTypesettingInfo().getElement().getNestedSvg()).toURL().openStream().readAllBytes();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
            NodeList groups = document.getElementsByTagName("g");
            for (int i = 0; i < groups.getLength(); i++) {
                Element g = (Element) groups.item(i);
                String id = g.getAttribute("id");
                if (StringUtils.isBlank(id) || StringUtils.equals(id, "forme-base")) {
                    continue;
                }
                String relatedId = resolveRelatedTypesettingId(g, id);
                double h = firstValid(parseDouble(g.getAttribute("data-cell-height")), parseDouble(g.getAttribute("height")), ELEMENT_D_HEIGHT_MM);
                return new MarkerBand("ys0", 0D, h, relatedId);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private boolean isBloodBand(MarkerBand band) {
        if (band == null || StringUtils.isBlank(band.relatedTypesettingId)) {
            return false;
        }
        TypesettingInfo info = typesettingService.findById(band.relatedTypesettingId);
        if (info != null) {
            return Boolean.TRUE.equals(info.getHaveBlood());
        }
        List<TypesettingInfo> infos = typesettingService.findTypesettingListByTypesettingId(band.relatedTypesettingId);
        return infos != null && !infos.isEmpty() && infos.get(0) != null && Boolean.TRUE.equals(infos.get(0).getHaveBlood());
    }

    private TypesettingInfo findTypesettingInfoById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        TypesettingInfo infoById = typesettingService.findById(id);
        if (infoById != null) {
            return infoById;
        }
        List<TypesettingInfo> infos = typesettingService.findTypesettingListByTypesettingId(id);
        if (infos != null && !infos.isEmpty()) {
            return infos.get(0);
        }
        return null;
    }

    private double resolveGroupHeightById(Element root, String groupId) {
        if (root == null || StringUtils.isBlank(groupId)) {
            return Double.NaN;
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) {
                continue;
            }
            Element child = (Element) children.item(i);
            if (!"g".equalsIgnoreCase(child.getTagName())) {
                continue;
            }
            if (!StringUtils.equals(groupId, child.getAttribute("id"))) {
                continue;
            }
            return firstValid(parseDouble(child.getAttribute("data-cell-height")), parseDouble(child.getAttribute("height")));
        }
        return Double.NaN;
    }

    private String resolveRelatedTypesettingId(Element markerElement, String fallbackId) {
        if (markerElement == null) {
            return fallbackId;
        }
        Element root = markerElement.getOwnerDocument() == null ? null : markerElement.getOwnerDocument().getDocumentElement();
        if (root == null) {
            return fallbackId;
        }
        List<Element> topLevelGroups = new ArrayList<>();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) {
                continue;
            }
            Element child = (Element) children.item(i);
            if ("g".equalsIgnoreCase(child.getTagName())) {
                topLevelGroups.add(child);
            }
        }
        if (Math.abs(firstValid(parseDouble(markerElement.getAttribute("data-cell-y")), parseTranslateY(markerElement.getAttribute("transform")))) < 0.0001) {
            for (Element g : topLevelGroups) {
                if (isPlateGroup(g)) {
                    return g.getAttribute("id");
                }
            }
        }
        for (int i = 0; i < topLevelGroups.size(); i++) {
            Element g = topLevelGroups.get(i);
            if (g != markerElement) {
                continue;
            }
            for (int j = i + 1; j < topLevelGroups.size(); j++) {
                Element next = topLevelGroups.get(j);
                if (isPlateGroup(next)) {
                    return next.getAttribute("id");
                }
            }
            break;
        }
        return fallbackId;
    }

    private boolean isPlateGroup(Element g) {
        if (g == null) {
            return false;
        }
        String id = g.getAttribute("id");
        if (StringUtils.isBlank(id) || StringUtils.equals(id, "forme-base")) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(g.getAttribute("data-forme"), "true")
                || StringUtils.endsWithIgnoreCase(g.getAttribute("data-source-name"), ".svg");
    }

    private double parseTranslateY(String transform) {
        if (StringUtils.isBlank(transform)) {
            return Double.NaN;
        }
        int l = transform.indexOf('(');
        int r = transform.indexOf(')');
        if (l < 0 || r <= l) {
            return Double.NaN;
        }
        String[] parts = transform.substring(l + 1, r).trim().split("\\s+|,");
        if (parts.length == 6) {
            return parseDouble(parts[5]);
        }
        return Double.NaN;
    }

    private double firstValid(double... values) {
        for (double value : values) {
            if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                return value;
            }
        }
        return Double.NaN;
    }

    private String extractFileName(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    protected FormeGenerationRequest.Mark createMark(String img, double width, double height, double x, double y) {
        FormeGenerationRequest.Mark mark = new FormeGenerationRequest.Mark();
        mark.setImg(img);
        mark.setSize(createSize(java.math.BigDecimal.valueOf(width), java.math.BigDecimal.valueOf(height)));
        mark.setPosition(createPosition((int) Math.max(0, Math.round(x)), (int) Math.max(0, Math.round(y))));
        return mark;
    }

    private byte[] createBlackPng(double width, double height) {
        try {
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage((int) Math.ceil(width), (int) Math.ceil(height), java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = image.createGraphics();
            g.setColor(java.awt.Color.BLACK);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.dispose();
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成黑色 PNG 失败", e);
        }
    }

    private static class MarkerBand {
        private final String id;
        private final double centerY;
        private final double height;
        private final String relatedTypesettingId;

        private MarkerBand(String id, double centerY, double height, String relatedTypesettingId) {
            this.id = id;
            this.centerY = centerY;
            this.height = height;
            this.relatedTypesettingId = relatedTypesettingId;
        }
    }
}
