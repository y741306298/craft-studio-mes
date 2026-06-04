package com.mes.application.command.typesetting.service;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkedNestingElementServiceTest {

    private final MarkedNestingElementService service = new MarkedNestingElementService();

    @Test
    void buildMarkedElementReturnsNullWhenMarksAreNull() {
        ProductionPiece piece = new ProductionPiece();
        piece.setId("piece-1");
        piece.setProductionPieceId("PP-1");
        piece.setProcessingFlow("留白5cm");
        piece.setRouteSvg("https://example.com/liubai.svg");

        assertFalse(service.hasMarks(piece));
        assertNull(service.buildMarkedElement(piece));
    }

    @Test
    void buildMarkedElementUsesMarksNotLiubaiTextAsHitCondition() {
        ProductionPiece piece = new ProductionPiece();
        piece.setId("piece-2");
        piece.setProductionPieceId("PP-2");
        piece.setQuantity(3);
        piece.setMarks(new LinkedHashMap<>());
        ImageFile maskFile = new ImageFile();
        maskFile.setRawFile("https://example.com/marked-mask.svg");
        piece.setMaskImageFile(maskFile);

        NestingRequest.Element element = service.buildMarkedElement(piece);

        assertTrue(service.hasMarks(piece));
        assertEquals("marked-nesting-piece-2", element.getId());
        assertEquals(3, element.getCounts());
        assertEquals(Boolean.TRUE, element.getForme());
        assertEquals("https://example.com/marked-mask.svg", element.getSvg());
        assertEquals("https://example.com/marked-mask.svg", element.getImg());
    }

    @Test
    void buildMarkedElementRequiresLayoutSvgWhenMarksArePresent() {
        ProductionPiece piece = new ProductionPiece();
        piece.setId("piece-3");
        piece.setProductionPieceId("PP-3");
        piece.setMarks(new LinkedHashMap<>());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.buildMarkedElement(piece));

        assertEquals("带 marks 的生产工件缺少可参与排版的SVG地址：PP-3", exception.getMessage());
    }
}
