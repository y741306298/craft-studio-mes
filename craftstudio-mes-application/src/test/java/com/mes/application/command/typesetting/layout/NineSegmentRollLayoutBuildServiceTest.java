package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NineSegmentRollLayoutBuildServiceTest {
    @Test
    void buildsEdgeMarksAndSkipsAnyRectangleExtendingBelowBoard() throws Exception {
        FormeBuildContext context = context("200", Arrays.asList(0D, 20D, 118D, 119D, 125D,
                20D, null, -1D, Double.NaN, Double.POSITIVE_INFINITY));
        OssTagUploadService upload = mock(OssTagUploadService.class);
        when(upload.uploadTagPng(eq("roll"), any(byte[].class), eq("mark")))
                .thenAnswer(invocation -> {
                    var png = ImageIO.read(new ByteArrayInputStream(invocation.getArgument(1)));
                    assertThat(png.getWidth()).isIn(10, 2);
                    assertThat(png.getHeight()).isEqualTo(png.getWidth() == 10 ? 7 : 200);
                    assertThat(png.getRGB(0, 0) & 0xffffff).isZero();
                    assertThat(png.getRGB(png.getWidth() - 1, png.getHeight() - 1) & 0xffffff).isZero();
                    return png.getWidth() == 10 ? "a.png" : "b.png";
                });
        FormeLayoutBuildResult result = new NineSegmentRollLayoutBuildService(upload).build(context);

        assertThat(result.getMargin().getLeft()).isEqualTo(12);
        assertThat(result.getMargin().getRight()).isEqualTo(14);
        assertThat(result.getMargin().getTop()).isZero();
        assertThat(result.getMargin().getBottom()).isZero();
        assertThat(result.getMarks()).hasSize(7);
        FormeGenerationRequest.Mark b = result.getMarks().get(0);
        assertThat(b.getImg()).isEqualTo("b.png");
        assertThat(b.getPosition().getX()).isEqualTo(1012);
        assertThat(b.getPosition().getY()).isZero();
        assertThat(b.getSize().getWidth()).isEqualByComparingTo("2");
        assertThat(b.getSize().getHeight()).isEqualByComparingTo("200");
        assertThat(result.getMarks().subList(1, 7)).extracting(m -> m.getPosition().getX())
                .containsExactly(0, 1016, 0, 1016, 0, 1016);
        assertThat(result.getMarks().subList(1, 7)).extracting(m -> m.getPosition().getY())
                .containsExactly(75, 75, 95, 95, 193, 193);
        assertThat(result.getMarks().subList(1, 7)).allSatisfy(m -> {
            assertThat(m.getSize().getWidth()).isEqualByComparingTo("10");
            assertThat(m.getSize().getHeight()).isEqualByComparingTo("7");
        });
        assertThat(context.getTypesettingInfo().getMarks()).containsEntry("elementA", "a.png")
                .containsEntry("elementB", "b.png");
        assertThat(result.getAnchorPoints()).isEmpty();
        assertThat(result.getOutputs().getJson()).isNotNull();
        assertThat(result.getOutputs().getFormeSvg()).isNotNull();
        assertThat(result.getOutputs().getPlt()).isNull();
    }

    @Test
    void addsZeroWithoutGridLinesAndHonorsHeightBoundary() {
        OssTagUploadService upload = mock(OssTagUploadService.class);
        NineSegmentRollLayoutBuildService service = new NineSegmentRollLayoutBuildService(upload);
        assertThat(service.build(context("82", null)).getMarks()).hasSize(3);
        assertThat(service.build(context("81.9", null)).getMarks()).hasSize(1);
        assertThat(service.build(context("74", null)).getMarks()).hasSize(1);
        FormeBuildContext withoutInfo = context("100", null);
        withoutInfo.setTypesettingInfo(null);
        assertThat(service.build(withoutInfo).getMarks()).hasSize(3);
        assertThat(TypesettingLayoutMode.fromCode("xy_cutting_aux_line_nine_segment_roll"))
                .isEqualTo(service.supportMode());
        assertThat(service.supportMode().getLayoutCategory()).isEqualTo("vertical_typesetting");
        assertThat(service.supportMode().isQueryable()).isTrue();
    }

    private FormeBuildContext context(String height, List<Double> ys) {
        TypesettingElement element = new TypesettingElement();
        if (ys != null) {
            element.setGridLines(new TypesettingElement.GridLines(List.of(), ys));
        }
        TypesettingInfo info = new TypesettingInfo();
        info.setElement(element);
        FormeBuildContext context = new FormeBuildContext();
        context.setBusinessId("roll");
        context.setNestedWidth(new BigDecimal("1000"));
        context.setNestedHeight(new BigDecimal(height));
        context.setTypesettingInfo(info);
        return context;
    }
}
