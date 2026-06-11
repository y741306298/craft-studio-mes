package com.mes.application.command.typesetting.vo;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypesettingProductionPieceVOTest {

    @Test
    void fromTypesettingInfoRemovesMirrorSuffixFromGroupId() {
        TypesettingInfo info = buildTypesettingInfo("TS-001-Mirror");

        TypesettingProductionPieceVO vo = TypesettingProductionPieceVO.fromTypesettingInfo(info);

        assertEquals("TS-001", vo.getGroupId());
    }

    @Test
    void fromTypesettingInfoKeepsNonMirrorTypesettingIdAsGroupId() {
        TypesettingInfo info = buildTypesettingInfo("TS-001");

        TypesettingProductionPieceVO vo = TypesettingProductionPieceVO.fromTypesettingInfo(info);

        assertEquals("TS-001", vo.getGroupId());
    }

    @Test
    void fromProductionPieceReturnsProcessingFlow() {
        ProductionPiece piece = new ProductionPiece();
        piece.setProcessingFlow("激光切割-覆膜");

        TypesettingProductionPieceVO vo = TypesettingProductionPieceVO.fromProductionPiece(piece);

        assertEquals("激光切割-覆膜", vo.getProcessingFlow());
    }

    private TypesettingInfo buildTypesettingInfo(String typesettingId) {
        TypesettingInfo info = new TypesettingInfo();
        info.setTypesettingId(typesettingId);
        TypesettingElement element = new TypesettingElement();
        element.setWidth(BigDecimal.ONE);
        element.setHeight(BigDecimal.ONE);
        info.setElement(element);
        return info;
    }
}
