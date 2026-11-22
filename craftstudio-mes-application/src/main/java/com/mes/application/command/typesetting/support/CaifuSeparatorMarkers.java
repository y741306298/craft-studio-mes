package com.mes.application.command.typesetting.support;

import com.mes.application.command.api.req.NestingRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class CaifuSeparatorMarkers {

    private static final String MARK_PREFIX = "caifuMarker_";
    private static final Pattern MARKER_ID = Pattern.compile("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");

    private CaifuSeparatorMarkers() {
    }

    public static Map<String, String> collect(NestingRequest request) {
        if (request == null || request.getNestManifest() == null || request.getNestManifest().getElements() == null) {
            return Map.of();
        }
        Map<String, String> markers = new LinkedHashMap<>();
        for (NestingRequest.Element element : request.getNestManifest().getElements()) {
            if (element instanceof SeparatorElement marker
                    && StringUtils.isNotBlank(marker.getId()) && StringUtils.isNotBlank(marker.getImg())) {
                markers.put(MARK_PREFIX + marker.getId(), marker.getImg());
            }
        }
        return markers;
    }

    public static Set<String> groupIds(Map<String, String> marks) {
        Set<String> ids = new LinkedHashSet<>();
        if (marks == null) {
            return ids;
        }
        marks.forEach((key, image) -> {
            if (key == null || !key.startsWith(MARK_PREFIX)) {
                return;
            }
            String id = key.substring(MARK_PREFIX.length());
            if (!MARKER_ID.matcher(id).matches()) {
                // Older records numbered their keys and used the PNG filename as the SVG group ID.
                String path = StringUtils.substringBefore(StringUtils.substringBefore(StringUtils.trimToEmpty(image), '?'), '#');
                id = StringUtils.substringBeforeLast(path.substring(path.lastIndexOf('/') + 1), ".");
            }
            if (StringUtils.isNotBlank(id)) {
                ids.add(id);
            }
        });
        return ids;
    }

    /** Identifies a generated separator in MES and serializes as a regular nesting element. */
    public static final class SeparatorElement extends NestingRequest.Element {
    }
}
