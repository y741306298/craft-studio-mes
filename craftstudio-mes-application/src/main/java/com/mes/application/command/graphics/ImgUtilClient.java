package com.mes.application.command.graphics;

import com.alibaba.fastjson2.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/** POST asynchronous tasks and poll only from a bounded MES background worker. */
@Service
public class ImgUtilClient {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(ImgUtilClient.class);
    private final RestTemplate http;
    private final String base,key,artifactBase;
    private final long timeoutMs,pollMs;
    public ImgUtilClient(@Value("${graphics.imgutil.base-url:http://127.0.0.1:8092}")String base,
                         @Value("${graphics.imgutil.api-key:}")String key,
                         @Value("${graphics.imgutil.artifact-base-url:}")String artifactBase,
                         @Value("${graphics.imgutil.wait-timeout-ms:180000}")long timeoutMs,
                         @Value("${graphics.imgutil.poll-interval-ms:500}")long pollMs){
        this.base=base.replaceAll("/+$","");this.key=key;this.artifactBase=artifactBase.replaceAll("/+$","");this.timeoutMs=timeoutMs;this.pollMs=pollMs;
        if(timeoutMs<1000 || pollMs<100)throw new IllegalArgumentException("图形轮询配置无效");
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(Duration.ofSeconds(3));factory.setReadTimeout(Duration.ofSeconds(10));http=new RestTemplate(factory);
    }
    private HttpHeaders headers(){var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);headers.set("X-Api-Key",key);return headers;}
    private JSONObject task(String workflow,String operation,Map<String,Object> parameters){
        String requestId=id(workflow,operation,parameters);
        log.info("imgUtil 图形步骤: workflowId={}, taskId={}, operation={}",workflow,requestId,operation);
        Map<String,Object> body=Map.of("requestId",requestId,"mes",Map.of("operation",operation,"parameters",parameters));
        long deadline=System.currentTimeMillis()+timeoutMs;
        // Re-submit the identical request ID after an uncertain response, never create duplicate render jobs.
        for(;;){
            try{http.exchange(base+"/api/graphics/tasks",HttpMethod.POST,new HttpEntity<>(JSON.toJSONString(body),headers()),String.class);break;}
            catch(HttpClientErrorException | HttpServerErrorException e){
                int status=e.getStatusCode().value();if(status!=429 && status!=503)throw new IllegalStateException("imgUtil 拒绝任务: HTTP "+status,e);
            }catch(ResourceAccessException e){if(System.currentTimeMillis()>=deadline)throw new IllegalStateException("imgUtil 提交超时",e);}
            pause(deadline);
        }
        for(;;){
            JSONObject data;
            try{
                String response=http.exchange(base+"/api/graphics/tasks/"+requestId,HttpMethod.GET,new HttpEntity<Void>(headers()),String.class).getBody();
                data=JSON.parseObject(response).getJSONObject("data");
            }catch(ResourceAccessException | HttpServerErrorException e){pause(deadline);continue;}
            String status=data.getString("status");
            if("SUCCEEDED".equals(status))return data;
            if("FAILED".equals(status)||"TIMED_OUT".equals(status))throw new IllegalStateException("imgUtil taskId="+requestId+" "+data.getString("errorCode")+": "+data.getString("message"));
            pause(deadline);
        }
    }
    public RemoteImage render(String workflow,String operation,Map<String,Object> parameters){
        JSONObject job=task(workflow,operation,parameters),artifact=job.getJSONObject("artifact");
        String object=artifact.getString("objectName");
        if(object==null || object.isBlank() || artifactBase.isBlank())throw new IllegalStateException("MES 接入需开启 imgUtil OSS 并配置 graphics.imgutil.artifact-base-url");
        return new RemoteImage(job.getString("taskId"),artifactBase+"/"+object,artifact.getIntValue("width"),artifact.getIntValue("height"));
    }
    public String svg(String workflow,String operation,Map<String,Object> parameters){
        JSONObject job=task(workflow,operation,parameters);
        String path=job.getJSONObject("artifact").getString("downloadPath");
        if(path==null || !path.equals("/api/graphics/tasks/"+job.getString("taskId")+"/artifact"))throw new IllegalStateException("imgUtil 返回了无效下载路径");
        return http.execute(base+path,HttpMethod.GET,r->r.getHeaders().putAll(headers()),response->{
            byte[] bytes=response.getBody().readNBytes(1048577);
            if(bytes.length>1048576)throw new IllegalStateException("SVG 结果超过 MES 1MiB 上限");
            return new String(bytes,StandardCharsets.UTF_8);
        });
    }
    public String saveSvg(String workflow,byte[] content){return render(workflow,"SVG_SAVE",Map.of("source",new String(content,StandardCharsets.UTF_8))).url();}
    private void pause(long deadline){
        if(System.currentTimeMillis()>=deadline)throw new IllegalStateException("等待 imgUtil 结果超时，请按 workflowId 在日志中查询远程 taskId");
        try{Thread.sleep(Math.min(pollMs,Math.max(1,deadline-System.currentTimeMillis())));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("图形工作流已停止",e);}
    }
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
