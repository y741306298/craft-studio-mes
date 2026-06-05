package com.mes.application.command.api.req;

import com.mes.application.command.orderPreprocessing.splice.SpliceProcessStrategies;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageMaskRequestSpliceProcessStrategyTest {

    @Test
    void allSpliceNodeNamesAreRecognized() {
        for (String nodeName : List.of("超幅拼接", "背胶拼接", "写真拼接", "覆板拼接", "喷绘拼接", "无痕拼接", "板材拼接")) {
            ProcedureFlow procedureFlow = new ProcedureFlow();
            ProcedureFlowNode node = new ProcedureFlowNode();
            node.setNodeName(nodeName);
            procedureFlow.setNodes(List.of(node));

            assertTrue(SpliceProcessStrategies.hasSpliceNode(procedureFlow), nodeName + " should be treated as splice node");
        }
    }

    @Test
    void spliceStrategyControlsSliceDefaultBloodBeforeCallingAlgorithm() {
        ImageMaskRequest photoRequest = buildRequest("写真拼接");
        ImageMaskRequest inkjetRequest = buildRequest("喷绘拼接");
        ImageMaskRequest adhesiveRequest = buildRequest("背胶拼接");

        assertEquals(0, photoRequest.getSlice().getXs().getFirst().getBlood());
        assertEquals(30, inkjetRequest.getSlice().getXs().getFirst().getBlood());
        assertEquals(20, adhesiveRequest.getSlice().getXs().getFirst().getBlood());
    }

    @Test
    void superWidthSpliceKeepsCoordinateBloodOverrideForCompatibility() {
        ImageMaskRequest request = buildRequest("超幅拼接", List.of(Map.of("value", 100, "blood", 12)), List.of(200));

        assertEquals(12, request.getSlice().getXs().getFirst().getBlood());
        assertEquals(20, request.getSlice().getYs().getFirst().getBlood());
    }

    private ImageMaskRequest buildRequest(String nodeName) {
        return buildRequest(nodeName, List.of(100), List.of(200));
    }

    private ImageMaskRequest buildRequest(String nodeName, List<?> xs, List<?> ys) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(nodeName);
        MTOProductSpecDTO.ProcessParamConfigDTO config = new MTOProductSpecDTO.ProcessParamConfigDTO();
        config.setParam(Map.of("xs", xs, "ys", ys));
        node.setParamConfigs(List.of(config));

        return ImageMaskRequest.processWithSplicing(
                orderItemWithImages(),
                List.of(node),
                false,
                true,
                SpliceProcessStrategies.defaults()
        );
    }

    private OrderItem orderItemWithImages() {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId("OI_TEST");
        orderItem.setProductionImgFile(imageFile("https://example.com/raw.png"));
        orderItem.setMaskImgFile(imageFile("https://example.com/mask.svg"));
        return orderItem;
    }

    private ImageFile imageFile(String rawUrl) {
        ImageFile imageFile = new ImageFile();
        imageFile.setRawFile(rawUrl);
        FilePreview preview = new FilePreview();
        preview.setRaw(rawUrl);
        imageFile.setFilePreview(preview);
        return imageFile;
    }
}
