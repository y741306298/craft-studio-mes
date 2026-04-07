package com.mes.domain.manufacturer.manufacturerMtsProductCfg.service;

import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity.ManufacturerMtsProductCfg;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.repository.ManufacturerMtsProductCfgRepository;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.vo.ManufacturerMtsProductSpec;
import com.mes.domain.shared.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManufacturerMtsProductCfgService {

    private final ManufacturerMtsProductCfgRepository repository;

    public ManufacturerMtsProductCfgService(ManufacturerMtsProductCfgRepository repository) {
        this.repository = repository;
    }

    /**
     * 根据制造商 ID 和产品名称分页查询标品商品配置（从外部系统获取）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productName 产品名称，可为空
     * @param current 当前页码，从 1 开始，必须大于 0
     * @param size 每页大小，范围 1-100
     * @return 标品商品配置列表
     * @throws BusinessNotAllowException 当参数不合法或外部系统调用失败时抛出此异常
     */
    public List<ManufacturerMtsProductCfg> findByManufacturerIdAndProductName(String manufacturerId, String productName, int current, int size) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException("制造商 ID 不能为空");
        }
        if (current <= 0) {
            throw new BusinessNotAllowException("当前页码必须大于 0");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }

        // TODO: 需要在 application 层调用 ProductApiService
        throw new UnsupportedOperationException("此方法已移至 ProductApiService");
    }

    /**
     * 根据 ID 查询标品商品配置
     */
    public ManufacturerMtsProductCfg findById(String id) {
        return repository.findById(id);
    }

    /**
     * 保存标品商品配置
     */
    public ManufacturerMtsProductCfg save(ManufacturerMtsProductCfg cfg) {
        return repository.add(cfg);
    }

    /**
     * 更新标品商品配置
     */
    public void update(ManufacturerMtsProductCfg cfg) {
        repository.update(cfg);
    }

    /**
     * 删除标品商品配置
     */
    public void delete(ManufacturerMtsProductCfg cfg) {
        repository.delete(cfg);
    }

    /**
     * 根据制造商 ID 查询标品商品配置列表（从数据库获取）
     */
    public List<ManufacturerMtsProductCfg> listByManufacturerId(String manufacturerId, long current, int size) {
        return repository.filterList(current, size, java.util.Map.of("manufacturerId", manufacturerId));
    }

    /**
     * 批量保存标品商品配置
     */
    public java.util.Collection<ManufacturerMtsProductCfg> batchSave(List<ManufacturerMtsProductCfg> items) {
        return repository.batchAdd(items);
    }

    /**
     * 根据 manufacturerId、productId 和 specId 更新标品规格的价格
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productId 产品 ID，不能为空
     * @param specId 规格 ID，不能为空
     * @param price 新的价格信息，不能为空
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void updateSpecPrice(String manufacturerId, String productId, String specId, UnitPrice price) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException("制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(productId)) {
            throw new BusinessNotAllowException("产品 ID 不能为空");
        }
        if (StringUtils.isBlank(specId)) {
            throw new BusinessNotAllowException("规格 ID 不能为空");
        }
        if (price == null || price.getPrice() == null) {
            throw new BusinessNotAllowException("价格不能为空");
        }

        List<ManufacturerMtsProductCfg> cfgList = repository.filterList(1, 1, java.util.Map.of(
            "manufacturerId", manufacturerId,
            "productId", productId
        ));
        ManufacturerMtsProductCfg cfg = cfgList.isEmpty() ? null : cfgList.get(0);
        
        if (cfg == null) {
            throw new BusinessNotAllowException("未找到对应的标品商品配置");
        }

        List<ManufacturerMtsProductSpec> specs = cfg.getMtsProductSpecs();
        if (specs == null || specs.isEmpty()) {
            throw new BusinessNotAllowException("该商品没有配置规格");
        }

        boolean found = false;
        for (ManufacturerMtsProductSpec spec : specs) {
            if (specId.equals(spec.getId())) {
                spec.setPrice(price);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new BusinessNotAllowException("未找到对应的规格");
        }

        repository.update(cfg);
    }

    /**
     * 逻辑删除标品规格（将规格的 status 改为 INVALID）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productId 产品 ID，不能为空
     * @param specId 规格 ID，不能为空
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void deleteSpec(String manufacturerId, String productId, String specId) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException("制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(productId)) {
            throw new BusinessNotAllowException("产品 ID 不能为空");
        }
        if (StringUtils.isBlank(specId)) {
            throw new BusinessNotAllowException("规格 ID 不能为空");
        }

        List<ManufacturerMtsProductCfg> cfgList = repository.filterList(1, 1, java.util.Map.of(
            "manufacturerId", manufacturerId,
            "productId", productId
        ));
        ManufacturerMtsProductCfg cfg = cfgList.isEmpty() ? null : cfgList.get(0);
        
        if (cfg == null) {
            throw new BusinessNotAllowException("未找到对应的标品商品配置");
        }

        List<ManufacturerMtsProductSpec> specs = cfg.getMtsProductSpecs();
        if (specs == null || specs.isEmpty()) {
            throw new BusinessNotAllowException("该商品没有配置规格");
        }

        boolean found = false;
        for (ManufacturerMtsProductSpec spec : specs) {
            if (specId.equals(spec.getId())) {
                spec.setStatus(CfgStatus.INVALID);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new BusinessNotAllowException("未找到对应的规格");
        }

        repository.update(cfg);
    }

    /**
     * 逻辑删除整个标品商品配置（将 status 改为 INVALID）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productId 产品 ID，不能为空
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void deleteProductCfg(String manufacturerId, String productId) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException("制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(productId)) {
            throw new BusinessNotAllowException("产品 ID 不能为空");
        }

        List<ManufacturerMtsProductCfg> cfgList = repository.filterList(1, 1, java.util.Map.of(
            "manufacturerId", manufacturerId,
            "productId", productId
        ));
        ManufacturerMtsProductCfg cfg = cfgList.isEmpty() ? null : cfgList.get(0);
        
        if (cfg == null) {
            throw new BusinessNotAllowException("未找到对应的标品商品配置");
        }

        cfg.setStatus(CfgStatus.INVALID);
        repository.update(cfg);
    }
}
