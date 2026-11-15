package com.mes.application.command.graphics;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.typesetting.vo.*;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;
import java.util.*;

/** Existing application APIs enqueue durable workflows; worker re-entry executes the original business guards. */
@Aspect
@Component
public class GraphicsWorkflowAspect {
    private final GraphicsWorkflowService workflows;
    public GraphicsWorkflowAspect(GraphicsWorkflowService workflows){this.workflows=workflows;}
    @Around("execution(public * com.mes.application.command.typesetting.AppTypesettingService.toLayout(..)) || execution(public * com.mes.application.command.typesetting.AppTypesettingService.confirmLayout(..)) || execution(public * com.mes.application.command.typesetting.AppTypesettingService.confirmPrint(..)) || execution(public * com.mes.application.command.typesetting.AppTypesettingService.batchConfirmLayout(..)) || execution(public * com.mes.application.command.typesetting.AppTypesettingService.batchConfirmPrint(..)) || execution(public * com.mes.application.command.typesetting.AppTypesettingService.handleNestingCallback(..)) || execution(public * com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService.preprocessOrder(..)) || execution(public * com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService.handleGenerateMaskFilesCallback(..))")
    public Object enqueue(ProceedingJoinPoint call)throws Throwable {
        if(RemoteGraphics.active())return call.proceed();
        String method=call.getSignature().getName();Object[] args=call.getArgs();
        if(method.equals("preprocessOrder")){
            if(args[0]==null)return null;
            for(Object value:(List<?>)args[0]){
                OrderItem item=(OrderItem)value;if(item==null)continue;
                workflows.submit("PREPROCESS",RemoteGraphics.params("orderItemId",item.getOrderItemId(),"preprocessRequestId",item.getPreprocessRequestId()),"preprocess:"+item.getOrderItemId()+":"+item.getPreprocessRequestId());
            }
            return null;
        }
        String kind=switch(method){case "toLayout"->"TO_LAYOUT";case "confirmLayout"->"CONFIRM_LAYOUT";case "confirmPrint"->"CONFIRM_PRINT";case "batchConfirmLayout"->"BATCH_LAYOUT";case "batchConfirmPrint"->"BATCH_PRINT";case "handleNestingCallback"->"NESTING_CALLBACK";default->"MASK_CALLBACK";};
        Object input=kind.equals("MASK_CALLBACK")?RemoteGraphics.params("response",args[0],"callbackId",args[1]):args[0];
        String key=requestId();
        if(kind.endsWith("CALLBACK"))key=kind+":"+JSON.toJSONString(input);
        var job=workflows.submit(kind,input,key);
        accepted();
        if(kind.endsWith("CALLBACK"))return null;
        if(kind.equals("CONFIRM_PRINT")||kind.equals("BATCH_PRINT")){
            ConfirmPrintResult result=new ConfirmPrintResult();result.setSuccess(true);result.setGraphicsTaskId(job.id());result.setMessage("图形工作流已接收，请查询工作流状态；尚未完成制版或下发");return result;
        }
        LayoutConfirmResult result=new LayoutConfirmResult();result.setSuccess(true);result.setGraphicsTaskId(job.id());result.setMessage("图形工作流已接收，请查询工作流状态；尚未完成排版或制版");return result;
    }
    private String requestId(){var attributes=RequestContextHolder.getRequestAttributes();return attributes instanceof ServletRequestAttributes a?a.getRequest().getHeader("X-Graphics-Request-Id"):null;}
    private void accepted(){var attributes=RequestContextHolder.getRequestAttributes();if(attributes instanceof ServletRequestAttributes a && a.getResponse()!=null)a.getResponse().setStatus(202);}
}
