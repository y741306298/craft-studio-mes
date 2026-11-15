package com.mes.application.command.graphics;

import com.alibaba.fastjson2.*;
import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.command.productionPiece.AppProductionPieceService;
import com.mes.application.command.typesetting.AppTypesettingService;
import com.mes.application.command.api.resp.*;
import com.mes.application.dto.req.typesetting.*;
import com.mes.application.dto.req.productionpiece.*;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.shared.graphics.GraphicsWorkflow;
import com.mes.infra.dal.graphics.GraphicsWorkflowRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

@Service
public class GraphicsWorkflowService {
    private static final Logger log=LoggerFactory.getLogger(GraphicsWorkflowService.class);
    private final GraphicsWorkflowRepository repository;
    private final ImgUtilClient client;
    private final ObjectProvider<AppTypesettingService> typesetting;
    private final ObjectProvider<AppOrderPreprocessingService> preprocessing;
    private final ObjectProvider<AppProductionPieceService> production;
    private final OrderItemService items;
    private final TypesettingService layouts;
    private final ProductionPieceService pieces;
    private final OrderInfoService orders;
    private final String owner=UUID.randomUUID().toString();
    private final Set<String> active=ConcurrentHashMap.newKeySet();
    private final Semaphore slots;
    private final ThreadPoolExecutor executor;
    private final int capacity;
    public GraphicsWorkflowService(GraphicsWorkflowRepository repository,ImgUtilClient client,ObjectProvider<AppTypesettingService> typesetting,ObjectProvider<AppOrderPreprocessingService> preprocessing,ObjectProvider<AppProductionPieceService> production,OrderItemService items,TypesettingService layouts,ProductionPieceService pieces,OrderInfoService orders,
                                   @Value("${graphics.workflow.workers:2}")int workers,@Value("${graphics.workflow.capacity:1000}")int capacity){
        if(workers<1||workers>8||capacity<1)throw new IllegalArgumentException("图形工作流并发配置无效");
        this.repository=repository;this.client=client;this.typesetting=typesetting;this.preprocessing=preprocessing;this.production=production;this.items=items;this.layouts=layouts;this.pieces=pieces;this.orders=orders;this.capacity=capacity;slots=new Semaphore(workers);
        executor=new ThreadPoolExecutor(workers,workers,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(workers),Thread.ofPlatform().name("mes-graphics-",0).factory(),new ThreadPoolExecutor.AbortPolicy());
    }
    public synchronized GraphicsWorkflow submit(String kind,Object input,String requestId){
        String payload=JSON.toJSONString(input);if(payload.getBytes(StandardCharsets.UTF_8).length>1048576)throw new IllegalArgumentException("图形工作流输入超过 1MiB");
        JSONObject p=JSON.parseObject(payload);String tenant=tenant(kind,p),business=business(kind,p);
        if(tenant==null || tenant.isBlank())throw new IllegalArgumentException("无法确定图形工作流所属工厂");
        for(String field:List.of("requests","typesettingInfos","pieces")){
            JSONArray batch=p.getJSONArray(field);
            if(batch!=null){
                if(batch.isEmpty()||batch.size()>100)throw new IllegalArgumentException("图形工作流批量大小必须为 1–100");
                for(int i=0;i<batch.size();i++)if(!Objects.equals(tenant,tenant(field.equals("pieces")?"REDO":"CONFIRM_LAYOUT",batch.getJSONObject(i))))throw new IllegalArgumentException("批量图形工作流不能跨工厂");
            }
        }
        if(repository.waiting()>=capacity)throw new GraphicsWorkflowException(429,"图形工作流队列已满，请稍后重试");
        String key=requestId==null||requestId.isBlank()?UUID.randomUUID().toString():requestId;
        String dedup=digest(tenant+"\n"+kind+"\n"+key);
        GraphicsWorkflow job=repository.insert(UUID.randomUUID().toString(),tenant,kind,business,payload,dedup);
        if(!job.payload().equals(payload))throw new GraphicsWorkflowException(409,"图形请求编号已绑定不同参数");
        return job;
    }
    public GraphicsWorkflow find(String id,String tenant){GraphicsWorkflow job=repository.find(id,tenant);if(job==null)throw new GraphicsWorkflowException(404,"图形工作流不存在");return job;}
    public List<GraphicsWorkflow> list(String business,String tenant){return repository.list(business,tenant);}
    public GraphicsWorkflow retry(String id,String tenant){
        GraphicsWorkflow old=find(id,tenant);
        if(!Set.of("FAILED","RECOVERY_REQUIRED").contains(old.status()))throw new IllegalArgumentException("仅失败或待人工恢复的工作流可以重试");
        return submit(old.kind(),JSON.parseObject(old.payload()),null);
    }
    @Scheduled(fixedDelayString="${graphics.workflow.poll-ms:500}") public void dispatch(){
        while(slots.tryAcquire()){
            GraphicsWorkflow job;
            try{job=repository.claim(owner);}catch(Exception e){slots.release();log.error("领取图形工作流失败",e);return;}
            if(job==null){slots.release();return;}
            active.add(job.id());
            try{executor.execute(()->execute(job));}catch(RejectedExecutionException e){active.remove(job.id());slots.release();repository.finish(job.id(),owner,null,"服务正在停止，请重试");return;}
        }
    }
    @Scheduled(fixedDelay=15000) public void heartbeat(){repository.heartbeat(owner,active);repository.failAbandoned();}
    private void execute(GraphicsWorkflow job){
        try{
            RemoteGraphics.enter(client,job.id());
            JSONObject p=JSON.parseObject(job.payload());Object result=switch(job.kind()){
                case "TO_LAYOUT" -> typesetting.getObject().toLayout(p.toJavaObject(LayoutConfirmRequest.class));
                case "CONFIRM_LAYOUT" -> typesetting.getObject().confirmLayout(p.toJavaObject(TypesettingInfo.class));
                case "CONFIRM_PRINT" -> typesetting.getObject().confirmPrint(p.toJavaObject(ConfirmPrintRequest.class));
                case "BATCH_LAYOUT" -> p.toJavaObject(BatchConfirmLayoutRequest.class).getTypesettingInfos().stream().map(v->typesetting.getObject().confirmLayout(v)).toList();
                case "BATCH_PRINT" -> p.toJavaObject(BatchConfirmPrintRequest.class).getRequests().stream().map(v->typesetting.getObject().confirmPrint(v)).toList();
                case "MASK_CALLBACK" -> {preprocessing.getObject().handleGenerateMaskFilesCallback(p.getObject("response",ImageMaskResponse.class),p.getString("callbackId"));yield Map.of("accepted",true);}
                case "NESTING_CALLBACK" -> {typesetting.getObject().handleNestingCallback(p.toJavaObject(NestingResponse.class));yield Map.of("accepted",true);}
                case "PREPROCESS" -> {
                    var item=items.findByOrderItemId(p.getString("orderItemId"));
                    var order=item==null?null:orders.findByOrderId(item.getOrderId());
                    if(item==null || OrderStatus.RETURNED.equals(item.getStatus()) || (order!=null && OrderStatus.RETURNED.equals(order.getStatus())) || !Objects.equals(item.getPreprocessRequestId(),p.getString("preprocessRequestId")))yield Map.of("skipped",true,"reason","过期或已退单的预处理请求");
                    preprocessing.getObject().preprocessOrder(List.of(item));yield Map.of("accepted",true);
                }
                case "REDO" -> production.getObject().increasePendingTypesettingQuantity(p.getString("productionPieceId"),p.getInteger("increaseQuantity"));
                case "BATCH_REDO" -> production.getObject().batchRedo(p.getList("pieces",BatchRedoProductionPieceRequest.PieceRedoItem.class));
                default -> throw new IllegalArgumentException("未知图形工作流类型");
            };
            Object encoded=result==null?null:JSON.parse(JSON.toJSONString(result));
            if(encoded instanceof JSONObject value && Boolean.FALSE.equals(value.getBoolean("success")))throw new IllegalStateException(value.getString("message"));
            if(encoded instanceof JSONArray batch && batch.stream().anyMatch(v->v instanceof JSONObject row && Boolean.FALSE.equals(row.getBoolean("success")))){
                repository.finish(job.id(),owner,JSON.toJSONString(result),"批量任务存在失败项，请查看逐项 result 后重试失败项");return;
            }
            repository.finish(job.id(),owner,JSON.toJSONString(result),null);
        }catch(Exception e){
            log.error("图形工作流失败: {}",job.id(),e);
            String message=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
            if(job.kind().equals("PREPROCESS")){
                JSONObject p=JSON.parseObject(job.payload());var item=items.findByOrderItemId(p.getString("orderItemId"));
                if(item!=null && !OrderStatus.RETURNED.equals(item.getStatus()) && Objects.equals(item.getPreprocessRequestId(),p.getString("preprocessRequestId")))items.markAsFailed(item.getOrderItemId(),message);
            }
            repository.finish(job.id(),owner,null,message);
        }
        finally{RemoteGraphics.exit();active.remove(job.id());slots.release();}
    }
    private String tenant(String kind,JSONObject p){
        if("NESTING_CALLBACK".equals(kind)){var layout=layouts.findTypesettingByTypesettingId(p.getString("id"));return layout==null?null:layout.getManufacturerMetaId();}
        if("PREPROCESS".equals(kind)){var item=items.findByOrderItemId(p.getString("orderItemId"));return item==null?null:item.getManufacturerId();}
        if("MASK_CALLBACK".equals(kind)){String callback=p.getString("callbackId");var item=items.findByOrderItemId(callback.split("#",2)[0]);return item==null?null:item.getManufacturerId();}
        if("REDO".equals(kind)){var piece=pieces.findByProductionPieceId(p.getString("productionPieceId"));return piece==null?null:piece.getManufacturerId();}
        if("BATCH_REDO".equals(kind))return tenant("REDO",p.getJSONArray("pieces").getJSONObject(0));
        if(p.getJSONArray("typesettingInfos")!=null && !p.getJSONArray("typesettingInfos").isEmpty())return tenant("CONFIRM_LAYOUT",p.getJSONArray("typesettingInfos").getJSONObject(0));
        if(p.getString("manufacturerMetaId")!=null)return p.getString("manufacturerMetaId");
        if(p.getJSONArray("requests")!=null && !p.getJSONArray("requests").isEmpty())return tenant("CONFIRM_LAYOUT",p.getJSONArray("requests").getJSONObject(0));
        if(p.getString("id")!=null){var layout=layouts.findById(p.getString("id"));if(layout!=null)return layout.getManufacturerMetaId();}
        return null;
    }
    private String business(String kind,JSONObject p){
        if("MASK_CALLBACK".equals(kind))return p.getString("callbackId").split("#",2)[0];
        for(String field:List.of("orderItemId","id","productionPieceId"))if(p.getString(field)!=null)return p.getString(field);
        return "batch";
    }
    private String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    @PreDestroy public void close(){executor.shutdown();try{if(!executor.awaitTermination(15,TimeUnit.SECONDS))executor.shutdownNow();}catch(InterruptedException e){Thread.currentThread().interrupt();executor.shutdownNow();}}
}
