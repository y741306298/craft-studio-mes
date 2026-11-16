package com.mes.application.command.typesetting.proces.splice;

import com.mes.application.command.graphics.*;
import com.mes.domain.manufacturer.procedureFlow.entity.*;
import com.mes.domain.manufacturer.productionPiece.entity.*;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SpliceMarksBatchTest {
    @ParameterizedTest
    @CsvSource({"1,6,false", "2,8,false", "3,2,false", "1,0,true"})
    void submitsAllPieceMarksOnceAndUsesPublishedSvg(int seq, int expectedMarks, boolean existingMarks) {
        String original = "<svg xmlns='http://www.w3.org/2000/svg' width='100' height='200'><path d='M0 0 H100 V200 H0 Z'/></svg>";
        if (existingMarks) original = original.replace("</svg>", "<g id='adhesive-splice-existing'/></svg>");
        var client = mock(ImgUtilClient.class);
        Map<String,String> urls = new LinkedHashMap<>();
        for (String name : List.of("dark-horizontal", "dark-vertical", "stripe-horizontal", "stripe-vertical",
                "yellow-horizontal", "yellow-vertical", "gray-horizontal", "gray-vertical")) urls.put(name, "https://bucket/" + name + ".png");
        when(client.splicePiece(anyString(), anyMap())).thenReturn(new RemotePieceGraphics("https://bucket/final.svg", urls));
        var service = new SuperWidthSpliceProcessService();
        var piece = new ProductionPiece();
        piece.setId("piece-mongo"); piece.setProductionPieceId("piece-business");
        piece.setWidth(100D); piece.setHeight(200D); piece.setSeq(seq); piece.setGroup("# 1-3");
        var mask = new ImageFile(); mask.setRawFile("https://bucket/original.svg"); piece.setMaskImageFile(mask);
        var order = new OrderItem(); order.setManufacturerId("factory");
        var node = new ProcedureFlowNode(); node.setNodeName("背胶拼接");
        var flow = new ProcedureFlow(); flow.setNodes(List.of(node));
        var blood = new Blood(); blood.setX(0); blood.setY(-20);
        try (var reader = mockStatic(SvgResourceReader.class)) {
            reader.when(() -> SvgResourceReader.read("https://bucket/original.svg"))
                    .thenReturn(original.getBytes(StandardCharsets.UTF_8));
            RemoteGraphics.enter(client, "workflow");
            try { service.process(order, flow, piece, blood); }
            finally { RemoteGraphics.exit(); }
        }
        var parameters = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(client, times(1)).splicePiece(eq("workflow"), parameters.capture());
        var request = parameters.getValue();
        assertEquals(original, request.get("originalSvg"));
        assertEquals("piece-mongo", request.get("pieceMongoId"));
        @SuppressWarnings("unchecked") var marks = (List<Map<String, Object>>) request.get("marks");
        assertEquals(expectedMarks, marks.size());
        assertEquals(expectedMarks, marks.stream().map(mark -> mark.get("id")).distinct().count());
        if (seq < 3 && !existingMarks) {
            var gray = marks.stream().filter(mark -> mark.get("id").toString().contains("text-gray-bottom-a")).findFirst().orElseThrow();
            assertEquals(0D, gray.get("x")); assertEquals(184D, gray.get("y"));
            assertEquals(40D, gray.get("width")); assertEquals(8D, gray.get("height"));
        }
        assertEquals(existingMarks ? "https://bucket/original.svg" : "https://bucket/final.svg", piece.getMaskImageFile().getRawFile());
        assertEquals("https://bucket/dark-horizontal.png", piece.getMarks().get("superWidthSpliceHorizontalDarkMark"));
        assertTrue(marks.stream().allMatch(mark -> mark.containsKey("assetKey") && !mark.containsKey("img")));
        verifyNoMoreInteractions(client);
    }
}
