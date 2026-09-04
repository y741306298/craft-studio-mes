package com.mes.application.command.typesetting.strategy.policy;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DoubleSideMountingManifestPolicyTest {

    @Test
    void shouldFillMirrorImageAndSvgForMarkedElement() {
        MirrorConfig mirrorConfig = new MirrorConfig();
        mirrorConfig.setImg("mirror.png");
        mirrorConfig.setSvg("mirror.svg");
        ProductionPiece piece = new ProductionPiece();
        piece.setId("piece-1");
        piece.setMirrorConfigs(List.of(mirrorConfig));

        NestingRequest.Element element = new NestingRequest.Element();
        element.setId("marked-nesting-piece-1");
        NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
        manifest.setElements(List.of(element));

        new DoubleSideMountingManifestPolicy().apply(manifest, List.of(piece), List.of());

        assertThat(element.getMirrorImg()).isEqualTo("mirror.png");
        assertThat(element.getMirrorSvg()).isEqualTo("mirror.svg");
        assertThat(manifest.getMirrorAppend()).isTrue();
        assertThat(manifest.getMirrorRequirePlt()).isFalse();
    }
}
