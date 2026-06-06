package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Liubai15CmProcessStrategyTest {
    private final FakeOssTagUploadService ossTagUploadService = new FakeOssTagUploadService();
    private final Liubai15CmProcessStrategy strategy = new Liubai15CmProcessStrategy(null, ossTagUploadService);

    @Test
    void superWidthSpliceSkipBloodEdgesIncludesCoveredEdge() {
        ProductionPiece piece = pieceWithInlineMask();
        piece.setSeq(1);
        piece.setGroup("12#1-3");
        Blood blood = new Blood();
        blood.setX(-1);
        blood.setY(0);
        piece.setBlood(blood);

        strategy.process(context(piece, true));

        assertEquals(1000D, piece.getWidth());
        assertEquals(800D, piece.getHeight());
    }

    @Test
    void horizontalSuperWidthSpliceSkipTopAndBottomBloodEdges() {
        ProductionPiece piece = pieceWithInlineMask();
        piece.setSeq(1);
        piece.setGroup("12#1-3");
        Blood blood = new Blood();
        blood.setX(0);
        blood.setY(1);
        piece.setBlood(blood);

        strategy.process(context(piece, true));

        assertEquals(1300D, piece.getWidth());
        assertEquals(500D, piece.getHeight());
    }


    @Test
    void superWidthSpliceDashedBorderSticksToVerticalBloodEdges() {
        ProductionPiece piece = pieceWithInlineMask();
        piece.setSeq(1);
        piece.setGroup("12#1-3");
        Blood blood = new Blood();
        blood.setX(-1);
        blood.setY(0);
        piece.setBlood(blood);

        strategy.process(context(piece, true));

        String uploadedSvg = ossTagUploadService.uploadedSvgText();
        assertTrue(uploadedSvg.contains("<path d=\"M0 -150 H1000 V650 H0 Z\""));
        assertTrue(uploadedSvg.contains("<path d=\"M0 -100 H1000 V600 H0 Z\" fill=\"none\" stroke=\"#808080\" stroke-width=\"1.23\" stroke-dasharray=\"6 4\""));
    }

    @Test
    void superWidthSpliceDashedBorderSticksToHorizontalBloodEdges() {
        ProductionPiece piece = pieceWithInlineMask();
        piece.setSeq(1);
        piece.setGroup("12#1-3");
        Blood blood = new Blood();
        blood.setX(0);
        blood.setY(1);
        piece.setBlood(blood);

        strategy.process(context(piece, true));

        String uploadedSvg = ossTagUploadService.uploadedSvgText();
        assertTrue(uploadedSvg.contains("<path d=\"M-150 0 H1150 V500 H-150 Z\""));
        assertTrue(uploadedSvg.contains("<path d=\"M-100 0 H1100 V500 H-100 Z\" fill=\"none\" stroke=\"#808080\" stroke-width=\"1.23\" stroke-dasharray=\"6 4\""));
    }

    @Test
    void directRouteStillExpandsAllEdges() {
        ProductionPiece piece = pieceWithInlineMask();
        piece.setSeq(1);
        piece.setGroup("12#1-3");
        Blood blood = new Blood();
        blood.setX(-1);
        blood.setY(0);
        piece.setBlood(blood);

        strategy.process(context(piece, false));

        assertEquals(1300D, piece.getWidth());
        assertEquals(800D, piece.getHeight());
    }

    private ProductionPiece pieceWithInlineMask() {
        ProductionPiece piece = new ProductionPiece();
        piece.setProductionPieceId("PP_TEST");
        piece.setManufacturerId("M_TEST");
        piece.setWidth(1000D);
        piece.setHeight(500D);
        ImageFile mask = new ImageFile();
        mask.setRawFile("<svg width=\"1000\" height=\"500\" viewBox=\"0 0 1000 500\"><rect width=\"1000\" height=\"500\"/></svg>");
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

    private static class FakeOssTagUploadService extends OssTagUploadService {
        private byte[] uploadedSvgBytes;

        private FakeOssTagUploadService() {
            super(null);
        }

        @Override
        public String uploadTagPng(String businessId, byte[] bytes, String subDir) {
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
