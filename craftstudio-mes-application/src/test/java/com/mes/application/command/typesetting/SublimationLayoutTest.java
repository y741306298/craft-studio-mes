package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.layout.LargeVisionLayoutBuildService;
import com.mes.application.command.typesetting.layout.FormeBuildContext;
import com.mes.application.command.typesetting.layout.FormeLayoutBuildResult;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SublimationLayoutTest {
    private final AppTypesettingService service = new AppTypesettingService();
    private final String largeVision = TypesettingLayoutMode.LARGE_VISION_LAYOUT.getCode();

    @Test
    void rejectsMixedPiecesAndMissingFlows() {
        assertNotEquals("PASS", validate(List.of(piece("热升华"), piece("打印")), largeVision));
        assertNotEquals("PASS", validate(List.of(piece("热升华"), new ProductionPiece()), largeVision));
    }

    @Test
    void sublimationRequiresExplicitLargeVisionMode() {
        List<ProductionPiece> pieces = List.of(piece("热升华"), piece("热升华（1.6米双面喷夹黑布）"));
        assertEquals("PASS", validate(pieces, largeVision));
        assertNotEquals("PASS", validate(pieces, null));
        for (TypesettingLayoutMode mode : TypesettingLayoutMode.values()) {
            if (mode != TypesettingLayoutMode.LARGE_VISION_LAYOUT) {
                assertNotEquals("PASS", validate(pieces, mode.getCode()));
            }
        }
    }

    @Test
    void ordinaryPiecesKeepExistingModeChoices() {
        assertEquals("PASS", validate(List.of(piece("打印")), null));
        assertEquals("PASS", validate(List.of(piece("打印")), largeVision));
    }

    @Test
    void sourcePlatesCannotBypassRestrictions() {
        TypesettingInfo plate = new TypesettingInfo();
        plate.setProcedureFlow(flow("打印"));
        assertNotEquals("PASS", service.validateSublimationLayout(List.of(piece("热升华")), List.of(plate), largeVision));
        plate.setProcedureFlow(flow("热升华"));
        assertEquals("PASS", service.validateSublimationLayout(List.of(piece("热升华")), List.of(plate), largeVision));
        assertNotEquals("PASS", service.validateSublimationLayout(List.of(), List.of(plate), null));
    }

    @Test
    void largeVisionUsesShapedPltQrWithFourMillimeterSpacing() {
        TypesettingLayoutMode mode = TypesettingLayoutMode.fromCode(largeVision);
        assertEquals("大视觉排版", mode.getDescription());
        assertEquals("shaped_typesetting", mode.getLayoutCategory());
        assertEquals("plt_qr", mode.getCodeGenerateType());
        assertEquals(4, mode.getNestingSpacingMm());
        assertTrue(mode.isQueryable());
        assertTrue(mode.isRequireJsonFile());
        assertTrue(mode.isRequirePltFile());
        assertTrue(mode.isRequireSvgFile());
        assertEquals(mode, new LargeVisionLayoutBuildService().supportMode());
    }

    @Test
    void largeVisionAddsNoMarksOrMarginsAndNeverRequestsQrResources() {
        FormeBuildContext context = new FormeBuildContext();
        context.setBusinessId("layout-test");
        context.setMarginHeight(java.math.BigDecimal.valueOf(30));
        context.setPlateNameSupplier(() -> { throw new AssertionError("不应申请队列编号"); });
        context.setPlateNameBBSupplier(() -> { throw new AssertionError("不应申请反面队列编号"); });
        context.setQrDataUriGenerator(value -> { throw new AssertionError("不应生成二维码"); });
        context.setElementAResolver(value -> { throw new AssertionError("不应生成标签"); });

        FormeLayoutBuildResult result = new LargeVisionLayoutBuildService().build(context);
        assertTrue(result.getMarks().isEmpty());
        assertTrue(result.getAnchorPoints().isEmpty());
        assertEquals(0, result.getMargin().getLeft());
        assertEquals(0, result.getMargin().getTop());
        assertEquals(0, result.getMargin().getRight());
        assertEquals(0, result.getMargin().getBottom());
        assertNotNull(result.getOutputs());
        assertEquals("none", TypesettingLayoutMode.LARGE_VISION_LAYOUT.getAnchorPointShape());
        assertNull(TypesettingLayoutMode.LARGE_VISION_LAYOUT.getTempCodeFormat());
    }

    private String validate(List<ProductionPiece> pieces, String mode) {
        return service.validateSublimationLayout(pieces, List.of(), mode);
    }

    private ProductionPiece piece(String name) {
        ProductionPiece piece = new ProductionPiece();
        piece.setProcedureFlow(flow(name));
        return piece;
    }

    private ProcedureFlow flow(String name) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(name);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(node));
        return flow;
    }
}
