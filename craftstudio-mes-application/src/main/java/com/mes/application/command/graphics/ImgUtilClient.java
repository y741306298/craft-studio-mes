package com.mes.application.command.graphics;

import com.alibaba.fastjson2.*;
import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.GraphicsGenerationRequest;
import com.mes.application.command.api.resp.GraphicsGenerationResponse;
import com.mes.application.command.api.vo.*;
import com.mes.infra.dal.graphics.GraphicsCallbackInbox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Shared algorithm submission + durable callback inbox. Never polls imgUtil task status. */
@Service
public class ImgUtilClient {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(ImgUtilClient.class);
    private final AlgorithmCoreApiService algorithms;
    private final GraphicsCallbackInbox inbox;
    private final GraphicsCallbackNotifier notifier;
    private final String callbackUrl,artifactBase;
    private final long timeoutMs;
    public ImgUtilClient(AlgorithmCoreApiService algorithms,GraphicsCallbackInbox inbox,GraphicsCallbackNotifier notifier,
                         @Value("${external.callbackApi.generate_graphics:}")String callbackUrl,
                         @Value("${graphics.imgutil.artifact-base-url:}")String artifactBase,
                         @Value("${graphics.imgutil.wait-timeout-ms:180000}")long timeoutMs){
        this.algorithms=algorithms;this.inbox=inbox;this.notifier=notifier;this.callbackUrl=callbackUrl;
        this.artifactBase=artifactBase.replaceAll("/+$","");this.timeoutMs=timeoutMs;
        if(timeoutMs<1000)throw new IllegalArgumentException("图形回调等待期限无效");
    }
    private JSONObject task(String workflow,String operation,Map<String,Object> parameters){
        if(callbackUrl.isBlank())throw new IllegalArgumentException("请配置 external.callbackApi.generate_graphics");
        String requestId=id(workflow,operation,parameters);
        log.info("imgUtil callback step: workflowId={}, taskId={}, operation={}",workflow,requestId,operation);
        var signal=notifier.listen(requestId);
        try{
            inbox.expect(requestId,workflow);
            String completed=inbox.completed(requestId);
            if(completed==null){
                GraphicsGenerationRequest request=new GraphicsGenerationRequest();request.setRequestId(requestId);
                request.setMes(Map.of("operation",operation,"parameters",parameters));
                GraphicsCallbackCustomValue custom=new GraphicsCallbackCustomValue();custom.setId(workflow);custom.setWorkflowId(workflow);custom.setTaskId(requestId);
                CallbackConfig callback=new CallbackConfig();callback.setCallbackUrl(callbackUrl);callback.setCallbackCustomValue(custom);request.setCallbackConfig(callback);
                long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMs);
                algorithms.generateGraphicsAsync(request);
                completed=inbox.completed(requestId);
                if(completed==null){
                    try{signal.get(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);}
                    catch(TimeoutException ignored){ /* Re-read once: a durable callback may outlive a lost Redis wake-up. */ }
                    completed=inbox.completed(requestId);
                }
            }
            if(completed==null)throw new IllegalStateException("等待图形回调超时: taskId="+requestId);
            GraphicsGenerationResponse response=JSON.parseObject(completed,GraphicsGenerationResponse.class);
            if(!"success".equals(response.getStatus()))throw new IllegalStateException("imgUtil taskId="+requestId+" "+response.getErrorCode()+": "+response.getError());
            return JSON.parseObject(JSON.toJSONString(response.getResult()));
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("等待图形回调被中断",e);}
        catch(ExecutionException e){throw new IllegalStateException("图形回调监听失败",e.getCause());}
        finally{notifier.release(requestId,signal);}
    }
    public RemoteImage render(String workflow,String operation,Map<String,Object> parameters){
        JSONObject job=task(workflow,operation,parameters),artifact=job.getJSONObject("artifact");
        String object=artifact.getString("objectName");
        if(object==null || object.isBlank() || artifactBase.isBlank())throw new IllegalStateException("MES 接入需开启 imgUtil OSS 并配置 graphics.imgutil.artifact-base-url");
        return new RemoteImage(job.getString("taskId"),artifactBase+"/"+object,artifact.getIntValue("width"),artifact.getIntValue("height"));
    }
    public RemotePieceGraphics splicePiece(String workflow, Map<String,Object> parameters) {
        if (artifactBase.isBlank()) throw new IllegalStateException("请配置 graphics.imgutil.artifact-base-url");
        JSONObject artifact = task(workflow, "SPLICE_PIECE", RemoteGraphics.with(parameters, "artifactBaseUrl", artifactBase)).getJSONObject("artifact");
        String object = artifact.getString("objectName");
        JSONObject assets = artifact.getJSONObject("assets");
        if (object == null || object.isBlank() || assets == null || assets.size() != 8) {
            throw new IllegalStateException("零件图形结果缺少 OSS 蒙版或素材");
        }
        Map<String,String> urls = new LinkedHashMap<>();
        assets.forEach((key,value) -> {
            if (!(value instanceof String name) || name.isBlank()) throw new IllegalStateException("零件素材 OSS 对象名为空");
            urls.put(key, artifactBase + "/" + name);
        });
        return new RemotePieceGraphics(artifactBase + "/" + object, urls);
    }
    public String svg(String workflow,String operation,Map<String,Object> parameters){
        JSONObject job=task(workflow,operation,parameters);
        // An artifact GET transfers SVG text only; it never queries task status.
        return algorithms.readGraphicsSvgArtifact(job.getString("taskId"));
    }
    public String saveSvg(String workflow,byte[] content){return render(workflow,"SVG_SAVE",Map.of("source",new String(content,StandardCharsets.UTF_8))).url();}
    private String id(String workflow,String operation,Map<String,Object> parameters){
        try{
            Object canonical=canonical(JSON.parse(JSON.toJSONString(parameters)));
            byte[] hash=MessageDigest.getInstance("SHA-256").digest((operation+JSON.toJSONString(canonical)).getBytes(StandardCharsets.UTF_8));
            return "mes-"+workflow.replace("-","")+"-"+HexFormat.of().formatHex(hash).substring(0,32);
        }catch(Exception e){throw new IllegalArgumentException("图形任务参数无法序列化",e);}
    }
    private Object canonical(Object value){
        if(value instanceof Map<?,?> m){Map<String,Object> sorted=new TreeMap<>();m.forEach((k,v)->sorted.put(k.toString(),canonical(v)));return sorted;}
        if(value instanceof Collection<?> c)return c.stream().map(this::canonical).toList();return value;
    }
}
