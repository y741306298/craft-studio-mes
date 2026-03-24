package com.mes.interfaces.api.platform.configSide.procedure;

import com.mes.application.command.procedure.AppProcedureService;
import com.mes.domain.manufacturer.procedure.entity.Procedure;
import com.mes.interfaces.api.dto.req.procedure.ProcedureListRequest;
import com.mes.interfaces.api.dto.req.procedure.ProcedureRequest;
import com.mes.interfaces.api.dto.resp.ApiResponse;
import com.mes.interfaces.api.dto.resp.PagedApiResponse;
import com.mes.interfaces.api.dto.resp.procedure.ProcedureListResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configSide/procedure")
public class ProcedureController {

    @Autowired
    AppProcedureService appProcedureService;

    /**
     * 分页查询工序
     * @param request 分页请求参数
     * @param procedureName 工序名称（可选）
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<ProcedureListResponse> listProcedures(
            @Valid @RequestBody ProcedureListRequest request) {
        
        // 参数验证已经在PagedApiRequest中处理
        
        // 转换为领域层查询对象
        PagedQuery query = request.toPagedQuery();
        String procedureName = request.getProcedureName();
        
        // 调用应用服务查询数据
        PagedResult<Procedure> result = appProcedureService.findProcedures(procedureName, query);
        
        // 转换为响应DTO
        List<ProcedureListResponse> responses = result.items().stream()
                .map(ProcedureListResponse::from)
                .collect(Collectors.toList());
        
        // 返回分页响应
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    /**
     * 根据ID获取工序详情
     * @param id 工序ID
     * @return 工序详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ProcedureListResponse> getProcedureById(@PathVariable String id) {
        Procedure procedure = appProcedureService.findById(id);
        ProcedureListResponse response = ProcedureListResponse.from(procedure);
        return ApiResponse.success(response);
    }

    /**
     * 新增工序
     * @param request 新增请求参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> addProcedure(@Valid @RequestBody ProcedureRequest request) {
        // 参数验证已经在@Valid和Request中处理
        
        // 直接转换并保存
        Procedure procedure = request.toDomainEntity();
        appProcedureService.addProcedure(procedure);
        
        return ApiResponse.success("success");
    }

    /**
     * 编辑工序
     * @param request 编辑请求参数
     * @return 操作结果
     */
    @PutMapping("/edit")
    public ApiResponse<String> updateProcedure(@Valid @RequestBody ProcedureRequest request) {
        // 先查询现有数据
        Procedure existingProcedure = appProcedureService.findById(request.getId());
        if (existingProcedure == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "工序不存在");
        }

        // 使用Request中的转换方法更新所有字段
        Procedure updatedProcedure = request.toDomainEntity();

        // 保留原有ID和其他不可更改的字段
        updatedProcedure.setId(existingProcedure.getId());
        updatedProcedure.setCreateTime(existingProcedure.getCreateTime());

        // 调用应用服务更新
        appProcedureService.updateProcedure(updatedProcedure);

        return ApiResponse.success("success");
    }

    /**
     * 删除工序
     * @param id 工序ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteProcedure(@PathVariable String id) {
        appProcedureService.deleteProcedure(id);
        return ApiResponse.success("success");
    }
}
