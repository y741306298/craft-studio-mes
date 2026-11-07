package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.typesetting.nesting.CaifuOpenBackA30ProNoFilmNestingComposeService;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CaifuOpenBackA30ProNoFilmLayoutBuildServiceTest {
    @TempDir
    Path tempDir;

    private OssTagUploadService uploads;
    private TypesettingService typesettings;
    private CaifuOpenBackA30ProNoFilmLayoutBuildService builder;
    private final Map<String, byte[]> pngs = new HashMap<>();

    @BeforeEach
    void setUp() {
        uploads = mock(OssTagUploadService.class);
        typesettings = mock(TypesettingService.class);
        when(uploads.uploadTagPng(anyString(), any(byte[].class), anyString())).thenAnswer(call -> {
            String url = "mark/" + pngs.size() + ".png";
            pngs.put(url, call.getArgument(1));
            return url;
        });
        builder = new CaifuOpenBackA30ProNoFilmLayoutBuildService(uploads, typesettings);
    }

    @Test
    void modeIsQueryableAndUsesOpenBackComposition() {
        TypesettingLayoutMode mode = TypesettingLayoutMode.fromCode("xy_cutting_aux_line_caifu_open_back_a30pro_no_film");
        assertEquals(mode, builder.supportMode());
        assertEquals("裁赋开背A30pro不覆膜", mode.getDescription());
        assertTrue(mode.isQueryable());
        assertEquals("vertical_typesetting", mode.getLayoutCategory());
        assertTrue(mode.isRequireJsonFile());
        assertTrue(mode.isRequireSvgFile());
        assertFalse(mode.isRequirePltFile());
        assertEquals("none", mode.getAnchorPointShape());
        CaifuOpenBackA30ProNoFilmNestingComposeService compose = new CaifuOpenBackA30ProNoFilmNestingComposeService(uploads);
        assertEquals(mode, compose.supportMode());
        assertEquals(0, compose.resolveSpacing(mode));
    }

    @Test
    void compositionKeepsAuxiliaryMarkersBetweenSourcePlates() {
        when(uploads.uploadTagPng(anyString(), any(byte[].class), anyString(), anyString()))
                .thenAnswer(call -> call.getArgument(2) + "/" + call.getArgument(3));
        when(uploads.uploadTagSvg(anyString(), any(byte[].class), anyString(), anyString()))
                .thenAnswer(call -> call.getArgument(2) + "/" + call.getArgument(3));
        NestingRequest.Element first = new NestingRequest.Element();
        NestingRequest.Element second = new NestingRequest.Element();
        NestingRequest.Container container = new NestingRequest.Container();
        container.setWidth(1000);
        List<NestingRequest.Element> elements = new CaifuOpenBackA30ProNoFilmNestingComposeService(uploads)
                .composeElements("factory", "test", List.of(first, second), List.of(container));
        assertEquals(3, elements.size());
        assertSame(first, elements.get(0));
        assertSame(second, elements.get(2));
        NestingRequest.Element marker = elements.get(1);
        assertEquals("left", marker.getHGravity());
        assertFalse(marker.getForme());
        assertEquals("mark/factory/caifu/" + marker.getId() + ".png", marker.getImg());
        assertEquals("mark/factory/caifu/" + marker.getId() + ".svg", marker.getSvg());
    }

    @Test
    void expandsThreeSidesAndUploadsBlackPngsWithoutExtraTagStrips() throws Exception {
        FormeBuildContext context = context(300);
        context.setElementAResolver(info -> { throw new AssertionError("A30pro 不生成标签条"); });
        FormeLayoutBuildResult result = builder.build(context);
        assertEquals(11, result.getMargin().getLeft());
        assertEquals(3, result.getMargin().getTop());
        assertEquals(3, result.getMargin().getRight());
        assertEquals(0, result.getMargin().getBottom());
        FormeGenerationRequest.Mark a = marks(context, result, "elementA").getFirst();
        assertMark(a, 3, 303, 0, 0);
        assertBlackPng(a.getImg(), 3, 303);
        String b = context.getTypesettingInfo().getMarks().get("elementB");
        assertBlackPng(b, 8, 3);
        assertFalse(context.getTypesettingInfo().getMarks().containsKey("elementF"));
        assertTrue(result.getAnchorPoints().isEmpty());
        assertNotNull(result.getOutputs().getJson());
        assertNotNull(result.getOutputs().getFormeSvg());
        assertNull(result.getOutputs().getPlt());
        assertEquals("forme/test/", result.getUploadPath());
    }

    @Test
    void bUsesTranslatedGridYsAndBottomOnBothSidesWithoutDuplicates() {
        FormeBuildContext context = context(400);
        context.getTypesettingInfo().getElement().setGridLines(new TypesettingElement.GridLines(
                List.of(), Arrays.asList(0D, 123D, 200D, 200D, 400D, null, Double.NaN, Double.POSITIVE_INFINITY)));
        FormeLayoutBuildResult result = builder.build(context);
        List<FormeGenerationRequest.Mark> b = marks(context, result, "elementB");
        assertEquals(List.of("3,0", "1011,0", "3,77", "1011,77", "3,277", "1011,277"), positions(b));
        b.forEach(mark -> assertEquals(BigDecimal.valueOf(8), mark.getSize().getWidth()));
    }

    @Test
    void bMustFitCompletelyWithinHeightAndStillUsesBottomWithoutGridLines() {
        FormeBuildContext context = context(300);
        context.getTypesettingInfo().getElement().setGridLines(new TypesettingElement.GridLines(
                null, List.of(122D, 423D, 424D, 1000D)));
        FormeLayoutBuildResult result = builder.build(context);
        assertEquals(List.of("3,300", "1011,300", "3,177", "1011,177"), positions(marks(context, result, "elementB")));

        context = context(300);
        result = builder.build(context);
        assertEquals(List.of("3,177", "1011,177"), positions(marks(context, result, "elementB")));

        context = context(100);
        result = builder.build(context);
        assertTrue(marks(context, result, "elementB").isEmpty());
    }

    @Test
    void dAndNonBloodLinesKeepOriginalRightEdgeAndCellBottoms() throws Exception {
        FormeBuildContext context = twoPlates(true, false);
        FormeLayoutBuildResult result = builder.build(context);
        List<FormeGenerationRequest.Mark> d = marks(context, result, "elementD");
        assertEquals(List.of("991,8", "981,8", "991,87", "981,87"), positions(d));
        d.forEach(mark -> assertEquals(new BigDecimal("0.3"), mark.getSize().getWidth()));
        List<FormeGenerationRequest.Mark> nonBlood = marks(context, result, "elementE");
        assertEquals(List.of("1011,111", "991,111", "981,111"), positions(nonBlood));
        nonBlood.forEach(mark -> assertEquals(0, BigDecimal.valueOf(90).compareTo(mark.getSize().getHeight())));
        // 扩展后右边界 1014；相对该边界的标记位置额外左移 3mm。
        assertEquals((int) Math.round(1014 - 20.3 - 3), d.getFirst().getPosition().getX());
    }

    @Test
    void eachBloodPlateUsesItsOwnBottomAndSupportsTypesettingIdLookup() throws Exception {
        FormeBuildContext context = twoPlates(true, true);
        TypesettingInfo second = new TypesettingInfo();
        second.setHaveBlood(true);
        when(typesettings.findById("plate-b")).thenReturn(null);
        when(typesettings.findTypesettingListByTypesettingId("plate-b")).thenReturn(List.of(second));
        FormeLayoutBuildResult result = builder.build(context);
        assertEquals(List.of("991,8", "981,8", "991,87", "981,87",
                        "991,111", "981,111", "991,210", "981,210"),
                positions(marks(context, result, "elementD")));
        assertFalse(context.getTypesettingInfo().getMarks().containsKey("elementE"));
    }

    @Test
    void allNonBloodPlatesFollowA30hRuleAndDoNotAddDOrELines() throws Exception {
        FormeBuildContext context = twoPlates(false, false);
        FormeLayoutBuildResult result = builder.build(context);
        assertTrue(marks(context, result, "elementD").isEmpty());
        assertFalse(context.getTypesettingInfo().getMarks().containsKey("elementE"));
    }

    @Test
    void a30hKeepsItsMarginsTagsAndSingleSided295mmBPlacement() throws Exception {
        FormeBuildContext context = twoPlates(true, false);
        FormeLayoutBuildResult result = new CaifuOpenBackA30HNoFilmLayoutBuildService(uploads, typesettings).build(context);
        assertEquals(11, result.getMargin().getLeft());
        assertEquals(23, result.getMargin().getTop());
        assertEquals(0, result.getMargin().getRight());
        assertEquals(20, result.getMargin().getBottom());
        assertMark(marks(context, result, "elementA").getFirst(), 3, 403, 0, 20);
        assertMark(marks(context, result, "elementF").getFirst(), 1000, 20, 11, 0);
        assertMark(marks(context, result, "elementFRotated").getFirst(), 1000, 20, 11, 423);
        assertEquals(List.of("3,128"), positions(marks(context, result, "elementB")));
        assertEquals(List.of("991,28", "981,28", "991,107", "981,107"), positions(marks(context, result, "elementD")));
        assertEquals(List.of("1011,131", "991,131", "981,131"), positions(marks(context, result, "elementE")));
    }

    private FormeBuildContext context(int height) {
        FormeBuildContext context = new FormeBuildContext();
        context.setBusinessId("test");
        context.setNestedWidth(BigDecimal.valueOf(1000));
        context.setNestedHeight(BigDecimal.valueOf(height));
        TypesettingInfo info = new TypesettingInfo();
        info.setElement(new TypesettingElement());
        context.setTypesettingInfo(info);
        return context;
    }

    private FormeBuildContext twoPlates(boolean firstBlood, boolean secondBlood) throws Exception {
        FormeBuildContext context = context(400);
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\">"
                + "<g id=\"plate-a\" data-forme=\"true\" data-cell-y=\"0\" data-cell-height=\"100\"/>"
                + "<g id=\"marker\" data-cell-y=\"100\" data-cell-height=\"6\"/>"
                + "<g id=\"plate-b\" data-forme=\"true\" data-cell-y=\"106\" data-cell-height=\"120\"/>"
                + "</svg>";
        Path file = tempDir.resolve("nested.svg");
        Files.writeString(file, svg);
        context.getTypesettingInfo().getElement().setNestedSvg(file.toUri().toString());
        context.getTypesettingInfo().setMarks(Collections.singletonMap("caifuMarker_1", "mark/marker.png"));
        TypesettingInfo first = new TypesettingInfo();
        first.setHaveBlood(firstBlood);
        TypesettingInfo second = new TypesettingInfo();
        second.setHaveBlood(secondBlood);
        when(typesettings.findById("plate-a")).thenReturn(first);
        when(typesettings.findById("plate-b")).thenReturn(second);
        return context;
    }

    private List<FormeGenerationRequest.Mark> marks(FormeBuildContext context, FormeLayoutBuildResult result, String key) {
        String image = context.getTypesettingInfo().getMarks().get(key);
        return result.getMarks().stream().filter(mark -> mark.getImg().equals(image)).collect(Collectors.toList());
    }

    private List<String> positions(List<FormeGenerationRequest.Mark> marks) {
        return marks.stream().map(mark -> mark.getPosition().getX() + "," + mark.getPosition().getY()).collect(Collectors.toList());
    }

    private void assertMark(FormeGenerationRequest.Mark mark, int width, int height, int x, int y) {
        assertEquals(BigDecimal.valueOf(width), mark.getSize().getWidth());
        assertEquals(BigDecimal.valueOf(height), mark.getSize().getHeight());
        assertEquals(x, mark.getPosition().getX());
        assertEquals(y, mark.getPosition().getY());
    }

    private void assertBlackPng(String image, int width, int height) throws Exception {
        BufferedImage png = ImageIO.read(new ByteArrayInputStream(pngs.get(image)));
        assertEquals(width, png.getWidth());
        assertEquals(height, png.getHeight());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                assertEquals(0, png.getRGB(x, y) & 0xffffff);
            }
        }
    }
}
