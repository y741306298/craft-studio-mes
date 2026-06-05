package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Liubai15CmProcessStrategyTest {
    private final Liubai15CmProcessStrategy strategy = new Liubai15CmProcessStrategy(null, new FakeOssTagUploadService());

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
        private FakeOssTagUploadService() {
            super(null);
        }

        @Override
        public String uploadTagPng(String businessId, byte[] bytes, String subDir) {
            return "https://example.test/mark.png";
        }

        @Override
        public String uploadTagSvg(String businessId, byte[] bytes, String subDir) {
            return "https://example.test/mask.svg";
        }
    }
}
