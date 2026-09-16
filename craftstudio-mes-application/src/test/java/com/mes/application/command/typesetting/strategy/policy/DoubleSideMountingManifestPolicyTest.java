package com.mes.application.command.typesetting.strategy.policy;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DoubleSideMountingManifestPolicyTest {

    @Test
    void addsMirrorConfigKeyedByMongoIdForMarkedElement() {
        ProductionPiece piece = new ProductionPiece();
        piece.setId("mongo-piece-id");
        piece.setProductionPieceId("business-piece-id");
        MirrorConfig sourceMirrorConfig = new MirrorConfig();
        sourceMirrorConfig.setImg("https://example.com/mirror.tif");
        sourceMirrorConfig.setSvg("https://example.com/mirror.svg");
        piece.setMirrorConfigs(List.of(sourceMirrorConfig));

        NestingRequest.Element element = new NestingRequest.Element();
        element.setId("marked-nesting-mongo-piece-id");
        NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
        manifest.setElements(List.of(element));

        new DoubleSideMountingManifestPolicy().apply(manifest, List.of(piece), List.of());

        assertThat(element.getMirrorConfig()).containsOnlyKeys("mongo-piece-id");
        NestingRequest.ElementMirrorConfig mirrorConfig = element.getMirrorConfig().get("mongo-piece-id");
        assertThat(mirrorConfig.getImg()).isEqualTo("https://example.com/mirror.tif");
        assertThat(mirrorConfig.getRequirePlt()).isTrue();
    }
}
