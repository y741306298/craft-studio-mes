package com.mes.application.command.graphics;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.resp.GraphicsGenerationResponse;
import com.mes.infra.dal.graphics.GraphicsCallbackInbox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class GraphicsCallbackService {
    private final GraphicsCallbackInbox inbox;
    private final GraphicsCallbackNotifier notifier;
    private final String key;
    public GraphicsCallbackService(GraphicsCallbackInbox inbox,GraphicsCallbackNotifier notifier,@Value("${graphics.imgutil.api-key:}")String key){this.inbox=inbox;this.notifier=notifier;this.key=key;}
    public void accept(byte[] body,String signature){
        if(key==null || key.isBlank())throw new GraphicsWorkflowException(503,"图形回调密钥未配置");
        if(signature==null || !MessageDigest.isEqual(sign(body).getBytes(StandardCharsets.US_ASCII),signature.getBytes(StandardCharsets.US_ASCII)))throw new GraphicsWorkflowException(401,"图形回调签名无效");
        GraphicsGenerationResponse response=JSON.parseObject(body,GraphicsGenerationResponse.class);
        var custom=response.getCallbackCustomValue();
        if(custom==null || custom.getWorkflowId()==null || response.getId()==null || !response.getId().equals(custom.getTaskId()))throw new IllegalArgumentException("图形回调关联参数无效");
        if(!"success".equals(response.getStatus())&&!"error".equals(response.getStatus()))throw new IllegalArgumentException("图形回调不是终态结果");
        if("success".equals(response.getStatus()) && (response.getResult()==null || response.getResult().getArtifact()==null || !response.getId().equals(response.getResult().getTaskId())))throw new IllegalArgumentException("图形成功回调缺少结果");
        inbox.accept(response.getId(),custom.getWorkflowId(),JSON.toJSONString(response));
        // Publishing is retried by the sender if Redis is unavailable; the persisted duplicate remains safe.
        notifier.publish(response.getId());
    }
    private String sign(byte[] body){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(body));}catch(Exception e){throw new IllegalStateException("图形回调验签失败",e);}}
}
