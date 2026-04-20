package com.mes.domain.manufacturer.manufacturerMeta.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ManufacturerDeviceCfgService {

    @Autowired
    private ManufacturerDeviceCfgRepository manufacturerDeviceCfgRepository;

    /**
     * 根据制造商 ID 查询设备配置列表（支持分页）
     * @param manufacturerMetaId 制造商 ID
     * @param current 当前页码
     * @param size 每页大小
     * @return 设备配置列表
     */
    public List<ManufacturerDeviceCfg> findDeviceCfgsByManufacturerId(String manufacturerMetaId, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("manufacturerMetaId", manufacturerMetaId);
        return manufacturerDeviceCfgRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 根据制造商 ID 获取设备配置总数
     * @param manufacturerMetaId 制造商 ID
     * @return 总数
     */
    public long getTotalCountByManufacturerId(String manufacturerMetaId) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            return 0;
        }
        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("manufacturerMetaId", manufacturerMetaId);
        return manufacturerDeviceCfgRepository.totalByFuzzySearch(searchFilters);
    }

    /**
     * 添加设备配置
     * @param deviceCfg 设备配置实体
     * @return 添加后的实体
     */
    public ManufacturerDeviceCfg addDeviceCfg(ManufacturerDeviceCfg deviceCfg) {
        if (deviceCfg == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备配置不能为空");
        }
        if (StringUtils.isBlank(deviceCfg.getManufacturerMetaId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(deviceCfg.getDeviceInfoId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备 ID 不能为空");
        }
        if (StringUtils.isBlank(deviceCfg.getDeviceName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备名称不能为空");
        }
        
        // 设置默认状态为 NORMAL
        if (deviceCfg.getStatus() == null) {
            deviceCfg.setStatus(CfgStatus.NORMAL);
        }
        
        return manufacturerDeviceCfgRepository.add(deviceCfg);
    }

    /**
     * 更新设备配置
     * @param deviceCfg 设备配置实体
     */
    public void updateDeviceCfg(ManufacturerDeviceCfg deviceCfg) {
        if (deviceCfg == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备配置不能为空");
        }
        if (StringUtils.isBlank(deviceCfg.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备 ID 不能为空");
        }
        if (StringUtils.isBlank(deviceCfg.getDeviceName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "设备名称不能为空");
        }
        
        manufacturerDeviceCfgRepository.update(deviceCfg);
    }

    /**
     * 删除设备配置
     * @param id 设备 ID
     */
    public void deleteDeviceCfg(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        
        ManufacturerDeviceCfg deviceCfg = manufacturerDeviceCfgRepository.findById(id);
        if (deviceCfg != null) {
            manufacturerDeviceCfgRepository.delete(deviceCfg);
        }
    }

    /**
     * 根据 ID 获取设备配置
     * @param id 设备 ID
     * @return 设备配置
     */
    public ManufacturerDeviceCfg findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        return manufacturerDeviceCfgRepository.findById(id);
    }
}
