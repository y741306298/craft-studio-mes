package com.mes.application.command.graphics;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.command.productionPiece.AppProductionPieceService;
import com.mes.application.command.typesetting.AppTypesettingService;
import com.mes.application.command.typesetting.vo.LayoutConfirmResult;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.*;
import com.mes.domain.shared.graphics.GraphicsWorkflow;
import com.mes.infra.dal.graphics.GraphicsWorkflowRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GraphicsWorkflowServiceTest {
    private GraphicsWorkflowRepository repository;
    private AppTypesettingService layouts;
    private AppOrderPreprocessingService preprocess;
    private OrderItemService items;
    private TypesettingService domainLayouts;
    private AppProductionPieceService production;
    private ProductionPieceService pieces;
    private GraphicsWorkflowService service;
    @BeforeEach @SuppressWarnings("unchecked") void setup(){
        repository=mock(GraphicsWorkflowRepository.class);layouts=mock(AppTypesettingService.class);preprocess=mock(AppOrderPreprocessingService.class);items=mock(OrderItemService.class);domainLayouts=mock(TypesettingService.class);
        ObjectProvider<AppTypesettingService> lp=mock(ObjectProvider.class);when(lp.getObject()).thenReturn(layouts);
        ObjectProvider<AppOrderPreprocessingService> pp=mock(ObjectProvider.class);when(pp.getObject()).thenReturn(preprocess);
        ObjectProvider<AppProductionPieceService> rp=mock(ObjectProvider.class);production=mock(AppProductionPieceService.class);when(rp.getObject()).thenReturn(production);
        pieces=mock(ProductionPieceService.class);
        service=new GraphicsWorkflowService(repository,mock(ImgUtilClient.class),lp,pp,rp,items,domainLayouts,pieces,mock(OrderInfoService.class),1,10);
    }
    @AfterEach void close(){service.close();}
    private GraphicsWorkflow job(String kind,String payload){return new GraphicsWorkflow(UUID.randomUUID().toString(),"factory",kind,"business","RUNNING",payload,null,null,new Date(),new Date(),"owner");}
    @Test void dispatchReturnsBeforeRemoteWorkAndRunsWithGraphicsScope()throws Exception{
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);var finished=new CountDownLatch(1);
        var job=job("CONFIRM_LAYOUT","{\"id\":\"layout\"}");when(repository.claim(anyString())).thenReturn(job,null);
        Thread caller=Thread.currentThread();
        when(layouts.confirmLayout(any())).thenAnswer(call->{assertNotSame(caller,Thread.currentThread());assertTrue(RemoteGraphics.active());started.countDown();assertTrue(release.await(2,TimeUnit.SECONDS));var result=new LayoutConfirmResult();result.setSuccess(true);return result;});
        doAnswer(call->{finished.countDown();return null;}).when(repository).finish(eq(job.id()),anyString(),anyString(),isNull());
        try{
            service.dispatch();assertTrue(started.await(2,TimeUnit.SECONDS));assertEquals(1,finished.getCount());assertFalse(RemoteGraphics.active());
            release.countDown();assertTrue(finished.await(2,TimeUnit.SECONDS));
        }finally{release.countDown();}
    }
    @Test void stalePreprocessIsSkippedWithoutWritingPieces()throws Exception{
        var job=job("PREPROCESS","{\"orderItemId\":\"item\",\"preprocessRequestId\":\"old\"}");when(repository.claim(anyString())).thenReturn(job,null);
        var item=new OrderItem();item.setOrderItemId("item");item.setPreprocessRequestId("new");when(items.findByOrderItemId("item")).thenReturn(item);
        var done=new CountDownLatch(1);doAnswer(call->{assertTrue(call.getArgument(2,String.class).contains("skipped"));done.countDown();return null;}).when(repository).finish(eq(job.id()),anyString(),anyString(),isNull());
        service.dispatch();assertTrue(done.await(2,TimeUnit.SECONDS));verifyNoInteractions(preprocess);
    }
    @Test void businessFailureCannotBeReportedAsSucceeded()throws Exception{
        var job=job("CONFIRM_LAYOUT","{\"id\":\"layout\"}");when(repository.claim(anyString())).thenReturn(job,null);
        when(layouts.confirmLayout(any())).thenReturn(LayoutConfirmResult.failed("invalid layout"));
        var done=new CountDownLatch(1);doAnswer(call->{done.countDown();return null;}).when(repository).finish(eq(job.id()),anyString(),contains("invalid layout"),eq("invalid layout"));
        service.dispatch();assertTrue(done.await(2,TimeUnit.SECONDS));
    }
    @Test void duplicateRequestIdWithDifferentPayloadIsRejected(){
        var layout=new TypesettingInfo();layout.setId("layout");layout.setManufacturerMetaId("factory");when(domainLayouts.findById("layout")).thenReturn(layout);
        when(repository.insert(anyString(),eq("factory"),eq("CONFIRM_LAYOUT"),anyString(),anyString(),anyString())).thenReturn(job("CONFIRM_LAYOUT","{\"id\":\"different\"}"));
        assertThrows(GraphicsWorkflowException.class,()->service.submit("CONFIRM_LAYOUT",Map.of("id","layout"),"same-key"));
    }

    private TypesettingInfo storedLayout(String id) {
        var info=new TypesettingInfo();info.setId(id);info.setManufacturerMetaId("factory");when(domainLayouts.findById(id)).thenReturn(info);return info;
    }
    @Test
    void workflowAcceptsTenMebibytesButRejectsOneMoreByte() {
        var input = new LinkedHashMap<>(Map.of("manufacturerMetaId", "factory", "padding", ""));
        int remaining = 10 * 1024 * 1024 - JSON.toJSONString(input).getBytes(StandardCharsets.UTF_8).length;
        String padding = "图".repeat(remaining / 3) + "a".repeat(remaining % 3);
        input.put("padding", padding);
        when(repository.insert(anyString(), eq("factory"), eq("TO_LAYOUT"), anyString(), anyString(), anyString()))
                .thenAnswer(call -> job("TO_LAYOUT", call.getArgument(4)));

        var accepted = service.submit("TO_LAYOUT", input, "size-limit");

        assertEquals(10 * 1024 * 1024, accepted.payload().getBytes(StandardCharsets.UTF_8).length);
        input.put("padding", padding + "a");
        var error = assertThrows(IllegalArgumentException.class, () -> service.submit("TO_LAYOUT", input, "too-large"));
        assertEquals("图形工作流输入超过 10MiB", error.getMessage());
        verify(repository, times(1)).insert(anyString(), eq("factory"), eq("TO_LAYOUT"),
                anyString(), anyString(), anyString());
    }

    @Test void oversizedLayoutIsRejectedBeforePersistingOrRendering(){
        doThrow(new IllegalArgumentException("PP-test零件的尺寸大于所选规格，不能排版")).when(layouts).validateToLayoutRequest(any());
        assertThrows(IllegalArgumentException.class,()->service.submit("TO_LAYOUT",Map.of("manufacturerMetaId","factory"),null));
        verify(repository,never()).insert(anyString(),anyString(),anyString(),anyString(),anyString(),anyString());
        verify(repository,never()).waiting();
    }
    @Test void batchPrintingValidatesEveryItemBeforeAnyJobIsSaved(){
        storedLayout("first");storedLayout("second");
        doThrow(new IllegalArgumentException("设备编号不存在")).when(layouts).validateConfirmPrintRequest(argThat(r->r.getId().equals("second")));
        assertThrows(IllegalArgumentException.class,()->service.submit("BATCH_PRINT",Map.of("requests",List.of(Map.of("id","first","deviceCode","ok"),Map.of("id","second","deviceCode","bad"))),null));
        verify(layouts).validateConfirmPrintRequest(argThat(r->r.getId().equals("first")));
        verify(repository,never()).insert(anyString(),anyString(),anyString(),anyString(),anyString(),anyString());
    }
    @Test void redoAndBatchRedoUseSameAdmissionValidation(){
        var piece=new com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece();piece.setManufacturerId("factory");
        when(pieces.findByProductionPieceId("piece")).thenReturn(piece);
        doThrow(new IllegalArgumentException("未找到待排版节点")).when(production).validateRedoRequest("piece",1);
        assertThrows(IllegalArgumentException.class,()->service.submit("REDO",Map.of("productionPieceId","piece","increaseQuantity",1),null));
        assertThrows(IllegalArgumentException.class,()->service.submit("BATCH_REDO",Map.of("pieces",List.of(Map.of("productionPieceId","piece","increaseQuantity",1))),null));
        verify(repository,never()).insert(anyString(),anyString(),anyString(),anyString(),anyString(),anyString());
    }
    @Test void failedIdempotentReplayReturnsConflictInsteadOfAccepted(){
        storedLayout("layout");
        var failed=new GraphicsWorkflow("failed-id","factory","CONFIRM_LAYOUT","layout","FAILED","{\"id\":\"layout\"}",null,"尺寸超限",new Date(),new Date(),null);
        when(repository.findByDedupKey(anyString())).thenReturn(failed);
        var error=assertThrows(GraphicsWorkflowException.class,()->service.submit("CONFIRM_LAYOUT",Map.of("id","layout"),"same"));
        assertEquals(409,error.status());assertTrue(error.getMessage().contains("failed-id"));assertTrue(error.getMessage().contains("尺寸超限"));
        verifyNoInteractions(layouts);verify(repository,never()).waiting();
    }
    @Test void successfulReplayIsReturnedEvenIfQueueIsFullOrRecordStatusChanged(){
        storedLayout("layout");
        var succeeded=new GraphicsWorkflow("done-id","factory","CONFIRM_LAYOUT","layout","SUCCEEDED","{\"id\":\"layout\"}","{\"success\":true}",null,new Date(),new Date(),null);
        when(repository.findByDedupKey(anyString())).thenReturn(succeeded);when(repository.waiting()).thenReturn(100L);
        assertSame(succeeded,service.submit("CONFIRM_LAYOUT",Map.of("id","layout"),"same"));
        verifyNoInteractions(layouts);verify(repository,never()).waiting();
    }
    @Test void rejectsNullBatchItemsDuplicatesAndCrossFactory(){
        storedLayout("layout");var other=storedLayout("other");other.setManufacturerMetaId("other-factory");
        assertThrows(IllegalArgumentException.class,()->service.submit("BATCH_LAYOUT",Map.of("typesettingInfos",List.of()),null));
        assertThrows(IllegalArgumentException.class,()->service.submit("BATCH_LAYOUT",Map.of("typesettingInfos",Arrays.asList((Object)null)),null));
        assertThrows(IllegalArgumentException.class,()->service.submit("BATCH_LAYOUT",Map.of("typesettingInfos",List.of(Map.of("id","layout"),Map.of("id","layout"))),null));
        assertThrows(IllegalArgumentException.class,()->service.submit("BATCH_LAYOUT",Map.of("typesettingInfos",List.of(Map.of("id","layout"),Map.of("id","other"))),null));
        verifyNoInteractions(layouts);verify(repository,never()).insert(anyString(),anyString(),anyString(),anyString(),anyString(),anyString());
    }
    @Test void runtimeBatchFailureKeepsSuccessfulAndFailedItemResults()throws Exception{
        var batch=job("BATCH_LAYOUT","{\"typesettingInfos\":[{\"id\":\"one\"},{\"id\":\"two\"},{\"id\":\"three\"}]}");when(repository.claim(anyString())).thenReturn(batch,null);
        when(layouts.confirmLayout(any())).thenAnswer(call->{if(call.getArgument(0,TypesettingInfo.class).getId().equals("two"))throw new IllegalStateException("状态已变化");var ok=new LayoutConfirmResult();ok.setSuccess(true);return ok;});
        var done=new CountDownLatch(1);
        doAnswer(call->{var rows=com.alibaba.fastjson2.JSON.parseArray(call.getArgument(2,String.class));assertEquals(3,rows.size());assertTrue(rows.getJSONObject(0).getBooleanValue("success"));assertFalse(rows.getJSONObject(1).getBooleanValue("success"));assertEquals("two",rows.getJSONObject(1).getString("id"));assertTrue(rows.getJSONObject(2).getBooleanValue("success"));done.countDown();return null;})
                .when(repository).finish(eq(batch.id()),anyString(),anyString(),contains("批量任务存在失败项"));
        service.dispatch();assertTrue(done.await(2,TimeUnit.SECONDS));
    }
    @Test void algorithmErrorCallbackIsAcknowledgedButWorkflowFails()throws Exception{
        var callback=job("NESTING_CALLBACK","{\"id\":\"layout\",\"status\":\"error\",\"error\":\"套料失败\"}");when(repository.claim(anyString())).thenReturn(callback,null);
        var done=new CountDownLatch(1);doAnswer(call->{assertTrue(call.getArgument(2,String.class).contains("\"success\":false"));done.countDown();return null;})
                .when(repository).finish(eq(callback.id()),anyString(),anyString(),eq("套料失败"));
        service.dispatch();assertTrue(done.await(2,TimeUnit.SECONDS));verify(layouts).handleNestingCallback(any());
    }
    @Test void failedMaskProcessingCannotBeReportedAsSucceeded()throws Exception{
        var callback=job("MASK_CALLBACK","{\"callbackId\":\"item#current\",\"response\":{\"status\":\"success\",\"preprocessRequestId\":\"current\"}}");when(repository.claim(anyString())).thenReturn(callback,null);
        var item=new OrderItem();item.setStatus(com.mes.domain.order.enums.OrderStatus.FAILED);item.setPreprocessRequestId("current");item.setFailureReason("图像蒙版处理结果为空");when(items.findByOrderItemId("item")).thenReturn(item);
        var done=new CountDownLatch(1);doAnswer(call->{done.countDown();return null;}).when(repository).finish(eq(callback.id()),anyString(),anyString(),eq("图像蒙版处理结果为空"));
        service.dispatch();assertTrue(done.await(2,TimeUnit.SECONDS));
    }

}
