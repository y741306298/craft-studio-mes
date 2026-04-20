package com.mes.domain.manufacturer.manufacturerMeta.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerProductionLineMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerWorkshopMeta;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerMetaRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.mes.domain.shared.utils.IdGenerator;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ManufacturerMetaService {

    @Autowired
    private ManufacturerMetaRepository manufacturerMetaRepository;

    /**
     * 根据条件查询制造商元数据（支持分页）
     * @param name 制造商名称，可为空
     * @param manufacturerType 制造商类型，可为空
     * @param current 当前页码
     * @param size 每页大小
     * @return 制造商元数据列表
     */
    public List<ManufacturerMeta> findManufacturerMetasByConditions(String name, String manufacturerType, int current, int size) {
        // 参数验证
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        // 构建查询条件
        Map<String, String> searchFilters = new HashMap<>();
        if (StringUtils.isNotBlank(name)) {
            searchFilters.put("name", name);
        }
        if (StringUtils.isNotBlank(manufacturerType)) {
            searchFilters.put("manufacturerMetaType", manufacturerType);
        }
        
        return manufacturerMetaRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 获取制造商元数据总数
     * @param name 制造商名称，可为空
     * @param manufacturerType 制造商类型，可为空
     * @return 总数
     */
    public long getTotalCount(String name, String manufacturerType) {
        Map<String, String> searchFilters = new HashMap<>();
        if (StringUtils.isNotBlank(name)) {
            searchFilters.put("name", name);
        }
        if (StringUtils.isNotBlank(manufacturerType)) {
            searchFilters.put("manufacturerMetaType", manufacturerType);
        }
        
        if (!searchFilters.isEmpty()) {
            return manufacturerMetaRepository.totalByFuzzySearch(searchFilters);
        } else {
            return manufacturerMetaRepository.total();
        }
    }

    /**
     * 添加制造商元数据
     * @param manufacturerMeta 制造商元数据实体
     * @return 添加后的实体
     */
    public ManufacturerMeta addManufacturerMeta(ManufacturerMeta manufacturerMeta) {
        // 业务验证
        if (manufacturerMeta == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商元数据不能为空");
        }
        if (StringUtils.isBlank(manufacturerMeta.getName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商名称不能为空");
        }
        
        // 生成唯一的 manufacturerMetaId
        String manufacturerMetaId = IdGenerator.generateManufacturerMetaId();
        manufacturerMeta.setManufacturerMetaId(manufacturerMetaId);
        
        // 设置默认状态为 NORMAL
        if (manufacturerMeta.getStatus() == null) {
            manufacturerMeta.setStatus(CfgStatus.NORMAL);
        }
        
        // 为车间和产线生成唯一 ID
        if (manufacturerMeta.getManufacturerWorkshopMetas() != null) {
            for (ManufacturerWorkshopMeta workshop : manufacturerMeta.getManufacturerWorkshopMetas()) {
                // 生成车间 ID
                if (StringUtils.isBlank(workshop.getWorkshopId())) {
                    String workshopId = IdGenerator.generateId("WORKSHOP");
                    workshop.setWorkshopId(workshopId);
                    workshop.setStatus(CfgStatus.NORMAL.getCode());
                }
                
                // 为每个车间的生产线生成 ID
                if (workshop.getManufacturerProductionLineMetas() != null) {
                    for (ManufacturerProductionLineMeta productionLine : workshop.getManufacturerProductionLineMetas()) {
                        if (StringUtils.isBlank(productionLine.getProductionLineId())) {
                            String productionLineId = IdGenerator.generateId("LINE");
                            productionLine.setProductionLineId(productionLineId);
                            productionLine.setStatus(CfgStatus.NORMAL.getCode());
                        }
                    }
                }
            }
        }
        return manufacturerMetaRepository.add(manufacturerMeta);
    }

    /**
     * 更新制造商元数据
     * @param manufacturerMeta 制造商元数据实体
     */
    public void updateManufacturerMeta(ManufacturerMeta manufacturerMeta) {
        // 业务验证
        if (manufacturerMeta == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商元数据不能为空");
        }
        if (StringUtils.isBlank(manufacturerMeta.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商ID不能为空");
        }
        if (StringUtils.isBlank(manufacturerMeta.getName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商名称不能为空");
        }
        
        manufacturerMetaRepository.update(manufacturerMeta);
    }

    /**
     * 删除制造商元数据
     * @param id 制造商ID
     */
    public void deleteManufacturerMeta(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }
        
        ManufacturerMeta manufacturerMeta = manufacturerMetaRepository.findById(id);
        if (manufacturerMeta != null) {
            manufacturerMetaRepository.delete(manufacturerMeta);
        }
    }

    /**
     * 根据ID获取制造商元数据
     * @param id 制造商ID
     * @return 制造商元数据
     */
    public ManufacturerMeta findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }
        return manufacturerMetaRepository.findById(id);
    }
}
