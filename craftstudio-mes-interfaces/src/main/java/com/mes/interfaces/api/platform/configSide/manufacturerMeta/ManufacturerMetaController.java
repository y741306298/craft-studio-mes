package com.mes.interfaces.api.platform.configSide.manufacturerMeta;

import com.mes.application.command.manufacturerMeta.AppManufacturerDeviceCfgService;
import com.mes.application.command.manufacturerMeta.AppManufacturerMetaService;
import com.mes.application.dto.req.manufacturerMeta.ManufacturerDeviceCfgRequest;
import com.mes.application.dto.req.manufacturerMeta.ManufacturerMetaListRequest;
import com.mes.application.dto.req.manufacturerMeta.ManufacturerMetaRequest;
import com.mes.application.dto.req.manufacturerMeta.WorkshopRequest;
import com.mes.application.dto.resp.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.manufacturerMeta.DeviceCfgSummary;
import com.mes.application.dto.resp.manufacturerMeta.ManufacturerMetaDetailResponse;
import com.mes.application.dto.resp.manufacturerMeta.ManufacturerMetaListResponse;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerWorkshopMeta;
import com.mes.domain.manufacturer.manufacturerMeta.enums.ManufacturerType;
import com.mes.domain.shared.enums.ProductUnit;

import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
        
        // 调用应用服务查询数据（包含设备数量）
        PagedResult<AppManufacturerMetaService.ManufacturerMetaWithDeviceCount> result = 
            appManufacturerMetaService.findManufacturerMetasWithDeviceCount(name, manufacturerType, query);
        
        // 转换为响应 DTO
        List<ManufacturerMetaListResponse> responses = result.items().stream()
                .map(item -> {
                    ManufacturerMetaListResponse response = ManufacturerMetaListResponse.from(item.getManufacturerMeta());
                    response.setDeviceCount(item.getDeviceCount());
                    return response;
                })
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
                new com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery(1, 100);
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
     * 为工厂添加车间信息
     * @param request 添加车间请求
     * @return 操作结果
     */
    @PostMapping("/workshops/add")
    public ApiResponse<String> addWorkshopsForManufacturer(
            @Valid @RequestBody AddWorkshopsRequest request) {
        
        if (StringUtils.isBlank(request.getManufacturerMetaId())) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }
        
        if (request.getWorkshops() == null || request.getWorkshops().isEmpty()) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "车间列表不能为空");
        }
        
        // 将 WorkshopRequest 转换为 ManufacturerWorkshopMeta
        List<ManufacturerWorkshopMeta> workshopMetas = new ArrayList<>();
        for (WorkshopRequest workshopRequest : request.getWorkshops()) {
            ManufacturerWorkshopMeta workshopMeta = workshopRequest.toDomainEntity();
            workshopMetas.add(workshopMeta);
        }
        
        // 调用应用服务添加车间
        appManufacturerMetaService.addWorkshopsForManufacturer(request.getManufacturerMetaId(), workshopMetas);
        
        return ApiResponse.success("success");
    }

    /**
     * 编辑制造商元数据
     * @param request 编辑请求参数
     * @return 操作结果
     */
    @PostMapping("/edit")
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
     * 获取所有产品单位列表
     * @param unitType 单位类型（可选），如：面积单位、体积单位、长度单位等
     * @return 产品单位列表
     */
    @GetMapping("/productUnits")
    public ApiResponse<List<ProductUnitVO>> getProductUnits(
            @RequestParam(required = false) String unitType) {
        
        List<ProductUnitVO> unitList;
        
        if (unitType == null || unitType.trim().isEmpty()) {
            // 返回所有单位
            unitList = Arrays.stream(ProductUnit.values())
                    .map(unit -> new ProductUnitVO(
                            unit.getChineseName(),
                            unit.getSymbol(),
                            unit.getUnitType()
                    ))
                    .collect(Collectors.toList());
        } else {
            // 根据单位类型过滤
            unitList = Arrays.stream(ProductUnit.values())
                    .filter(unit -> unit.getUnitType().equals(unitType))
                    .map(unit -> new ProductUnitVO(
                            unit.getChineseName(),
                            unit.getSymbol(),
                            unit.getUnitType()
                    ))
                    .collect(Collectors.toList());
        }
        
        return ApiResponse.success(unitList);
    }

    /**
     * 获取所有单位类型列表
     * @return 单位类型列表
     */
    @GetMapping("/unitTypes")
    public ApiResponse<List<String>> getUnitTypes() {
        List<String> unitTypes = Arrays.stream(ProductUnit.values())
                .map(ProductUnit::getUnitType)
                .distinct()
                .collect(Collectors.toList());
        
        return ApiResponse.success(unitTypes);
    }

    /**
     * 制造商模板 VO
     */
    @Data
    @RequiredArgsConstructor
    public static class ManufacturerTemplateVO {
        private final String manufacturerTempId;
        private final String manufacturerTempName;
    }

    /**
     * 产品单位 VO
     */
    @Data
    @RequiredArgsConstructor
    public static class ProductUnitVO {
        private final String chineseName;
        private final String symbol;
        private final String unitType;
    }

    /**
     * 制造商新增请求（包含设备配置）
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ManufacturerMetaAddRequest extends ManufacturerMetaRequest {

        private List<ManufacturerDeviceCfgRequest> deviceCfgs;
    }
    
    /**
     * 为工厂添加车间请求
     */
    @Data
    public static class AddWorkshopsRequest {
        
        @NotBlank(message = "制造商 ID 不能为空")
        private String manufacturerMetaId;
        
        @Valid
        private List<WorkshopRequest> workshops;
    }


}
