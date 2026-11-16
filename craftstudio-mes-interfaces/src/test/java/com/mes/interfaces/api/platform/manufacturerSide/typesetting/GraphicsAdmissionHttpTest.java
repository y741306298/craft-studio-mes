package com.mes.interfaces.api.platform.manufacturerSide.typesetting;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.graphics.*;
import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.command.productionPiece.AppProductionPieceService;
import com.mes.application.command.typesetting.AppTypesettingService;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.dto.req.typesetting.ToLayoutRequest;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.order.orderInfo.service.*;
import com.mes.domain.shared.graphics.GraphicsWorkflow;
import com.mes.infra.dal.graphics.GraphicsWorkflowRepository;
import com.mes.interfaces.api.handler.ApiControllerHandler;
import org.junit.jupiter.api.*;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GraphicsAdmissionHttpTest {
    private final GraphicsWorkflowRepository repository=mock(GraphicsWorkflowRepository.class);
    private final AppTypesettingService app=mock(AppTypesettingService.class);
    private GraphicsWorkflowService workflows;
    private MockMvc mvc;
    private String request;
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        ObjectProvider<AppTypesettingService> provider=mock(ObjectProvider.class);when(provider.getObject()).thenReturn(app);
        var layouts=mock(TypesettingService.class);var info=new TypesettingInfo();info.setId("layout");info.setManufacturerMetaId("factory");when(layouts.findById("layout")).thenReturn(info);
        workflows=new GraphicsWorkflowService(repository,mock(ImgUtilClient.class),provider,mock(ObjectProvider.class),mock(ObjectProvider.class),mock(OrderItemService.class),layouts,mock(ProductionPieceService.class),mock(OrderInfoService.class),1,10);
        var factory=new AspectJProxyFactory(app);factory.setProxyTargetClass(true);factory.addAspect(new GraphicsWorkflowAspect(workflows));
        var controller=new TypesettingController();ReflectionTestUtils.setField(controller,"appTypesettingService",factory.getProxy());
        mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiControllerHandler()).build();
        request=JSON.toJSONString(Map.of("manufacturerMetaId","factory","layoutMode","large_vision_layout",
                "typesettingCells",List.of(Map.of("sourceType",TypesettingSourceType.PART.getCode(),"sourceId","piece","quantity",1)),
                "containers",List.of(Map.of("width",1000,"height",1000))));
        when(repository.insert(anyString(),anyString(),anyString(),anyString(),anyString(),anyString())).thenAnswer(c ->
                new GraphicsWorkflow("new-task",c.getArgument(1),c.getArgument(2),c.getArgument(3),"QUEUED",c.getArgument(4),null,null,new Date(),new Date(),null));
    }
    @AfterEach void close(){workflows.close();}
    @Test void oversizedToLayoutReturnsHttp400AndNeverEnqueues() throws Exception {
        doThrow(new IllegalArgumentException("PP-test零件的尺寸大于所选规格，不能排版")).when(app).validateToLayoutRequest(any());
        mvc.perform(post("/api/manufacturerSide/typesetting/toLayout").contentType("application/json").content(request))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("PP-test零件的尺寸大于所选规格，不能排版"));
        verify(repository,never()).insert(anyString(),anyString(),anyString(),anyString(),anyString(),anyString());
        verify(app,never()).toLayout(any());
    }
    @Test void acceptedResponseExplicitlyContainsPendingStatus() throws Exception {
        mvc.perform(post("/api/manufacturerSide/typesetting/toLayout").contentType("application/json").content(request))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.status").value("QUEUED")).andExpect(jsonPath("$.data.graphicsTaskId").value("new-task"));
        verify(app).validateToLayoutRequest(any());verify(app,never()).toLayout(any());
    }
    @Test void allConfirmEndpointsRejectAdmissionFailuresInsteadOfReturningSuccess() throws Exception {
        doThrow(new IllegalArgumentException("只有待确认的排版记录才能确认")).when(app).validateConfirmLayoutRequest(any());
        doThrow(new IllegalArgumentException("设备编号不存在")).when(app).validateConfirmPrintRequest(any());
        for(var entry:Map.of("confirmLayout","{\"id\":\"layout\"}","confirmLayout/batch","{\"typesettingInfos\":[{\"id\":\"layout\"}]}",
                "confirmPrint","{\"id\":\"layout\",\"deviceCode\":\"missing\"}","confirmPrint/batch","{\"requests\":[{\"id\":\"layout\",\"deviceCode\":\"missing\"}]}").entrySet()) {
            mvc.perform(post("/api/manufacturerSide/typesetting/"+entry.getKey()).contentType("application/json").content(entry.getValue())).andExpect(status().isBadRequest());
        }
        verify(repository,never()).insert(anyString(),anyString(),anyString(),anyString(),anyString(),anyString());
    }
    @Test void knownFailedReplayReturnsHttp409WithOriginalTaskIdAndReason() throws Exception {
        String payload=JSON.toJSONString(JSON.parseObject(request,ToLayoutRequest.class).toLayoutConfirmRequest());
        when(repository.findByDedupKey(anyString())).thenReturn(new GraphicsWorkflow("failed-task","factory","TO_LAYOUT","batch","FAILED",payload,null,"尺寸超限",new Date(),new Date(),null));
        String body=mvc.perform(post("/api/manufacturerSide/typesetting/toLayout").header("X-Graphics-Request-Id","same").contentType("application/json").content(request))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409)).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("failed-task"));assertTrue(body.contains("尺寸超限"));
    }
    @Test void completedReplayReturnsHttp200WithCompletedStatus() throws Exception {
        String payload=JSON.toJSONString(JSON.parseObject(request,ToLayoutRequest.class).toLayoutConfirmRequest());
        when(repository.findByDedupKey(anyString())).thenReturn(new GraphicsWorkflow("done-task","factory","TO_LAYOUT","batch","SUCCEEDED",payload,"{\"success\":true,\"message\":\"排版任务已提交\"}",null,new Date(),new Date(),null));
        mvc.perform(post("/api/manufacturerSide/typesetting/toLayout").header("X-Graphics-Request-Id","same").contentType("application/json").content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUCCEEDED")).andExpect(jsonPath("$.data.graphicsTaskId").value("done-task"));
        verify(app,never()).validateToLayoutRequest(any());
    }
}
