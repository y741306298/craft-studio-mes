package com.mes.application.command.manufacturerMeta;

import com.mes.application.dto.resp.ApiResponse;
import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.command.api.resp.MtsProductCategoryResponse;
import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity.ManufacturerMtsProductCfg;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.service.ManufacturerMtsProductCfgService;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppManufacturerMtsProductCfgService {

    private final ManufacturerMtsProductCfgService mtsProductCfgService;
    private final ProductCoreApiService productApiService;

    public AppManufacturerMtsProductCfgService(ManufacturerMtsProductCfgService mtsProductCfgService, ProductCoreApiService productApiService) {
        this.mtsProductCfgService = mtsProductCfgService;
        this.productApiService = productApiService;
    }

    /**
     * 分页获取某工厂的标品商品配置（从外部系统获取并转换为 ManufacturerMtsProductCfg）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productName 产品名称，可为空
     * @param query 分页参数，不能为空
     * @return 标品商品配置列表（包含 total 总数）
     * @throws BusinessNotAllowException 当参数不合法时抛出此异常
     */
    public PagedResult<ManufacturerMtsProductCfg> findMtsProductCfgsByManufacturerId(String manufacturerId, String productName, PagedQuery query) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }
        if (query == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        List<ManufacturerMtsProductCfg> items = productApiService.findMtsProductCfgsByManufacturerId(
                manufacturerId, productName, (int) query.getCurrent(), query.getSize());
        
        long total = items.size();

        return new PagedResult<>(items, total, query.getSize(), query.getCurrent());
    }

    /**
     * 根据 manufacturerId、productId 和 specId 更新规格价格
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productId 产品 ID，不能为空
     * @param specId 规格 ID，不能为空
     * @param price 新的价格信息，不能为空
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void updateSpecPrice(String manufacturerId, String productId, String specId, UnitPrice price) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(productId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "产品 ID 不能为空");
        }
        if (StringUtils.isBlank(specId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "规格 ID 不能为空");
        }
        if (price == null || price.getPrice() == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "价格不能为空");
        }

        mtsProductCfgService.updateSpecPrice(manufacturerId, productId, specId, price);
    }

    /**
     * 逻辑删除某个规格（将规格的 status 改为 INVALID）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productId 产品 ID，不能为空
     * @param specId 规格 ID，不能为空
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void deleteSpec(String manufacturerId, String productId, String specId) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(productId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "产品 ID 不能为空");
        }
        if (StringUtils.isBlank(specId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "规格 ID 不能为空");
        }

        mtsProductCfgService.deleteSpec(manufacturerId, productId, specId);
    }

    /**
     * 逻辑删除整个标品商品配置（将配置的 status 改为 INVALID）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productId 产品 ID，不能为空
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void deleteProductCfg(String manufacturerId, String productId) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(productId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "产品 ID 不能为空");
        }

        mtsProductCfgService.deleteProductCfg(manufacturerId, productId);
    }

    /**
     * 根据父分类 ID 查询成品商品分类列表
     * @param parentId 父分类 ID，null 表示查询首级分类
     * @return 分类列表
     */
    public List<MtsProductCategoryResponse> findCategoriesByParentId(String parentId) {
        return productApiService.findCategoriesByParentId(parentId);
    }
}
