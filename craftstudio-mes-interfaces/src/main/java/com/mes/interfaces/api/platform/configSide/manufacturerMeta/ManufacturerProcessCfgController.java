package com.mes.interfaces.api.platform.configSide.manufacturerMeta;

import com.mes.application.command.manufacturerMeta.AppManufacturerProcessCfgService;
import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity.ManufacturerProcessPriceCfg;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.vo.MaterialProcessPrice;
import com.mes.interfaces.api.dto.req.base.PagedApiRequest;
import com.mes.interfaces.api.dto.req.manufacturerMeta.UpdateProcessPriceRequest;
import com.mes.interfaces.api.dto.resp.ApiResponse;
import com.mes.interfaces.api.dto.resp.PagedApiResponse;
import com.mes.interfaces.api.dto.resp.manufacturerMeta.ManufacturerProcessCfgListResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configSide/processCfg")
public class ManufacturerProcessCfgController {

    @Autowired
    private AppManufacturerProcessCfgService processCfgService;

    /**
     * 分页查询工厂的工艺价格配置列表
     * @param request 分页请求参数
     * @param manufacturerId 制造商 ID（必填）
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public ApiResponse<PagedApiResponse<ManufacturerProcessCfgListResponse>> findProcessPriceCfgs(
            @Valid @RequestBody PagedApiRequest request,
            @RequestParam String manufacturerId) {

        PagedQuery query = request.toPagedQuery();
        List<ManufacturerProcessPriceCfg> result = processCfgService.getProcessPriceCfgByManufacturerId(
                manufacturerId, (int) query.getCurrent(), query.getSize());

        // 转换为响应 DTO
        List<ManufacturerProcessCfgListResponse> responses = result.stream()
                .map(ManufacturerProcessCfgListResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success(PagedApiResponse.success(
                responses,
                query.getCurrent(),
                query.getSize(),
                result.size()
        ));
    }

    /**
     * 更新工艺价格配置
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PutMapping("/price")
    public ApiResponse<String> updateProcessPrice(@Valid @RequestBody UpdateProcessPriceRequest request) {
        // 转换 MaterialProcessPriceRequest 到 MaterialProcessPrice
        List<MaterialProcessPrice> materialProcessPrices = null;
        if (request.getMaterialProcessPrices() != null && !request.getMaterialProcessPrices().isEmpty()) {
            materialProcessPrices = request.getMaterialProcessPrices().stream()
                    .map(dto -> {
                        MaterialProcessPrice mpp = new MaterialProcessPrice();
                        mpp.setMaterialId(dto.getMaterialId());
                        mpp.setMaterialName(dto.getMaterialName());
                        mpp.setProcessPrice(dto.getProcessPrice());
                        mpp.setBasePrice(dto.getBasePrice());
                        return mpp;
                    })
                    .collect(Collectors.toList());
        }

        processCfgService.updateProcessPriceConfig(
                request.getManufacturerId(),
                request.getProcessId(),
                request.getProcessPrice(),
                request.getBasePrice(),
                materialProcessPrices);

        return ApiResponse.success("success");
    }

    /**
     * 逻辑删除工艺价格配置
     * @param manufacturerId 制造商 ID
     * @param processId 工艺 ID
     * @return 操作结果
     */
    @DeleteMapping("/process")
    public ApiResponse<String> deleteProcessPriceConfig(
            @RequestParam String manufacturerId,
            @RequestParam String processId) {
        processCfgService.deleteProcessPriceConfig(manufacturerId, processId);
        return ApiResponse.success("success");
    }
}
