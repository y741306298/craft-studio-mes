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
    private static final int MAX_PAYLOAD_MIB = 10;
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
    public GraphicsWorkflowService(GraphicsWorkflowRepository repository,ImgUtilClient client,ObjectProvider<AppTypesettingService> typesetting,ObjectProvider<AppOrderPreprocessingService> preprocessing,ObjectProvider<AppProductionPieceService> production,OrderItemService items,TypesettingService layouts,ProductionPieceService pieces,OrderInfoService orders,
                                   @Value("${graphics.workflow.workers:2}")int workers){
        if(workers<1||workers>8)throw new IllegalArgumentException("图形工作流并发配置无效");
        this.repository=repository;this.client=client;this.typesetting=typesetting;this.preprocessing=preprocessing;this.production=production;this.items=items;this.layouts=layouts;this.pieces=pieces;this.orders=orders;slots=new Semaphore(workers);
        executor=new ThreadPoolExecutor(workers,workers,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(workers),Thread.ofPlatform().name("mes-graphics-",0).factory(),new ThreadPoolExecutor.AbortPolicy());
    }
    public synchronized GraphicsWorkflow submit(String kind,Object input,String requestId){
        if (!Set.of("TO_LAYOUT", "CONFIRM_LAYOUT", "CONFIRM_PRINT", "BATCH_LAYOUT", "BATCH_PRINT", "REDO", "BATCH_REDO", "PREPROCESS", "MASK_CALLBACK", "NESTING_CALLBACK").contains(kind == null ? "" : kind)) throw new IllegalArgumentException("未知图形工作流类型");
        if (input == null) throw new IllegalArgumentException("图形工作流参数不能为空");
        String payload = JSON.toJSONString(input);
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_MIB * 1024 * 1024) {
            throw new IllegalArgumentException("图形工作流输入超过 " + MAX_PAYLOAD_MIB + "MiB");
        }
        JSONObject p=JSON.parseObject(payload);
        String batchField = switch (kind) { case "BATCH_LAYOUT" -> "typesettingInfos"; case "BATCH_PRINT" -> "requests"; case "BATCH_REDO" -> "pieces"; default -> null; };
        if (batchField != null) {
            JSONArray batch = p.getJSONArray(batchField);
            if (batch == null || batch.isEmpty() || batch.size() > 100) throw new IllegalArgumentException("图形工作流批量大小必须为 1–100");
            if (batch.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("批量条目不能为空");
        }
        String tenant=tenant(kind,p),business=business(kind,p);
        if(tenant==null || tenant.isBlank())throw new IllegalArgumentException("无法确定图形工作流所属工厂");
        String key=requestId==null||requestId.isBlank()?UUID.randomUUID().toString():requestId;
        String dedup=digest(tenant+"\n"+kind+"\n"+key);
        GraphicsWorkflow existing = repository.findByDedupKey(dedup);
        if (existing != null) return checkedReplay(existing, payload);
        if (batchField != null) {
            Set<String> ids = new HashSet<>();
            for (Object row : p.getJSONArray(batchField)) {
                JSONObject value = (JSONObject)row;
                String childKind = kind.equals("BATCH_REDO") ? "REDO" : kind.equals("BATCH_PRINT") ? "CONFIRM_PRINT" : "CONFIRM_LAYOUT";
                if (!Objects.equals(tenant, tenant(childKind, value))) throw new IllegalArgumentException("批量图形工作流不能跨工厂");
                if (!kind.equals("BATCH_REDO") && !ids.add(value.getString("id"))) throw new IllegalArgumentException("批量确认不能包含重复印版ID");
            }
        }
        validateBeforeSubmit(kind, p);
        return checkedReplay(repository.insert(UUID.randomUUID().toString(),tenant,kind,business,payload,dedup), payload);
    }
    private GraphicsWorkflow checkedReplay(GraphicsWorkflow job, String payload) {
        if(!job.payload().equals(payload))throw new GraphicsWorkflowException(409,"图形请求编号已绑定不同参数");
        if (!Set.of("PREPROCESS", "MASK_CALLBACK", "NESTING_CALLBACK").contains(job.kind())
                && Set.of("FAILED", "RECOVERY_REQUIRED").contains(job.status())) {
            throw new GraphicsWorkflowException(409,"图形工作流 " + job.id() + " " + job.status() + ": " + job.error() + "；请核对业务状态后使用重试接口");
        }
        return job;
    }
    private void validateBeforeSubmit(String kind, JSONObject p) {
        switch (kind) {
            case "TO_LAYOUT" -> typesetting.getObject().validateToLayoutRequest(p.toJavaObject(LayoutConfirmRequest.class));
            case "CONFIRM_LAYOUT" -> typesetting.getObject().validateConfirmLayoutRequest(p.toJavaObject(TypesettingInfo.class));
            case "CONFIRM_PRINT" -> typesetting.getObject().validateConfirmPrintRequest(p.toJavaObject(ConfirmPrintRequest.class));
            case "BATCH_LAYOUT" -> p.getList("typesettingInfos", TypesettingInfo.class).forEach(typesetting.getObject()::validateConfirmLayoutRequest);
            case "BATCH_PRINT" -> p.getList("requests", ConfirmPrintRequest.class).forEach(typesetting.getObject()::validateConfirmPrintRequest);
            case "REDO" -> production.getObject().validateRedoRequest(p.getString("productionPieceId"),p.getInteger("increaseQuantity"));
            case "BATCH_REDO" -> p.getJSONArray("pieces").forEach(row -> {
                JSONObject item = (JSONObject)row;
                production.getObject().validateRedoRequest(item.getString("productionPieceId"),item.getInteger("increaseQuantity"));
            });
            default -> { /* Callback acceptance means durable receipt; results remain asynchronous. */ }
        }
    }
    public GraphicsWorkflow find(String id,String tenant){GraphicsWorkflow job=repository.find(id,tenant);if(job==null)throw new GraphicsWorkflowException(404,"图形工作流不存在");return job;}
    public List<GraphicsWorkflow> list(String business,String tenant){return repository.list(business,tenant);}
    public GraphicsWorkflow retry(String id,String tenant){
        GraphicsWorkflow old=find(id,tenant);
        if(!Set.of("FAILED","RECOVERY_REQUIRED").contains(old.status()))throw new IllegalArgumentException("仅失败或待人工恢复的工作流可以重试");
        JSONObject payload=JSON.parseObject(old.payload());
        if (!Objects.equals(tenant, tenant(old.kind(),payload))) throw new GraphicsWorkflowException(403,"工作流业务对象已不属于当前工厂，不能重试");
        return submit(old.kind(),payload,null);
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
                case "BATCH_LAYOUT" -> executeConfirmationBatch(p.getJSONArray("typesettingInfos"), false);
                case "BATCH_PRINT" -> executeConfirmationBatch(p.getJSONArray("requests"), true);
                case "MASK_CALLBACK" -> {
                    var response=p.getObject("response",ImageMaskResponse.class);
                    preprocessing.getObject().handleGenerateMaskFilesCallback(response,p.getString("callbackId"));
                    String error="error".equalsIgnoreCase(response.getStatus()) ? response.getError()==null?"图像蒙版处理失败":response.getError() : null;
                    String[] identity=p.getString("callbackId").split("#");
                    var current=items.findByOrderItemId(identity[0]);
                    String request=response.getPreprocessRequestId()!=null?response.getPreprocessRequestId():identity.length>1?identity[1]:null;
                    if (current!=null && OrderStatus.FAILED.equals(current.getStatus()) && (request==null || Objects.equals(current.getPreprocessRequestId(),request))) error=current.getFailureReason()==null?"图像蒙版处理失败":current.getFailureReason();
                    yield callbackOutcome(error);
                }
                case "NESTING_CALLBACK" -> {
                    var response=p.toJavaObject(NestingResponse.class);typesetting.getObject().handleNestingCallback(response);
                    yield callbackOutcome("success".equalsIgnoreCase(response.getStatus())?null:response.getError()==null?"排版异步生成失败":response.getError());
                }
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
            if(encoded instanceof JSONObject value && Boolean.FALSE.equals(value.getBoolean("success"))){
                repository.finish(job.id(),owner,JSON.toJSONString(result),value.getString("message") == null ? "业务执行失败" : value.getString("message"));return;
            }
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
    private Map<String,Object> callbackOutcome(String error) {
        return error == null ? Map.of("accepted",true,"success",true)
                : Map.of("accepted",true,"success",false,"message",error);
    }
    private List<JSONObject> executeConfirmationBatch(JSONArray batch, boolean printing) {
        List<JSONObject> results = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            JSONObject input = batch.getJSONObject(i);
            JSONObject result;
            try {
                Object value = printing ? typesetting.getObject().confirmPrint(input.toJavaObject(ConfirmPrintRequest.class))
                        : typesetting.getObject().confirmLayout(input.toJavaObject(TypesettingInfo.class));
                result = JSON.parseObject(JSON.toJSONString(value));
                if (result == null) throw new IllegalStateException("业务未返回处理结果");
            } catch (Exception e) {
                result = new JSONObject();result.put("success",false);result.put("message",e.getMessage()==null?"业务执行失败":e.getMessage());
            }
            result.put("id",input.getString("id"));results.add(result);
        }
        return results;
    }
    private String tenant(String kind,JSONObject p){
        if("NESTING_CALLBACK".equals(kind)){var layout=layouts.findTypesettingByTypesettingId(p.getString("id"));return layout==null?null:layout.getManufacturerMetaId();}
        if("PREPROCESS".equals(kind)){var item=items.findByOrderItemId(p.getString("orderItemId"));return item==null?null:item.getManufacturerId();}
        if("MASK_CALLBACK".equals(kind)){String callback=p.getString("callbackId");var item=items.findByOrderItemId(callback.split("#",2)[0]);return item==null?null:item.getManufacturerId();}
        if("REDO".equals(kind)){var piece=pieces.findByProductionPieceId(p.getString("productionPieceId"));return piece==null?null:piece.getManufacturerId();}
        if("BATCH_REDO".equals(kind))return tenant("REDO",p.getJSONArray("pieces").getJSONObject(0));
        if(p.getJSONArray("typesettingInfos")!=null && !p.getJSONArray("typesettingInfos").isEmpty())return tenant("CONFIRM_LAYOUT",p.getJSONArray("typesettingInfos").getJSONObject(0));
        if (Set.of("CONFIRM_LAYOUT", "CONFIRM_PRINT").contains(kind)) {
            var layout = p.getString("id") == null ? null : layouts.findById(p.getString("id"));
            return layout == null ? null : layout.getManufacturerMetaId();
        }
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
