package com.mes.interfaces.api.platform.configSide.manufacturerMeta;

import com.mes.application.command.manufacturerMeta.AppManufacturerMtsProductCfgService;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity.ManufacturerMtsProductCfg;
import com.mes.interfaces.api.dto.req.manufacturerMeta.ManufacturerMtsProductCfgListRequest;
import com.mes.interfaces.api.dto.req.manufacturerMeta.UpdateSpecPriceRequest;
import com.mes.interfaces.api.dto.resp.ApiResponse;
import com.mes.interfaces.api.dto.resp.PagedApiResponse;
import com.mes.interfaces.api.dto.resp.manufacturerMeta.ManufacturerMtsProductCfgListResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configSide/mtsProductCfg")
public class ManufacturerMtsProductCfgController {

    @Autowired
    private AppManufacturerMtsProductCfgService mtsProductCfgService;

    /**
     * 分页查询标品商品配置列表（从外部系统获取）
     * @param request 分页请求参数
     * @param manufacturerId 制造商 ID（必填）
     * @param productName 产品名称（可选）
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public ApiResponse<PagedApiResponse<ManufacturerMtsProductCfgListResponse>> findMtsProductCfgs(
            @Valid @RequestBody ManufacturerMtsProductCfgListRequest request) {
        
        PagedQuery query = request.toPagedQuery();
        String manufacturerId = request.getManufacturerId();
        String productName = request.getProductName();
        PagedResult<ManufacturerMtsProductCfg> result = mtsProductCfgService.findMtsProductCfgsByManufacturerId(
                manufacturerId, productName, query);
        
        // 转换为响应 DTO
        List<ManufacturerMtsProductCfgListResponse> responses = result.items().stream()
                .map(ManufacturerMtsProductCfgListResponse::from)
                .collect(Collectors.toList());
        
        return ApiResponse.success(PagedApiResponse.success(
            responses, 
            query.getCurrent(), 
            query.getSize(), 
            result.total()
        ));
    }

    /**
     * 更新标品规格价格
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PutMapping("/spec/price")
    public ApiResponse<String> updateSpecPrice(@Valid @RequestBody UpdateSpecPriceRequest request) {
        mtsProductCfgService.updateSpecPrice(
                request.getManufacturerId(),
                request.getProductId(),
                request.getSpecId(),
                request.getPrice());
        return ApiResponse.success("success");
    }

    /**
     * 逻辑删除标品规格
     * @param manufacturerId 制造商 ID
     * @param productId 产品 ID
     * @param specId 规格 ID
     * @return 操作结果
     */
    @DeleteMapping("/spec")
    public ApiResponse<String> deleteSpec(
            @RequestParam String manufacturerId,
            @RequestParam String productId,
            @RequestParam String specId) {
        mtsProductCfgService.deleteSpec(manufacturerId, productId, specId);
        return ApiResponse.success("success");
    }

    /**
     * 逻辑删除整个标品商品配置
     * @param manufacturerId 制造商 ID
     * @param productId 产品 ID
     * @return 操作结果
     */
    @DeleteMapping("/product")
    public ApiResponse<String> deleteProductCfg(
            @RequestParam String manufacturerId,
            @RequestParam String productId) {
        mtsProductCfgService.deleteProductCfg(manufacturerId, productId);
        return ApiResponse.success("success");
    }

}
