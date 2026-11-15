package com.mes.interfaces.api.platform.manufacturerSide.graphics;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.auth.AppLoginService;
import com.mes.application.command.graphics.GraphicsWorkflowService;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.shared.graphics.GraphicsWorkflow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/manufacturerSide/graphics/workflows")
public class GraphicsWorkflowController {
    private final GraphicsWorkflowService workflows;
    private final AppLoginService login;
    private final String defaultAuthorization;
    public GraphicsWorkflowController(GraphicsWorkflowService workflows,AppLoginService login,@Value("${mes.auth.default-authorization:Bearer craftstudio-mes-default}")String defaultAuthorization){this.workflows=workflows;this.login=login;this.defaultAuthorization=defaultAuthorization;}
    public record View(String graphicsTaskId,String manufacturerMetaId,String kind,String businessId,String status,Date createdAt,Date updatedAt,Object result,String error){
        public static View of(GraphicsWorkflow job){return new View(job.id(),job.manufacturerMetaId(),job.kind(),job.businessId(),job.status(),job.createdAt(),job.updatedAt(),job.result()==null?null:JSON.parse(job.result()),job.error());}
    }
    @GetMapping("/{id}") public ApiResponse<View> find(@PathVariable String id,@RequestParam String manufacturerMetaId,@RequestHeader("Authorization")String authorization){authorize(manufacturerMetaId,authorization);return ApiResponse.success(View.of(workflows.find(id,manufacturerMetaId)));}
    @GetMapping public ApiResponse<List<View>> list(@RequestParam String businessId,@RequestParam String manufacturerMetaId,@RequestHeader("Authorization")String authorization){authorize(manufacturerMetaId,authorization);return ApiResponse.success(workflows.list(businessId,manufacturerMetaId).stream().map(View::of).toList());}
    @PostMapping("/{id}/retry") @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<View> retry(@PathVariable String id,@RequestParam String manufacturerMetaId,@RequestHeader("Authorization")String authorization){authorize(manufacturerMetaId,authorization);return ApiResponse.success(View.of(workflows.retry(id,manufacturerMetaId)));}
    private void authorize(String tenant,String authorization){
        if(tenant==null||tenant.isBlank())throw new IllegalArgumentException("manufacturerMetaId 不能为空");
        if(defaultAuthorization!=null && !defaultAuthorization.isBlank() && defaultAuthorization.equals(authorization))return;
        if(!authorization.startsWith("Bearer ") || !Objects.equals(tenant,login.getManufacturerMetaIdByToken(authorization.substring(7).trim())))throw new IllegalArgumentException("不能查询其他工厂的图形工作流");
    }
}
