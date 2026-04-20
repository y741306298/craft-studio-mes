package com.mes.interfaces.api.platform.configSide.procedureFlow;

import com.mes.application.command.procedureFlow.AppProcedureFlowService;
import com.mes.application.dto.req.procedureFlow.ProcedureFlowListRequest;
import com.mes.application.dto.req.procedureFlow.ProcedureFlowRequest;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.procedureFlow.ProcedureFlowListResponse;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;

import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configSide/procedureFlow")
public class ProcedureFlowController {

    @Autowired
    private AppProcedureFlowService appProcedureFlowService;

    /**
     * 分页查询工序流程列表
     * @param request 分页请求参数
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<ProcedureFlowListResponse> listProcedureFlows(
            @Valid @RequestBody ProcedureFlowListRequest request) {
        
        PagedQuery query = request.toPagedQuery();
        String procedureFlowName = request.getProcedureFlowName();
        
        PagedResult<ProcedureFlow> result = appProcedureFlowService.findProcedureFlows(procedureFlowName, query);
        
        List<ProcedureFlowListResponse> responses = result.items().stream()
                .map(ProcedureFlowListResponse::from)
                .collect(Collectors.toList());
        
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    /**
     * 根据 ID 获取工序流程详情
     * @param id 工序流程 ID
     * @return 工序流程详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ProcedureFlowListResponse> getProcedureFlowById(@PathVariable String id) {
        ProcedureFlow procedureFlow = appProcedureFlowService.findById(id);
        ProcedureFlowListResponse response = ProcedureFlowListResponse.from(procedureFlow);
        return ApiResponse.success(response);
    }

    /**
     * 新增工序流程
     * @param request 新增请求参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> addProcedureFlow(@Valid @RequestBody ProcedureFlowRequest request) {
        ProcedureFlow procedureFlow = request.toDomainEntity();
        appProcedureFlowService.addProcedureFlow(procedureFlow);
        
        return ApiResponse.success("success");
    }

    /**
     * 编辑工序流程
     * @param request 编辑请求参数
     * @return 操作结果
     */
    @PostMapping("/edit")
    public ApiResponse<String> updateProcedureFlow(@Valid @RequestBody ProcedureFlowRequest request) {
        ProcedureFlow existingFlow = appProcedureFlowService.findById(request.getId());
        if (existingFlow == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "工序流程不存在");
        }

        ProcedureFlow updatedFlow = request.toDomainEntity();
        updatedFlow.setId(existingFlow.getId());
        updatedFlow.setCreateTime(existingFlow.getCreateTime());

        appProcedureFlowService.updateProcedureFlow(updatedFlow);

        return ApiResponse.success("success");
    }

    /**
     * 删除工序流程
     * @param id 工序流程 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteProcedureFlow(@PathVariable String id) {
        appProcedureFlowService.deleteProcedureFlow(id);
        return ApiResponse.success("success");
    }
}
