package com.mes.application.command.productionPiece;

import com.mes.domain.manufacturer.procedureFlow.entity.*;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedoAdmissionTest {
    @Test void rejectsInvalidQuantityAndMissingPendingNodeBeforeCreatingPiece() {
        var service=new AppProductionPieceService();var pieces=mock(ProductionPieceService.class);
        ReflectionTestUtils.setField(service,"domainProductionPieceService",pieces);
        for(Integer quantity:Arrays.asList(null,0,-1))assertThrows(BusinessNotAllowException.class,()->service.validateRedoRequest("piece",quantity));
        var piece=new ProductionPiece();piece.setQuantity(5);var flow=new ProcedureFlow();flow.setNodes(List.of());piece.setProcedureFlow(flow);
        when(pieces.findByProductionPieceId("piece")).thenReturn(piece);
        assertThrows(BusinessNotAllowException.class,()->service.validateRedoRequest("piece",1));
        var pending=new ProcedureFlowNode();pending.setNodeName("待排版");pending.setPieceQuantity(5);flow.setNodes(List.of(pending));
        assertSame(piece,service.validateRedoRequest("piece",1));
        assertEquals(5,piece.getQuantity());assertEquals(5,pending.getPieceQuantity());
        verify(pieces,never()).addProductionPiece(any());
    }
}
