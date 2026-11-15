package com.mes.application.command.graphics;

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
    private GraphicsWorkflowService service;
    @BeforeEach @SuppressWarnings("unchecked") void setup(){
        repository=mock(GraphicsWorkflowRepository.class);layouts=mock(AppTypesettingService.class);preprocess=mock(AppOrderPreprocessingService.class);items=mock(OrderItemService.class);domainLayouts=mock(TypesettingService.class);
        ObjectProvider<AppTypesettingService> lp=mock(ObjectProvider.class);when(lp.getObject()).thenReturn(layouts);
        ObjectProvider<AppOrderPreprocessingService> pp=mock(ObjectProvider.class);when(pp.getObject()).thenReturn(preprocess);
        ObjectProvider<AppProductionPieceService> rp=mock(ObjectProvider.class);
        service=new GraphicsWorkflowService(repository,mock(ImgUtilClient.class),lp,pp,rp,items,domainLayouts,mock(ProductionPieceService.class),mock(OrderInfoService.class),1,10);
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
        var done=new CountDownLatch(1);doAnswer(call->{done.countDown();return null;}).when(repository).finish(eq(job.id()),anyString(),isNull(),eq("invalid layout"));
        service.dispatch();assertTrue(done.await(2,TimeUnit.SECONDS));
    }
    @Test void duplicateRequestIdWithDifferentPayloadIsRejected(){
        var layout=new TypesettingInfo();layout.setId("layout");layout.setManufacturerMetaId("factory");when(domainLayouts.findById("layout")).thenReturn(layout);
        when(repository.insert(anyString(),eq("factory"),eq("CONFIRM_LAYOUT"),anyString(),anyString(),anyString())).thenReturn(job("CONFIRM_LAYOUT","{\"id\":\"different\"}"));
        assertThrows(GraphicsWorkflowException.class,()->service.submit("CONFIRM_LAYOUT",Map.of("id","layout"),"same-key"));
    }
}
