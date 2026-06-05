package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(uploadedSvg.contains("<path d=\"M-50 -50 H150 V130 H-50 Z\""));
        assertTrue(uploadedSvg.contains("<path d=\"M0 0 H100 V80 H0 Z\" fill=\"none\" stroke=\"#808080\""));
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
        return Math.max(1, (int) Math.ceil(valueMm / 25.4D * 36D));
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

        private CapturingOssTagUploadService() {
            super(null);
        }

        @Override
        public String uploadTagPng(String businessId, byte[] bytes, String subDir) {
            this.uploadedPngBytes = bytes;
            return "https://example.test/mark.png";
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
