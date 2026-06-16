package com.mes.application.command.typesetting.strategy.policy;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoubleSideMountingManifestPolicyTest {

    private final DoubleSideMountingManifestPolicy policy = new DoubleSideMountingManifestPolicy();

    @Test
    void applyFillsMirrorImgForMarkedNestingElementIds() {
        ProductionPiece piece = new ProductionPiece();
        piece.setId("piece-1");
        MirrorConfig mirrorConfig = new MirrorConfig();
        mirrorConfig.setImg("https://example.com/mirror.png");
        piece.setMirrorConfigs(Collections.singletonList(mirrorConfig));

        NestingRequest.Element element = new NestingRequest.Element();
        element.setId("marked-nesting-piece-1");
        NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
        manifest.setElements(new ArrayList<>(Collections.singletonList(element)));

        policy.apply(manifest, Collections.singletonList(piece), Collections.emptyList());

        assertEquals("https://example.com/mirror.png", element.getMirrorImg());
    }
}
