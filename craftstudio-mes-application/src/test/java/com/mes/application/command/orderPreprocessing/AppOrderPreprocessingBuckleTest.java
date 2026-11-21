package com.mes.application.command.orderPreprocessing;

import com.mes.application.command.graphics.ImgUtilClient;
import com.mes.application.command.graphics.RemoteGraphics;
import com.mes.application.command.orderPreprocessing.strategy.DoubleSideMaskStrategy;
import com.mes.application.command.orderPreprocessing.strategy.NoSpecialProcedureStrategy;
import com.mes.application.command.orderPreprocessing.vo.ProductionImageSize;
import com.mes.application.command.typesetting.proces.buckle.BuckleMarkPoint;
import com.mes.application.command.typesetting.proces.buckle.FourCornerBuckleProcessService;
import com.mes.application.command.typesetting.proces.buckle.FourCornerBuckleProcessStrategy;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedure.service.ProcedureService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.service.ProcedureFlowService;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppOrderPreprocessingBuckleTest {
    private static final String BUCKLE_MASK = "https://example.com/buckle.svg";
    private static final String FRONT_IMAGE = "https://example.com/front.jpg";
    private static final String BASE_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="600" height="900" viewBox="0 0 600 900">
              <path d="M0 0 H600 V900 H0 Z" fill="#000000" stroke="#000000" stroke-width="1"/>
            </svg>
            """;

    @ParameterizedTest
    @CsvSource({"false, true", "true, true", "true, false"})
    void generatesBuckleMarksFromTheProcessNode(boolean doubleSide, boolean buckle) {
        List<ProcedureFlowNode> nodes = new ArrayList<>();
        if (doubleSide) {
            nodes.add(node("覆双面(户外)"));
            nodes.add(node("反面相同画面"));
        }
        if (buckle) {
            nodes.add(node("四角打扣"));
        }
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(nodes);
        OrderItem item = new OrderItem();
        item.setOrderItemId("item-1");
        item.setManufacturerId("manufacturer-1");
        item.setProcedureFlow(flow);
        ImageFile artwork = new ImageFile();
        artwork.setRawFile(FRONT_IMAGE);
        FilePreview preview = new FilePreview();
        preview.setRaw(FRONT_IMAGE);
        artwork.setFilePreview(preview);
        item.setProductionImgFile(artwork);

        ProductionPiece piece = new ProductionPiece();
        piece.setId("piece-mongo-id");
        piece.setProductionPieceId("PP1");
        piece.setProductImageFile(artwork);
        ImageFile mask = new ImageFile();
        mask.setRawFile(BASE_SVG);
        piece.setMaskImageFile(mask);
        ProcedureService pieces = mock(ProcedureService.class);
        when(pieces.createProductionPiece(item, "ORIGINAL", FRONT_IMAGE, flow, BASE_SVG, 600D, 900D))
                .thenReturn(piece);
        ProcedureFlowService flows = mock(ProcedureFlowService.class);
        when(flows.parseProcessingFlow(flow)).thenReturn(flow);
        RestTemplate http = mock(RestTemplate.class);
        ImgUtilClient imgUtil = mock(ImgUtilClient.class);
        OssTagUploadService uploads = mock(OssTagUploadService.class);
        String uploadDirectory = "mask/manufacturer-1/item-1/buckle/";
        String renderedSvg = "<svg><g id=\"rendered-buckle-marks\"/></svg>";
        byte[] renderedBytes = renderedSvg.getBytes(StandardCharsets.UTF_8);
        when(imgUtil.svg(eq("workflow-1"), eq("BUCKLE_APPEND"), anyMap())).thenAnswer(call -> {
            Map<String, Object> parameters = call.getArgument(2);
            assertEquals(BASE_SVG.strip(), parameters.get("originalSvg"));
            List<?> points = (List<?>) parameters.get("markPoints");
            assertEquals(List.of("25 25", "575 25", "575 875", "25 875"), points.stream()
                    .map(BuckleMarkPoint.class::cast)
                    .map(point -> (int) point.getCenterX() + " " + (int) point.getCenterY())
                    .toList());
            return renderedSvg;
        });
        when(uploads.uploadTagSvg("PP1", renderedBytes, uploadDirectory)).thenReturn(BUCKLE_MASK);

        OrderInfoService orders = mock(OrderInfoService.class);
        DoubleSideMaskStrategy doubleSideStrategy = new DoubleSideMaskStrategy();
        NoSpecialProcedureStrategy singleSideStrategy = new NoSpecialProcedureStrategy();
        ReflectionTestUtils.setField(doubleSideStrategy, "orderInfoService", orders);
        ReflectionTestUtils.setField(singleSideStrategy, "orderInfoService", orders);
        AppOrderPreprocessingService service = spy(new AppOrderPreprocessingService());
        ReflectionTestUtils.setField(service, "orderItemProcessingStrategies", List.of(doubleSideStrategy, singleSideStrategy));
        ReflectionTestUtils.setField(service, "procedureService", pieces);
        ReflectionTestUtils.setField(service, "procedureFlowService", flows);
        ReflectionTestUtils.setField(service, "fourCornerBuckleProcessService",
                new FourCornerBuckleProcessService(List.of(new FourCornerBuckleProcessStrategy(http, uploads))));
        doReturn(BASE_SVG).when(service).generateRectMaskSvgForStrategy(item);
        doReturn(new ProductionImageSize(600D, 900D)).when(service).resolveProductionImageSizeForStrategy(item);

        RemoteGraphics.enter(imgUtil, "workflow-1");
        try {
            assertEquals(List.of(piece), ReflectionTestUtils.invokeMethod(service, "processSingleOrderItem", item));
        } finally {
            RemoteGraphics.exit();
        }
        if (doubleSide) {
            assertEquals(FRONT_IMAGE, piece.getMirrorConfigs().getFirst().getImg());
        }
        if (buckle) {
            assertEquals(BUCKLE_MASK, piece.getMaskImageFile().getRawFile());
            assertEquals(Set.of("four-corner-buckle-point-lt", "four-corner-buckle-point-rt",
                    "four-corner-buckle-point-rb", "four-corner-buckle-point-lb"), piece.getMarks().keySet());
            verify(imgUtil).svg(eq("workflow-1"), eq("BUCKLE_APPEND"), anyMap());
            verify(uploads).uploadTagSvg("PP1", renderedBytes, uploadDirectory);
        } else {
            assertEquals(BASE_SVG, piece.getMaskImageFile().getRawFile());
            assertNull(piece.getMarks());
            verifyNoInteractions(imgUtil, uploads);
        }
    }

    private static ProcedureFlowNode node(String name) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(name);
        return node;
    }
}
