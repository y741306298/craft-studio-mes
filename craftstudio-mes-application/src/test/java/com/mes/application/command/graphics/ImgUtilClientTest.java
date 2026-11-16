package com.mes.application.command.graphics;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.GraphicsGenerationRequest;
import com.mes.infra.dal.graphics.GraphicsCallbackInbox;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImgUtilClientTest {
    private final AlgorithmCoreApiService algorithms=mock(AlgorithmCoreApiService.class);
    private final GraphicsCallbackInbox inbox=mock(GraphicsCallbackInbox.class);
    private final GraphicsCallbackNotifier notifier=mock(GraphicsCallbackNotifier.class);
    private final CompletableFuture<Void> wakeup=new CompletableFuture<>();
    private final AtomicReference<String> stored=new AtomicReference<>();
    private ImgUtilClient client;
    @BeforeEach void setup(){
        when(notifier.listen(anyString())).thenReturn(wakeup);when(inbox.completed(anyString())).thenAnswer(c->stored.get());
        client=new ImgUtilClient(algorithms,inbox,notifier,"http://mes/api/manufacturerSide/graphics/callback/generate_graphics","https://bucket.example.com",1000);
    }
    private String callback(String id,String workflow,boolean success){
        return JSON.toJSONString(Map.of("id",id,"status",success?"success":"error","error","too big","errorCode","INVALID_GRAPHICS",
            "result",Map.of("taskId",id,"artifact",Map.of("objectName","imgutil/result.png","width",8,"height",3)),
            "callbackCustomValue",Map.of("workflowId",workflow,"taskId",id)));
    }
    @Test void fastCallbackBeforeSubmissionReturnsIsNotLost(){
        String workflow=UUID.randomUUID().toString();
        when(algorithms.generateGraphicsAsync(any())).thenAnswer(call->{
            var request=call.getArgument(0,GraphicsGenerationRequest.class);
            verify(inbox).expect(request.getRequestId(),workflow);
            assertEquals(workflow,((com.mes.application.command.api.vo.GraphicsCallbackCustomValue)request.getCallbackConfig().getCallbackCustomValue()).getWorkflowId());
            stored.set(callback(request.getRequestId(),workflow,true));wakeup.complete(null);return null;
        });
        var image=client.render(workflow,"BLACK",Map.of("width",8,"height",3));
        assertEquals("https://bucket.example.com/imgutil/result.png",image.url());assertEquals(8,image.getWidth());
        verify(algorithms).generateGraphicsAsync(any());verify(notifier).release(anyString(),same(wakeup));
    }
    @Test void durableResultSurvivesMissingWakeupAndDoesNotSubmitAgain(){
        stored.set(callback("remote", "workflow",true));
        assertEquals(3,client.render("workflow","BLACK",Map.of()).getHeight());verifyNoInteractions(algorithms);
    }
    @Test void backgroundWaitIsResumedByCallbackNotStatusPolling()throws Exception{
        var submitted=new CountDownLatch(1);var request=new AtomicReference<GraphicsGenerationRequest>();
        when(algorithms.generateGraphicsAsync(any())).thenAnswer(call->{request.set(call.getArgument(0));submitted.countDown();return null;});
        try(var worker=Executors.newSingleThreadExecutor()){
            var result=worker.submit(()->client.render("workflow","BLACK",Map.of()));assertTrue(submitted.await(2,TimeUnit.SECONDS));assertFalse(result.isDone());
            stored.set(callback(request.get().getRequestId(),"workflow",true));wakeup.complete(null);assertEquals(8,result.get(2,TimeUnit.SECONDS).getWidth());
        }
        verify(algorithms,times(1)).generateGraphicsAsync(any());verifyNoMoreInteractions(algorithms);
    }
    @Test void remoteFailureIsNotReplacedByLocalRendering(){stored.set(callback("task","workflow",false));assertThrows(IllegalStateException.class,()->client.render("workflow","BLACK",Map.of()));}
    @Test void callbackTimeoutIsReportedAndListenerReleased(){assertThrows(IllegalStateException.class,()->client.render("workflow","BLACK",Map.of()));verify(notifier).release(anyString(),same(wakeup));}
    @Test void renderingOutsideWorkflowIsForbidden(){assertThrows(IllegalStateException.class,()->RemoteGraphics.png("BLACK",Map.of()));}
    @Test void wholePieceSubmitsOnceAndReadsAllAssetUrlsFromSameCallback(){
        when(algorithms.generateGraphicsAsync(any())).thenAnswer(call->{
            var request=call.getArgument(0,GraphicsGenerationRequest.class);
            assertEquals("SPLICE_PIECE",request.getMes().get("operation"));
            assertEquals("https://bucket.example.com",((Map<?,?>)request.getMes().get("parameters")).get("artifactBaseUrl"));
            var response=JSON.parseObject(callback(request.getRequestId(),"workflow",true));
            Map<String,String> assets=new LinkedHashMap<>();
            for(String key:List.of("dark-horizontal","dark-vertical","stripe-horizontal","stripe-vertical","yellow-horizontal","yellow-vertical","gray-horizontal","gray-vertical"))assets.put(key,"imgutil/piece/"+key+".png");
            var artifact=response.getJSONObject("result").getJSONObject("artifact");artifact.put("assets",assets);artifact.put("objectName","imgutil/piece/result.svg");
            stored.set(response.toJSONString());wakeup.complete(null);return null;
        });
        var piece=client.splicePiece("workflow",Map.of("groupText","618#1-2","marks",List.of()));
        assertEquals("https://bucket.example.com/imgutil/piece/result.svg",piece.maskUrl());
        assertEquals("https://bucket.example.com/imgutil/piece/yellow-vertical.png",piece.asset("yellow-vertical"));
        assertEquals(8,piece.assets().size());verify(algorithms,times(1)).generateGraphicsAsync(any());verifyNoMoreInteractions(algorithms);
    }
}
