package com.mes.interfaces.api.platform.configSide.manufacturerMeta;

import com.mes.application.command.manufacturerMeta.AppManufacturerDeviceCfgService;
import com.mes.application.command.manufacturerMeta.AppManufacturerMetaService;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.enums.ManufacturerType;
import com.mes.interfaces.api.dto.req.manufacturerMeta.ManufacturerDeviceCfgRequest;
import com.mes.interfaces.api.dto.req.manufacturerMeta.ManufacturerMetaListRequest;
import com.mes.interfaces.api.dto.req.manufacturerMeta.ManufacturerMetaRequest;
import com.mes.interfaces.api.dto.resp.ApiResponse;
import com.mes.interfaces.api.dto.resp.PagedApiResponse;
import com.mes.interfaces.api.dto.resp.manufacturerMeta.DeviceCfgSummary;
import com.mes.interfaces.api.dto.resp.manufacturerMeta.ManufacturerMetaDetailResponse;
import com.mes.interfaces.api.dto.resp.manufacturerMeta.ManufacturerMetaListResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configSide/manufactureMeta")
public class ManufacturerMetaController {

    @Autowired
    private AppManufacturerMetaService appManufacturerMetaService;

    @Autowired
    private AppManufacturerDeviceCfgService appDeviceCfgService;

    /**
     * 分页查询制造商元数据
     * @param request 分页请求参数
     * @param name 制造商名称（可选）
     * @param manufacturerType 制造商类型（可选，不传则默认第一个类型）
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<ManufacturerMetaListResponse> listManufacturerMetas(
            @Valid @RequestBody ManufacturerMetaListRequest request) {
        
        // 参数验证已经在 PagedApiRequest 中处理
        
        // 转换为领域层查询对象
        PagedQuery query = request.toPagedQuery();
        String name = request.getName();
        String manufacturerType = request.getManufacturerType();
        
        // 如果没有传入类型，默认使用第一个类型
        if (manufacturerType == null || manufacturerType.trim().isEmpty()) {
            manufacturerType = ManufacturerType.values()[0].getCode();
        }
        
        // 调用应用服务查询数据
        PagedResult<ManufacturerMeta> result = appManufacturerMetaService.findManufacturerMetas(name, manufacturerType, query);
        
        // 转换为响应 DTO
        List<ManufacturerMetaListResponse> responses = result.items().stream()
                .map(ManufacturerMetaListResponse::from)
                .collect(Collectors.toList());
        
        // 返回分页响应
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    /**
     * 根据 ID 获取制造商详情（包含设备配置）
     * @param id 制造商 ID
     * @return 制造商详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ManufacturerMetaDetailResponse> getManufacturerMetaById(@PathVariable String id) {
        ManufacturerMeta manufacturerMeta = appManufacturerMetaService.findById(id);
        
        // 查询该制造商下的所有设备配置
        List<DeviceCfgSummary> deviceCfgs = null;
        if (manufacturerMeta != null && manufacturerMeta.getManufacturerMetaId() != null) {
            com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery query = 
                new com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery(1, 1000);
            com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult<ManufacturerDeviceCfg> result = 
                appDeviceCfgService.findDeviceCfgsByManufacturerId(manufacturerMeta.getManufacturerMetaId(), query);
            deviceCfgs = result.items().stream()
                .map(DeviceCfgSummary::from)
                .collect(Collectors.toList());
        }
        
        ManufacturerMetaDetailResponse response = ManufacturerMetaDetailResponse.from(manufacturerMeta, deviceCfgs);
        return ApiResponse.success(response);
    }

    /**
     * 新增制造商元数据（可同时添加设备配置）
     * @param request 新增请求参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> addManufacturerMeta(@Valid @RequestBody ManufacturerMetaAddRequest request) {
        // 转换并保存制造商信息
        ManufacturerMeta manufacturerMeta = request.toDomainEntity();
        appManufacturerMetaService.addManufacturerMeta(manufacturerMeta);
        
        // 如果包含设备配置，批量添加
        if (request.getDeviceCfgs() != null && !request.getDeviceCfgs().isEmpty()) {
            for (ManufacturerDeviceCfgRequest deviceCfgRequest : request.getDeviceCfgs()) {
                ManufacturerDeviceCfg deviceCfg = deviceCfgRequest.toDomainEntity();
                deviceCfg.setManufacturerMetaId(manufacturerMeta.getManufacturerMetaId());
                appDeviceCfgService.addDeviceCfg(deviceCfg);
            }
        }
        
        return ApiResponse.success("success");
    }

    /**
     * 编辑制造商元数据
     * @param request 编辑请求参数
     * @return 操作结果
     */
    @PutMapping("/edit")
    public ApiResponse<String> updateManufacturerMeta(@Valid @RequestBody ManufacturerMetaRequest request) {
        // 先查询现有数据
        ManufacturerMeta existingMeta = appManufacturerMetaService.findById(request.getId());
        if (existingMeta == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "制造商不存在");
        }

        // 使用 Request 中的转换方法更新所有字段
        ManufacturerMeta updatedMeta = request.toDomainEntity();

        // 保留原有 ID 和其他不可更改的字段
        updatedMeta.setId(existingMeta.getId());
        updatedMeta.setCreateTime(existingMeta.getCreateTime());

        // 调用应用服务更新
        appManufacturerMetaService.updateManufacturerMeta(updatedMeta);

        return ApiResponse.success("success");
    }

    /**
     * 删除制造商元数据
     * @param id 制造商 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteManufacturerMeta(@PathVariable String id) {
        appManufacturerMetaService.deleteManufacturerMeta(id);
        return ApiResponse.success("success");
    }

    /**
     * 获取所有制造商类型
     * @return 制造商类型列表
     */
    @GetMapping("/types")
    public ApiResponse<List<ManufacturerTypeVO>> getAllManufacturerTypes() {
        List<ManufacturerTypeVO> typeVOs = Arrays.stream(ManufacturerType.values())
                .map(type -> new ManufacturerTypeVO(type.getCode(), type.getDescription()))
                .collect(Collectors.toList());
        
        return ApiResponse.success(typeVOs);
    }

    /**
     * 查询工厂模板列表（返回 manufacturerMetaId 和 manufacturerMetaName）
     * @param manufacturerType 制造商类型（可选）
     * @return 工厂模板列表
     */
    @GetMapping("/templateList")
    public ApiResponse<List<ManufacturerTemplateVO>> getManufacturerTemplateList(
            @RequestParam(required = false) String manufacturerType) {
        
        // 生成假数据
        List<ManufacturerTemplateVO> templateList = new ArrayList<>();
        
            templateList.add(new ManufacturerTemplateVO("META_001", "华东工厂模板"));
            templateList.add(new ManufacturerTemplateVO("META_002", "华南工厂模板"));
            templateList.add(new ManufacturerTemplateVO("META_003", "华北工厂模板"));
            templateList.add(new ManufacturerTemplateVO("META_004", "华西工厂模板"));
            templateList.add(new ManufacturerTemplateVO("META_005", "华中工厂模板"));
        return ApiResponse.success(templateList);
    }

    /**
     * 制造商模板 VO
     */
    @Data
    @RequiredArgsConstructor
    public static class ManufacturerTemplateVO {
        private final String manufacturerMetaId;
        private final String manufacturerMetaName;
    }

    /**
     * 制造商新增请求（包含设备配置）
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ManufacturerMetaAddRequest extends ManufacturerMetaRequest {
        private List<ManufacturerDeviceCfgRequest> deviceCfgs;
    }
}
