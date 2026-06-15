package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Liubai5CmProcessStrategyTest {
    private final CapturingOssTagUploadService ossTagUploadService = new CapturingOssTagUploadService();
    private final Liubai5CmProcessStrategy strategy = new Liubai5CmProcessStrategy(null, ossTagUploadService);

    @Test
    void rebuildRouteAddsInnerOriginalGrayBorderToLiubaiGroup() {
        ProductionPiece piece = pieceWithInlineMask("<svg width=\"100\" height=\"80\" viewBox=\"0 0 100 80\"><rect width=\"100\" height=\"80\"/></svg>");

        strategy.process(context(piece, false));

        String uploadedSvg = ossTagUploadService.uploadedSvgText();
        assertTrue(uploadedSvg.contains("<g id=\"liubai-5cm-"));
        assertTrue(uploadedSvg.contains("<path d=\"M-50 -50 H150 V130 H-50 Z\" fill=\"#d1495b\" fill-opacity=\"0.82\" stroke=\"#111111\" stroke-width=\"1.722\""));
        assertTrue(uploadedSvg.contains("<path d=\"M0 0 H100 V80 H0 Z\" fill=\"none\" stroke=\"#808080\" stroke-width=\"1.722\""));
        assertTrue(uploadedSvg.indexOf("liubai-5cm-") < uploadedSvg.indexOf("data-forme=\"true\""));
        assertEquals(200D, piece.getWidth());
        assertEquals(180D, piece.getHeight());
    }

    @Test
    void spliceRouteAddsInnerOriginalGrayBorderToInsertedLiubaiGroup() {
        ProductionPiece piece = pieceWithInlineMask("<svg width=\"100\" height=\"80\" viewBox=\"0 0 100 80\"><g id=\"existing\"><path d=\"M0 0 H100 V80 H0 Z\"/></g></svg>");
        piece.setSeq(1);
        piece.setGroup("12#1-3");
        Blood blood = new Blood();
        blood.setX(-1);
        blood.setY(0);
        piece.setBlood(blood);

        strategy.process(context(piece, true));

        String uploadedSvg = ossTagUploadService.uploadedSvgText();
        assertTrue(uploadedSvg.contains("<path d=\"M0 -50 H100 V130 H0 Z\""));
        assertTrue(uploadedSvg.contains("<path d=\"M0 0 H100 V80 H0 Z\" fill=\"none\" stroke=\"#808080\""));
        assertTrue(uploadedSvg.indexOf("liubai-5cm-") < uploadedSvg.indexOf("<g id=\"existing\""));
        assertEquals(100D, piece.getWidth());
        assertEquals(180D, piece.getHeight());
    }

    @Test
    void allCentimeterLiubaiSpecsAddInnerOriginalGrayBorder() {
        CapturingOssTagUploadService twoCmOss = new CapturingOssTagUploadService();
        Liubai2CmProcessStrategy twoCmStrategy = new Liubai2CmProcessStrategy(null, twoCmOss);
        ProductionPiece piece = pieceWithInlineMask("<svg width=\"100\" height=\"80\" viewBox=\"0 0 100 80\"><rect width=\"100\" height=\"80\"/></svg>");

        twoCmStrategy.process(context(piece, false));

        String uploadedSvg = twoCmOss.uploadedSvgText();
        assertTrue(uploadedSvg.contains("<path d=\"M-20 -20 H120 V100 H-20 Z\""));
        assertTrue(uploadedSvg.contains("<path d=\"M0 0 H100 V80 H0 Z\" fill=\"none\" stroke=\"#808080\""));
        assertFalse(uploadedSvg.contains("stroke-dasharray"));
        assertEquals(140D, piece.getWidth());
        assertEquals(120D, piece.getHeight());
    }

    @Test
    void tenAndFifteenCentimeterLiubaiSpecsAddDashedFiveCentimeterInsetBorder() {
        CapturingOssTagUploadService tenCmOss = new CapturingOssTagUploadService();
        Liubai10CmProcessStrategy tenCmStrategy = new Liubai10CmProcessStrategy(null, tenCmOss);
        ProductionPiece tenCmPiece = pieceWithInlineMask("<svg width=\"100\" height=\"80\" viewBox=\"0 0 100 80\"><rect width=\"100\" height=\"80\"/></svg>");

        tenCmStrategy.process(context(tenCmPiece, false));

        String tenCmSvg = tenCmOss.uploadedSvgText();
        assertTrue(tenCmSvg.contains("<path d=\"M-100 -100 H200 V180 H-100 Z\""));
        assertTrue(tenCmSvg.contains("<path d=\"M-50 -50 H150 V130 H-50 Z\" fill=\"none\" stroke=\"#808080\" stroke-width=\"1.722\" stroke-dasharray=\"6 4\""));
        assertTrue(tenCmSvg.contains("<path d=\"M0 0 H100 V80 H0 Z\" fill=\"none\" stroke=\"#808080\""));

        CapturingOssTagUploadService fifteenCmOss = new CapturingOssTagUploadService();
        Liubai15CmProcessStrategy fifteenCmStrategy = new Liubai15CmProcessStrategy(null, fifteenCmOss);
        ProductionPiece fifteenCmPiece = pieceWithInlineMask("<svg width=\"100\" height=\"80\" viewBox=\"0 0 100 80\"><rect width=\"100\" height=\"80\"/></svg>");

        fifteenCmStrategy.process(context(fifteenCmPiece, false));

        String fifteenCmSvg = fifteenCmOss.uploadedSvgText();
        assertTrue(fifteenCmSvg.contains("<path d=\"M-150 -150 H250 V230 H-150 Z\""));
        assertTrue(fifteenCmSvg.contains("<path d=\"M-100 -100 H200 V180 H-100 Z\" fill=\"none\" stroke=\"#808080\" stroke-width=\"1.722\" stroke-dasharray=\"6 4\""));
        assertTrue(fifteenCmSvg.contains("<path d=\"M0 0 H100 V80 H0 Z\" fill=\"none\" stroke=\"#808080\""));
    }

    @Test
    void generatedPngForLargeLiubaiContainsDashedInsetAndOriginalBorders() throws Exception {
        CapturingOssTagUploadService tenCmOss = new CapturingOssTagUploadService();
        Liubai10CmProcessStrategy tenCmStrategy = new Liubai10CmProcessStrategy(null, tenCmOss);
        ProductionPiece piece = pieceWithInlineMask("<svg width=\"100\" height=\"80\" viewBox=\"0 0 100 80\"><rect width=\"100\" height=\"80\"/></svg>");

        tenCmStrategy.process(context(piece, false));

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(tenCmOss.uploadedPngBytes));
        assertNotNull(image);
        assertRgbClose(Color.BLACK, image.getRGB(0, 0));
        int dashedInset = convertMmToPixels(50D);
        assertRgbClose(Color.GRAY, image.getRGB(dashedInset, dashedInset));
        int originalInset = convertMmToPixels(100D);
        assertRgbClose(Color.GRAY, image.getRGB(originalInset, originalInset));
    }

    @Test
    void generatedPngContainsOuterBlackBorderAndInnerGrayOriginalBorder() throws Exception {
        ProductionPiece piece = pieceWithInlineMask("<svg width=\"100\" height=\"80\" viewBox=\"0 0 100 80\"><rect width=\"100\" height=\"80\"/></svg>");

        strategy.process(context(piece, false));

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(ossTagUploadService.uploadedPngBytes));
        assertNotNull(image);
        assertRgbClose(Color.BLACK, image.getRGB(0, 0));
        int innerLeft = convertMmToPixels(50D);
        int innerTop = convertMmToPixels(50D);
        assertRgbClose(Color.GRAY, image.getRGB(innerLeft, innerTop));
    }

    @Test
    void processAddsCombinedLiubaiPostProcessTagsOnOuterRectangleEdges() throws Exception {
        ProductionPiece piece = pieceWithInlineMask("<svg width=\"300\" height=\"280\" viewBox=\"0 0 300 280\"><rect width=\"300\" height=\"280\"/></svg>");
        LiubaiProcessContext context = context(piece, false);
        context.setProcedureFlow(procedureFlow("覆膜", "留白5cm", "穿钢丝绳", "粘边", "粘边"));

        strategy.process(context);

        String uploadedSvg = ossTagUploadService.uploadedSvgText();
        assertFalse(uploadedSvg.contains("data-tag-text"));
        assertTrue(uploadedSvg.contains("fill=\"#999999\""));
        assertFalse(uploadedSvg.contains("stroke=\"none\""));
        assertTrue(uploadedSvg.contains("id=\"liubai-tag-horizontal-top-left-"));
        assertTrue(uploadedSvg.contains("id=\"liubai-tag-vertical-left-top-"));
        assertTrue(uploadedSvg.contains("transform=\"translate(50 -50)\""));
        assertTrue(uploadedSvg.contains("transform=\"translate(-50 50)\""));
        assertEquals("https://example.test/mark-2.png", piece.getMarks().get("liubai-tag-horizontal"));
        assertEquals("https://example.test/mark-3.png", piece.getMarks().get("liubai-tag-vertical"));

        BufferedImage horizontal = ImageIO.read(new ByteArrayInputStream(ossTagUploadService.uploadedPngBytesList.get(1)));
        BufferedImage vertical = ImageIO.read(new ByteArrayInputStream(ossTagUploadService.uploadedPngBytesList.get(2)));
        assertNotNull(horizontal);
        assertNotNull(vertical);
        assertEquals(convertMmToPixels(10D, 300D), horizontal.getHeight());
        assertEquals(horizontal.getHeight(), vertical.getWidth());
        assertEquals(horizontal.getWidth(), vertical.getHeight());
    }

    private ProductionPiece pieceWithInlineMask(String svg) {
        ProductionPiece piece = new ProductionPiece();
        piece.setProductionPieceId("PP_TEST");
        piece.setManufacturerId("M_TEST");
        piece.setWidth(100D);
        piece.setHeight(80D);
        ImageFile mask = new ImageFile();
        mask.setRawFile(svg);
        piece.setMaskImageFile(mask);
        return piece;
    }

    private ProcedureFlow procedureFlow(String... nodeNames) {
        ProcedureFlow procedureFlow = new ProcedureFlow();
        List<ProcedureFlowNode> nodes = new ArrayList<>();
        for (String nodeName : nodeNames) {
            ProcedureFlowNode node = new ProcedureFlowNode();
            node.setNodeName(nodeName);
            nodes.add(node);
        }
        procedureFlow.setNodes(nodes);
        return procedureFlow;
    }

    private LiubaiProcessContext context(ProductionPiece piece, boolean skipBloodEdges) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId("OI_TEST");
        LiubaiProcessContext context = new LiubaiProcessContext();
        context.setOrderItem(orderItem);
        context.setProductionPiece(piece);
        context.setSkipBloodEdges(skipBloodEdges);
        return context;
    }

    private int convertMmToPixels(double valueMm) {
        return convertMmToPixels(valueMm, 36D);
    }

    private int convertMmToPixels(double valueMm, double dpi) {
        return Math.max(1, (int) Math.ceil(valueMm / 25.4D * dpi));
    }

    private void assertRgbClose(Color expected, int actualRgb) {
        Color actual = new Color(actualRgb, true);
        assertTrue(Math.abs(expected.getRed() - actual.getRed()) <= 1, "red channel");
        assertTrue(Math.abs(expected.getGreen() - actual.getGreen()) <= 1, "green channel");
        assertTrue(Math.abs(expected.getBlue() - actual.getBlue()) <= 1, "blue channel");
        assertTrue(actual.getAlpha() > 0, "alpha channel");
    }

    private static class CapturingOssTagUploadService extends OssTagUploadService {
        private byte[] uploadedPngBytes;
        private byte[] uploadedSvgBytes;
        private final List<byte[]> uploadedPngBytesList = new ArrayList<>();

        private CapturingOssTagUploadService() {
            super(null);
        }

        @Override
        public String uploadTagPng(String businessId, byte[] bytes, String subDir) {
            this.uploadedPngBytes = bytes;
            this.uploadedPngBytesList.add(bytes);
            return "https://example.test/mark-" + uploadedPngBytesList.size() + ".png";
        }

        @Override
        public String uploadTagSvg(String businessId, byte[] bytes, String subDir) {
            this.uploadedSvgBytes = bytes;
            return "https://example.test/mask.svg";
        }

        private String uploadedSvgText() {
            return new String(uploadedSvgBytes, StandardCharsets.UTF_8);
        }
    }
}
