package com.mes.application.command.graphics;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import java.util.*;

/** Rendering is permitted only inside a bounded, persistent background workflow. */
public final class RemoteGraphics {
    private RemoteGraphics(){}
    public record Scope(ImgUtilClient client,String workflowId){}
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
    public static boolean active(){return CURRENT.get()!=null;}
    public static void enter(ImgUtilClient client,String id){if(active())throw new IllegalStateException("图形上下文重复进入");CURRENT.set(new Scope(client,id));}
    public static void exit(){CURRENT.remove();}
    private static Scope current(){Scope value=CURRENT.get();if(value==null)throw new IllegalStateException("图形操作必须在异步工作流中执行");return value;}
    public static RemoteImage image(String operation,Map<String,Object> parameters){var scope=current();return scope.client().render(scope.workflowId(),operation,parameters);}
    public static RemoteImage png(String operation,Map<String,Object> parameters){return image(operation,parameters);}
    public static String svg(String operation,Map<String,Object> parameters){var scope=current();return scope.client().svg(scope.workflowId(),operation,parameters);}
    public static RemotePieceGraphics splicePiece(Map<String,Object> parameters){var scope=current();return scope.client().splicePiece(scope.workflowId(),parameters);}
    public static String saveSvg(byte[] content){var scope=current();return scope.client().saveSvg(scope.workflowId(),content);}
    public static RemoteImage transform(RemoteImage source,double angle,int cropWidth){return image("TRANSFORM",params("sourceTaskId",source.taskId(),"angle",angle,"cropWidth",cropWidth));}
    public static RemoteImage transformFloor(RemoteImage source,double angle){return image("TRANSFORM",params("sourceTaskId",source.taskId(),"angle",angle,"rounding","FLOOR"));}
    public static Map<String,Object> params(Object... pairs){return with(Map.of(),pairs);}
    public static Map<String,Object> with(Map<String,Object> base,Object... pairs){
        if(pairs.length%2!=0)throw new IllegalArgumentException("参数必须成对");
        Map<String,Object> result=new LinkedHashMap<>(base);
        for(int i=0;i<pairs.length;i+=2)if(pairs[i+1]!=null)result.put(pairs[i].toString(),pairs[i+1]);
        return result;
    }
    public static Object fields(Object value){return value==null?null:JSON.parse(JSON.toJSONString(value,JSONWriter.Feature.FieldBased));}
}
