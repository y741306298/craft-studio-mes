package com.mes.interfaces.api.platform.configSide.device;

import com.mes.application.command.device.AppDeviceService;
import com.mes.application.dto.req.device.DeviceListRequest;
import com.mes.application.dto.req.device.DeviceRequest;
import com.mes.application.dto.resp.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.device.DeviceListResponse;
import com.mes.domain.manufacturer.device.entity.Device;
import com.mes.domain.manufacturer.device.enums.DeviceType;

import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configSide/device")
public class DeviceController {

    @Autowired
    private AppDeviceService appDeviceService;

    /**
     * 分页查询设备列表
     * @param request 分页请求参数
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<DeviceListResponse> listDevices(
            @Valid @RequestBody DeviceListRequest request) {
        
        PagedQuery query = request.toPagedQuery();
        String deviceName = request.getDeviceName();
        String deviceType = request.getDeviceType();
        
        PagedResult<Device> result = appDeviceService.findDevices(deviceName,deviceType, query);
        
        List<DeviceListResponse> responses = result.items().stream()
                .map(DeviceListResponse::from)
                .collect(Collectors.toList());
        
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    /**
     * 根据 ID 获取设备详情
     * @param id 设备 ID
     * @return 设备详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DeviceListResponse> getDeviceById(@PathVariable String id) {
        Device device = appDeviceService.findById(id);
        DeviceListResponse response = DeviceListResponse.from(device);
        return ApiResponse.success(response);
    }

    /**
     * 获取所有设备类型
     * @return 设备类型列表
     */
    @GetMapping("/types")
    public ApiResponse<List<DeviceTypeVO>> getAllDeviceTypes() {
        List<DeviceTypeVO> typeVOs = Arrays.stream(DeviceType.values())
                .map(type -> new DeviceTypeVO(type.getCode(), type.getChineseName()))
                .collect(Collectors.toList());
        
        return ApiResponse.success(typeVOs);
    }

    /**
     * 新增设备
     * @param request 新增请求参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> addDevice(@Valid @RequestBody DeviceRequest request) {
        Device device = request.toDomainEntity();
        appDeviceService.addDevice(device);
        
        return ApiResponse.success("success");
    }

    /**
     * 编辑设备
     * @param request 编辑请求参数
     * @return 操作结果
     */
    @PostMapping("/edit")
    public ApiResponse<String> updateDevice(@Valid @RequestBody DeviceRequest request) {
        Device existingDevice = appDeviceService.findById(request.getId());
        if (existingDevice == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "设备不存在");
        }

        Device updatedDevice = request.toDomainEntity();
        updatedDevice.setId(existingDevice.getId());
        updatedDevice.setCreateTime(existingDevice.getCreateTime());

        appDeviceService.updateDevice(updatedDevice);

        return ApiResponse.success("success");
    }

    /**
     * 删除设备
     * @param id 设备 ID
     * @return 操作结果
     */
    @GetMapping("/delete/{id}")
    public ApiResponse<String> deleteDevice(@PathVariable String id) {
        appDeviceService.deleteDevice(id);
        return ApiResponse.success("success");
    }

    /**
     * 设备类型 VO
     */
    public static class DeviceTypeVO {
        private String code;
        private String name;

        public DeviceTypeVO() {
        }

        public DeviceTypeVO(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
