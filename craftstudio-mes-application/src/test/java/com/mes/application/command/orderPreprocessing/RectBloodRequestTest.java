package com.mes.application.command.orderPreprocessing;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.req.RectBloodRequest;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RectBloodRequestTest {
    @Test
    void usesSelectedEdgesAndWhiteAccessoryFromSameNode() {
        OrderItem item = item("""
                {"nodeName":"矩形旗缝套筒","paramConfigs":[
                  {"type":"RECT_EDGE_SELECTION","param":{"edges":["TOP"]}},
                  {"type":"ACC","param":{"accessorySnapshot":{"name":"大白边"}}}
                ]}
                """);
        RectBloodRequest request = RectBloodRequest.fromOrderItem(item);
        assertNotNull(request);
        assertEquals("input.tif", request.getImageUrl());
        assertEquals(RectBloodRequest.ScrollEdge.TOP, request.getBloodConfig().getScrollEdge());
        assertEquals("#FFFFFF", request.getBloodConfig().getScrollColorHex());
        assertEquals(20, request.getBloodConfig().getScrollWidth());
        assertEquals(10, request.getBloodConfig().getSewWidth());
        assertTrue(request.getBloodConfig().getMirrorRequired());
    }

    @Test
    void ignoresWhiteAccessoryOnOtherNodes() {
        OrderItem item = item("""
                {"nodeName":"矩形旗缝套筒","paramConfigs":[
                  {"type":"RECT_EDGE_SELECTION","param":{"edges":["LEFT"]}}
                ]}
                """);
        item.getProcedureFlow().getNodes().add(JSON.parseObject("""
                {"nodeName":"其他工艺","paramConfigs":[
                  {"type":"ACC","param":{"accessorySnapshot":{"name":"大白边"}}}
                ]}
                """, ProcedureFlowNode.class));
        assertNull(RectBloodRequest.fromOrderItem(item).getBloodConfig().getScrollColorHex());
    }

    @Test
    void skipsMissingConfigurationAndUnrelatedNodes() {
        assertNull(RectBloodRequest.fromOrderItem(item("{\"nodeName\":\"矩形旗缝套筒\"}")));
        assertNull(RectBloodRequest.fromOrderItem(item("""
                {"nodeName":"热升华","paramConfigs":[
                  {"type":"RECT_EDGE_SELECTION","param":{"edges":["TOP"]}}
                ]}
                """)));
    }

    @Test
    void rejectsMissingEdgesRatherThanSilentlySubmittingWrongBleed() {
        assertThrows(IllegalArgumentException.class, () -> RectBloodRequest.fromOrderItem(item("""
                {"nodeName":"矩形旗缝套筒","paramConfigs":[
                  {"type":"RECT_EDGE_SELECTION","param":{}}
                ]}
                """)));
    }

    private OrderItem item(String nodeJson) {
        OrderItem item = new OrderItem();
        ImageFile image = new ImageFile();
        image.setRawFile("input.tif");
        item.setProductionImgFile(image);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(new java.util.ArrayList<>(List.of(JSON.parseObject(nodeJson, ProcedureFlowNode.class))));
        item.setProcedureFlow(flow);
        return item;
    }
}
