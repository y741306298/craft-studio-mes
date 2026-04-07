package com.mes.interfaces.api.platform.manufacturerSide.manufacturerCfg;

import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.command.api.req.ConfigMTSProductSpecRequest;
import com.mes.application.command.api.resp.MtsProductCategoryResponse;
import com.mes.application.command.api.resp.MtsProductListResponse;
import com.mes.application.command.api.resp.MtsProductSpecResponse;

import com.mes.application.dto.resp.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manufacturerSide/mtsProductCfg")
public class ManufacturerMtsProductCfgController {

    @Autowired
    private ProductCoreApiService productApiService;

    /**
     * 根据父分类 ID 查询成品商品分类列表
     * @param parentId 父分类 ID，null 表示查询首级分类
     * @return 分类列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<MtsProductCategoryResponse>> findCategoriesByParentId(
            @RequestParam(required = false) String parentId) {
        
        List<MtsProductCategoryResponse> categories = productApiService.findCategoriesByParentId(parentId);
        return ApiResponse.success(categories);
    }

    /**
     * 分页查询成品商品列表
     * @param rmfId 工厂 ID，不能为空
     * @param categoryId 产品分类 ID，可为空
     * @param current 当前页码，从 1 开始
     * @param size 每页大小
     * @return 分页查询结果
     */
    @GetMapping("/products")
    public PagedApiResponse<MtsProductListResponse> findMTSProducts(
            @RequestParam String rmfId,
            @RequestParam(required = false) String categoryId,
            @RequestParam int current,
            @RequestParam int size) {
        
        ProductCoreApiService.PagedResult<MtsProductListResponse> result =
            productApiService.findMTSProducts(rmfId, categoryId, current, size);
        
        return PagedApiResponse.success(result.getItems(), result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 分页查询成品商品规格列表
     * @param rmfId 工厂 ID，不能为空
     * @param productId 成品商品 ID，不能为空
     * @param current 当前页码，从 1 开始
     * @param size 每页大小
     * @return 分页查询结果
     */
    @GetMapping("/product-specs")
    public PagedApiResponse<MtsProductSpecResponse> findMTSProductSpecs(
            @RequestParam String rmfId,
            @RequestParam String productId,
            @RequestParam int current,
            @RequestParam int size) {
        
        ProductCoreApiService.PagedResult<MtsProductSpecResponse> result =
            productApiService.findMTSProductSpecs(rmfId, productId, current, size);
        
        return PagedApiResponse.success(result.getItems(), result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 配置成品商品规格
     * @param request 配置请求参数
     * @return 操作结果
     */
    @PostMapping("/product-specs/config")
    public ApiResponse<String> configMTSProductSpec(@Valid @RequestBody ConfigMTSProductSpecRequest request) {
        
        productApiService.configMTSProductSpec(
            request.getRmfId(),
            request.getMtsProductSpecId(),
            request.getStock(),
            request.getUnitPrice(),
            request.getPrice()
        );
        return ApiResponse.success("success");
    }

}
