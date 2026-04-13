package com.mes.domain.manufacturer.manufacturerProcessPriceCfg.service;

import com.mes.application.dto.resp.ApiResponse;
import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity.ManufacturerProcessPriceCfg;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.repository.ManufacturerProcessPriceCfgRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManufacturerProcessPriceCfgService {

    private final ManufacturerProcessPriceCfgRepository repository;

    public ManufacturerProcessPriceCfgService(ManufacturerProcessPriceCfgRepository repository) {
        this.repository = repository;
    }

    /**
     * 根据制造商 ID 查询工艺价格配置列表（从外部系统获取）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param current 当前页码，从 1 开始，必须大于 0
     * @param size 每页大小，范围 1-100
     * @return 工艺价格配置列表
     * @throws BusinessNotAllowException 当参数不合法或外部系统调用失败时抛出此异常
     */
    public List<ManufacturerProcessPriceCfg> findByManufacturerId(String manufacturerId, int current, int size) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        // TODO: 需要在 application 层调用 ProductApiService
        throw new UnsupportedOperationException("此方法已移至 ProductApiService");
    }

    /**
     * 根据 ID 查询工艺价格配置
     */
    public ManufacturerProcessPriceCfg findById(String id) {
        return repository.findById(id);
    }

    /**
     * 保存工艺价格配置
     */
    public ManufacturerProcessPriceCfg save(ManufacturerProcessPriceCfg cfg) {
        return repository.add(cfg);
    }

    /**
     * 更新工艺价格配置
     */
    public void update(ManufacturerProcessPriceCfg cfg) {
        repository.update(cfg);
    }

    /**
     * 删除工艺价格配置
     */
    public void delete(ManufacturerProcessPriceCfg cfg) {
        repository.delete(cfg);
    }

    /**
     * 根据制造商 ID 查询工艺价格配置列表（从数据库获取）
     */
    public List<ManufacturerProcessPriceCfg> listByManufacturerId(String manufacturerId, long current, int size) {
        return repository.filterList(current, size, java.util.Map.of("manufacturerId", manufacturerId));
    }

    /**
     * 批量保存工艺价格配置
     */
    public java.util.Collection<ManufacturerProcessPriceCfg> batchSave(List<ManufacturerProcessPriceCfg> items) {
        return repository.batchAdd(items);
    }
}
