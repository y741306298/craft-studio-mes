package com.mes.domain.manufacturer.manufacturerMeta.service;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerMetaRepository;
import com.mes.domain.shared.exception.BusinessNotAllowException;
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
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
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
            throw new BusinessNotAllowException("制造商元数据不能为空");
        }
        if (StringUtils.isBlank(manufacturerMeta.getName())) {
            throw new BusinessNotAllowException("制造商名称不能为空");
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
            throw new BusinessNotAllowException("制造商元数据不能为空");
        }
        if (StringUtils.isBlank(manufacturerMeta.getId())) {
            throw new BusinessNotAllowException("制造商ID不能为空");
        }
        if (StringUtils.isBlank(manufacturerMeta.getName())) {
            throw new BusinessNotAllowException("制造商名称不能为空");
        }
        
        manufacturerMetaRepository.update(manufacturerMeta);
    }

    /**
     * 删除制造商元数据
     * @param id 制造商ID
     */
    public void deleteManufacturerMeta(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("ID不能为空");
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
            throw new BusinessNotAllowException("ID不能为空");
        }
        return manufacturerMetaRepository.findById(id);
    }
}
