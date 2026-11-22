package com.mes.application.command.typesetting.layout;

import com.mes.application.command.graphics.SvgResourceReader;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.support.CaifuSeparatorMarkers;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.apache.commons.lang3.StringUtils;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CaifuMarkerBands {

    private static final double DEFAULT_HEIGHT_MM = 5;

    private CaifuMarkerBands() {
    }

    static List<Band> read(TypesettingInfo typesetting) {
        if (typesetting == null || typesetting.getElement() == null
                || StringUtils.isBlank(typesetting.getElement().getNestedSvg())) {
            return List.of();
        }
        Set<String> markerIds = CaifuSeparatorMarkers.groupIds(typesetting.getMarks());
        Set<String> sourceIds = new HashSet<>();
        if (typesetting.getTypesettingCells() != null) {
            for (TypesettingSourceCell cell : typesetting.getTypesettingCells()) {
                if (cell != null && TypesettingSourceType.TYPESETTING.getCode().equalsIgnoreCase(cell.getSourceType())) {
                    sourceIds.add(cell.getSourceId());
                }
            }
        }
        try {
            return SvgResourceReader.scanXml(URI.create(typesetting.getElement().getNestedSvg()),
                    xml -> read(xml, markerIds, sourceIds));
        } catch (RuntimeException failure) {
            throw new IllegalStateException("无法读取裁赋开背分隔位置，印版=" + typesetting.getTypesettingId(), failure);
        }
    }

    private static List<Band> read(XMLStreamReader xml, Set<String> markerIds, Set<String> sourceIds)
            throws XMLStreamException {
        List<Group> topLevelGroups = new ArrayList<>();
        Group firstGroup = null;
        int depth = 0;
        while (xml.hasNext()) {
            int event = xml.next();
            if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }
            depth++;
            if ((depth != 2 && firstGroup != null) || !"g".equalsIgnoreCase(xml.getLocalName())) {
                continue;
            }
            String id = xml.getAttributeValue(null, "id");
            if (StringUtils.isBlank(id) || "forme-base".equals(id)) {
                continue;
            }
            boolean separator = markerIds.contains(id);
            boolean plate = !separator && (sourceIds.contains(id)
                    || "true".equalsIgnoreCase(xml.getAttributeValue(null, "data-forme"))
                    || StringUtils.endsWithIgnoreCase(xml.getAttributeValue(null, "data-source-name"), ".svg"));
            Group group = new Group(id, firstFinite(number(xml.getAttributeValue(null, "data-cell-y")),
                    translateY(xml.getAttributeValue(null, "transform")), number(xml.getAttributeValue(null, "y"))),
                    firstFinite(number(xml.getAttributeValue(null, "data-cell-height")),
                            number(xml.getAttributeValue(null, "height"))), separator, plate);
            if (firstGroup == null && !separator) {
                firstGroup = group;
            }
            if (depth == 2) {
                topLevelGroups.add(group);
            }
        }

        List<Band> bands = new ArrayList<>();
        Group firstPlate = topLevelGroups.stream().filter(Group::plate).findFirst().orElse(firstGroup);
        if (firstPlate != null) {
            bands.add(new Band("ys0", 0, firstFinite(firstPlate.height(), DEFAULT_HEIGHT_MM), firstPlate.id()));
        }
        for (int i = 0; i < topLevelGroups.size(); i++) {
            Group line = topLevelGroups.get(i);
            if (!line.separator() || !Double.isFinite(line.y()) || !Double.isFinite(line.height())) {
                continue;
            }
            Group plate = findFollowingPlate(topLevelGroups, i + 1);
            bands.add(new Band(line.id(), line.y() + line.height() / 2,
                    plate == null ? line.height() : firstFinite(plate.height(), line.height()),
                    plate == null ? null : plate.id()));
        }
        return bands;
    }

    private static Group findFollowingPlate(List<Group> groups, int start) {
        for (int i = start; i < groups.size(); i++) {
            Group group = groups.get(i);
            if (group.plate()) {
                return group;
            }
            if (group.separator()) {
                return null;
            }
        }
        return null;
    }

    private static double translateY(String transform) {
        if (StringUtils.isBlank(transform)) {
            return Double.NaN;
        }
        int start = transform.indexOf('(');
        int end = transform.indexOf(')');
        if (start < 0 || end <= start) {
            return Double.NaN;
        }
        String[] parts = transform.substring(start + 1, end).trim().split("[\\s,]+");
        String operation = transform.substring(0, start).trim();
        if ("matrix".equalsIgnoreCase(operation) && parts.length == 6) {
            return number(parts[5]);
        }
        if ("translate".equalsIgnoreCase(operation) && parts.length <= 2) {
            return parts.length == 1 ? 0 : number(parts[1]);
        }
        return Double.NaN;
    }

    private static double number(String value) {
        if (StringUtils.isBlank(value)) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private static double firstFinite(double... values) {
        for (double value : values) {
            if (Double.isFinite(value)) {
                return value;
            }
        }
        return Double.NaN;
    }

    private record Group(String id, double y, double height, boolean separator, boolean plate) {
    }

    record Band(String id, double centerY, double height, String relatedTypesettingId) {
    }
}
