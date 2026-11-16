package com.mes.application.command.graphics;

import java.util.Map;

public record RemotePieceGraphics(String maskUrl, Map<String, String> assets) {
    public RemotePieceGraphics { assets = Map.copyOf(assets); }
    public String asset(String key) {
        String url = assets.get(key);
        if (url == null || url.isBlank()) throw new IllegalStateException("零件图形结果缺少素材: " + key);
        return url;
    }
}
