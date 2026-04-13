package com.mes.domain.manufacturer.device.service;

import com.mes.application.dto.resp.ApiResponse;
import com.mes.domain.manufacturer.device.entity.Device;
import com.mes.domain.manufacturer.device.repository.DeviceInfoRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.mes.domain.shared.utils.IdGenerator;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceService {

    @Autowired
    private DeviceInfoRepository deviceInfoRepository;

    /**
     * 根据设备名称查询设备（支持分页）
     * @param deviceName 设备名称
     * @param current 当前页码
     * @param size 每页大小
     * @return 设备列表
     */
    public List<Device> findDevicesByName(String deviceName, int current, int size) {
        // 参数验证

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(deviceName)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备名称不能为空");
        }

        // 根据 deviceName 进行模糊查询
        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("deviceInfoName", deviceName);
        return deviceInfoRepository.fuzzySearch(searchFilters, current, size);
    }

    public List<Device> findDevicesByCondition(String deviceName, String deviceType, int current, int size) {
        // 参数验证
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        Map<String, String> searchFilters = new HashMap<>();
        if (StringUtils.isNotBlank(deviceName)) {
            searchFilters.put("deviceInfoName", deviceName);
        }
        if (StringUtils.isNotBlank(deviceType)) {
            searchFilters.put("deviceType", deviceType);
        }
        
        return deviceInfoRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 获取设备总数
     * @param deviceName 设备名称，可为空
     * @return 总数
     */
    public long getTotalCount(String deviceName) {
        if (StringUtils.isNotBlank(deviceName)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("deviceInfoName", deviceName);
            return deviceInfoRepository.totalByFuzzySearch(searchFilters);
        } else {
            return deviceInfoRepository.total();
        }
    }

    public long getTotalCount(String deviceName, String deviceType) {
        if (StringUtils.isNotBlank(deviceName) || StringUtils.isNotBlank(deviceType)) {
            Map<String, String> searchFilters = new HashMap<>();
            if (StringUtils.isNotBlank(deviceName)) {
                searchFilters.put("deviceInfoName", deviceName);
            }
            if (StringUtils.isNotBlank(deviceType)) {
                searchFilters.put("deviceType", deviceType);
            }
            return deviceInfoRepository.totalByFuzzySearch(searchFilters);
        } else {
            return deviceInfoRepository.total();
        }
    }

    /**
     * 添加设备
     * @param device 设备实体
     * @return 添加后的实体
     */
    public Device addDevice(Device device) {
        // 业务验证
        if (device == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备不能为空");
        }
        if (StringUtils.isBlank(device.getDeviceInfoName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备名称不能为空");
        }
        
        // 生成唯一的 deviceInfoId
        String deviceInfoId = IdGenerator.generateId("DEV");
        device.setDeviceInfoId(deviceInfoId);
        
        return deviceInfoRepository.add(device);
    }

    /**
     * 更新设备
     * @param device 设备实体
     */
    public void updateDevice(Device device) {
        // 业务验证
        if (device == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备不能为空");
        }
        if (StringUtils.isBlank(device.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备ID不能为空");
        }
        if (StringUtils.isBlank(device.getDeviceInfoName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备名称不能为空");
        }
        
        deviceInfoRepository.update(device);
    }

    /**
     * 删除设备
     * @param id 设备ID
     */
    public void deleteDevice(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }
        
        Device device = deviceInfoRepository.findById(id);
        if (device != null) {
            deviceInfoRepository.delete(device);
        }
    }

    /**
     * 根据ID获取设备
     * @param id 设备ID
     * @return 设备实体
     */
    public Device findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }
        return deviceInfoRepository.findById(id);
    }
}
