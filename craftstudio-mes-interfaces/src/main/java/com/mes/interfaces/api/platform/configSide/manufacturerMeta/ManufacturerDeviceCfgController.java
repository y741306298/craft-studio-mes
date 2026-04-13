package com.mes.interfaces.api.platform.configSide.manufacturerMeta;

import com.mes.application.command.device.AppDeviceService;
import com.mes.application.command.manufacturerMeta.AppManufacturerDeviceCfgService;
import com.mes.application.dto.req.manufacturerMeta.ManufacturerDeviceCfgListRequest;
import com.mes.application.dto.req.manufacturerMeta.ManufacturerDeviceCfgRequest;
import com.mes.application.dto.resp.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.manufacturerMeta.DeviceCfgSummary;
import com.mes.domain.manufacturer.device.entity.Device;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configSide/deviceCfg")
public class ManufacturerDeviceCfgController {

    @Autowired
    private AppManufacturerDeviceCfgService appDeviceCfgService;

    @Autowired
    private AppDeviceService appDeviceService;

    /**
     * 分页查询设备配置列表（根据制造商 ID）
     * @param request 分页请求参数
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<DeviceCfgSummary> listDeviceCfgs(
            @Valid @RequestBody ManufacturerDeviceCfgListRequest request) {

        PagedQuery query = request.toPagedQuery();
        String manufacturerMetaId = request.getManufacturerMetaId();
        PagedResult<ManufacturerDeviceCfg> result = appDeviceCfgService.findDeviceCfgsByManufacturerId(manufacturerMetaId, query);
        Collection<ManufacturerDeviceCfg> items = result.items();
        List<DeviceCfgSummary> responses = new ArrayList<DeviceCfgSummary>();
        for (ManufacturerDeviceCfg item : items) {
            String deviceId = item.getDeviceId();
            DeviceCfgSummary summary = DeviceCfgSummary.from(item);
            Device byDeviceInfoId = appDeviceService.findByDeviceInfoId(deviceId);
            summary.setBrand(byDeviceInfoId.getBrand());
            summary.setDeviceProcedures(byDeviceInfoId.getDeviceProcedures());
            responses.add(summary);
        }
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }
    /**
     * 根据 ID 获取设备配置详情
     * @param id 设备 ID
     * @return 设备配置详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DeviceCfgSummary> getDeviceCfgById(@PathVariable String id) {
        ManufacturerDeviceCfg deviceCfg = appDeviceCfgService.findById(id);
        DeviceCfgSummary response = DeviceCfgSummary.from(deviceCfg);
        return ApiResponse.success(response);
    }

    /**
     * 新增设备配置
     * @param request 新增请求参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> addDeviceCfg(@Valid @RequestBody ManufacturerDeviceCfgRequest request) {
        ManufacturerDeviceCfg deviceCfg = request.toDomainEntity();
        appDeviceCfgService.addDeviceCfg(deviceCfg);
        return ApiResponse.success("success");
    }

    /**
     * 编辑设备配置
     * @param request 编辑请求参数
     * @return 操作结果
     */
    @PutMapping("/edit")
    public ApiResponse<String> updateDeviceCfg(@Valid @RequestBody ManufacturerDeviceCfgRequest request) {
        ManufacturerDeviceCfg existingCfg = appDeviceCfgService.findById(request.getId());
        if (existingCfg == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "设备配置不存在");
        }

        ManufacturerDeviceCfg updatedCfg = request.toDomainEntity();
        updatedCfg.setId(existingCfg.getId());
        updatedCfg.setCreateTime(existingCfg.getCreateTime());

        appDeviceCfgService.updateDeviceCfg(updatedCfg);
        return ApiResponse.success("success");
    }

    /**
     * 删除设备配置
     * @param id 设备 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteDeviceCfg(@PathVariable String id) {
        appDeviceCfgService.deleteDeviceCfg(id);
        return ApiResponse.success("success");
    }
}
