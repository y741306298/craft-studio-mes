package com.mes.application.command.productionPiece;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppProductionPieceServiceTest {

    @Test
    void redoStartsWithQuantityOnlyInPendingTypesettingNode() {
        ProductionPiece original = new ProductionPiece();
        original.setProductionPieceId("piece-1");
        original.setQuantity(1);

        ProcedureFlowNode pendingTypesetting = node("NODE_TYPESETTING", "待排版", 0, NodeStatus.COMPLETED);
        ProcedureFlowNode typesettingInProgress = node(
                "NODE_TYPESETTING_IN_PROGRESS", "排版中", 1, NodeStatus.ACTIVE);
        typesettingInProgress.setOperatorId("operator-1");
        typesettingInProgress.setStartTime(new Date());
        typesettingInProgress.setRetryCount(2);
        typesettingInProgress.setErrorMessage("old error");
        ProcedureFlowNode printingInProgress = node(
                "NODE_PRINTING_IN_PROGRESS", "打印中", 0, NodeStatus.PENDING);

        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(pendingTypesetting, typesettingInProgress, printingInProgress));
        original.setProcedureFlow(flow);

        ProductionPieceService domainService = mock(ProductionPieceService.class);
        when(domainService.findByProductionPieceId("piece-1")).thenReturn(original);
        when(domainService.addProductionPiece(any(ProductionPiece.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AppProductionPieceService service = new AppProductionPieceService();
        ReflectionTestUtils.setField(service, "domainProductionPieceService", domainService);

        service.increasePendingTypesettingQuantity("piece-1", 1);

        ArgumentCaptor<ProductionPiece> captor = ArgumentCaptor.forClass(ProductionPiece.class);
        verify(domainService).addProductionPiece(captor.capture());
        ProductionPiece redo = captor.getValue();
        assertThat(redo.getQuantity()).isEqualTo(1);
        assertThat(redo.getProcedureFlow().getNodes())
                .extracting(ProcedureFlowNode::getPieceQuantity)
                .containsExactly(1, 0, 0);
        assertThat(redo.getProcedureFlow().getNodes())
                .extracting(ProcedureFlowNode::getNodeStatus)
                .containsOnly(NodeStatus.PENDING);
        ProcedureFlowNode redoTypesettingNode = redo.getProcedureFlow().getNodes().get(1);
        assertThat(redoTypesettingNode.getOperatorId()).isNull();
        assertThat(redoTypesettingNode.getStartTime()).isNull();
        assertThat(redoTypesettingNode.getRetryCount()).isNull();
        assertThat(redoTypesettingNode.getErrorMessage()).isNull();
        assertThat(typesettingInProgress.getPieceQuantity()).isEqualTo(1);
        assertThat(typesettingInProgress.getNodeStatus()).isEqualTo(NodeStatus.ACTIVE);
    }

    @Test
    void redoMarkedPieceRewritesEmbeddedSvgIdToNewPieceId() {
        ProductionPiece original = new ProductionPiece();
        original.setId("64f000000000000000000001");
        original.setProductionPieceId("piece-1");
        original.setOrderItemId("item-1");
        original.setManufacturerId("manufacturer-1");
        original.setProductionPieceType("ORIGINAL");
        original.setMarks(new HashMap<>());
        ImageFile mask = new ImageFile();
        mask.setRawFile("<svg><g id=\"64f000000000000000000001\"><path/></g></svg>");
        original.setMaskImageFile(mask);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(node("NODE_TYPESETTING", "待排版", 0, NodeStatus.PENDING)));
        original.setProcedureFlow(flow);

        ProductionPieceService domainService = mock(ProductionPieceService.class);
        when(domainService.findByProductionPieceId("piece-1")).thenReturn(original);
        when(domainService.addProductionPiece(any(ProductionPiece.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        OssTagUploadService uploadService = mock(OssTagUploadService.class);
        when(uploadService.uploadTagSvg(anyString(), any(byte[].class), anyString()))
                .thenReturn("https://oss.example/redo.svg");
        AppProductionPieceService service = new AppProductionPieceService();
        ReflectionTestUtils.setField(service, "domainProductionPieceService", domainService);
        ReflectionTestUtils.setField(service, "ossTagUploadService", uploadService);

        ProductionPiece redo = service.increasePendingTypesettingQuantity("piece-1", 2);

        assertThat(redo.getId()).isNotBlank().isNotEqualTo(original.getId());
        assertThat(redo.getProductionPieceId()).isNotBlank().isNotEqualTo(original.getProductionPieceId());
        assertThat(redo.getMaskImageFile().getRawFile()).isEqualTo("https://oss.example/redo.svg");
        assertThat(original.getMaskImageFile().getRawFile()).startsWith("<svg>");
        ArgumentCaptor<byte[]> svgCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(uploadService).uploadTagSvg(anyString(), svgCaptor.capture(), anyString());
        assertThat(new String(svgCaptor.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("id=\"" + redo.getId() + "\"")
                .doesNotContain("id=\"" + original.getId() + "\"");
    }

    @Test
    void rewriteSvgPieceIdSupportsSingleQuotedIdAndLeavesOtherIdsUntouched() {
        String rewritten = AppProductionPieceService.rewriteSvgPieceId(
                "<svg id='root'><g id='old-piece'/><g id='mark-old-piece'/></svg>",
                "old-piece", "new-piece");

        assertThat(rewritten).isEqualTo(
                "<svg id='root'><g id='new-piece'/><g id='mark-old-piece'/></svg>");
    }

    private ProcedureFlowNode node(String id, String name, int quantity, NodeStatus status) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeId(id);
        node.setNodeName(name);
        node.setPieceQuantity(quantity);
        node.setNodeStatus(status);
        return node;
    }
}
