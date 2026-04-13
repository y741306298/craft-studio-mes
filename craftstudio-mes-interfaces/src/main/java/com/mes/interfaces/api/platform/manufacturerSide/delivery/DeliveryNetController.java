package com.mes.interfaces.api.platform.manufacturerSide.delivery;

import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.command.api.req.ConfigLogisticsRequest;
import com.mes.application.command.api.resp.LogisticsConfigOptionsResponse;
import com.mes.application.command.api.resp.LogisticsConfigResponse;
import com.mes.application.command.delivery.AppDeliveryNetService;
import com.mes.application.dto.resp.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manufacturerSide/delivery/deliveryNet")
public class DeliveryNetController {

    @Autowired
    private AppDeliveryNetService appDeliveryNetService;

    @Autowired
    private ProductCoreApiService productApiService;

    /**
     * 查询物流配置列表（按父地区）
     * @param rmfId 工厂 ID，不能为空
     * @param parentRegionCode 父地区码，null 表示获取第一级地区（国家）配置
     * @return 物流配置映射，key 为地区编码，value 为该地区的物流商列表
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, List<LogisticsConfigResponse.LogisticsItem>>> listDeliveryNets(
            @RequestParam String rmfId,
            @RequestParam(required = false) String parentRegionCode) {

        Map<String, List<LogisticsConfigResponse.LogisticsItem>> logisticsConfigs =
                productApiService.findLogisticsConfigsByParentRegionCode(rmfId, parentRegionCode);

        return ApiResponse.success(logisticsConfigs);
    }

    /**
     * 获取物流配置选项（物流供应商和承运商列表）
     * @param rmfId 工厂 ID，不能为空
     * @return 物流配置选项，包含 providers（物流供应商）和 carriers（承运商）
     */
    @GetMapping("/options")
    public ApiResponse<LogisticsConfigOptionsResponse> getLogisticsConfigOptions(
            @RequestParam String rmfId) {

        LogisticsConfigOptionsResponse options = productApiService.getLogisticsConfigOptions(rmfId);
        return ApiResponse.success(options);
    }

    /**
     * 配置物流
     * @param request 配置请求参数
     * @return 操作结果
     */
    @PostMapping("/config")
    public ApiResponse<String> configLogistics(@Valid @RequestBody ConfigLogisticsRequest request) {
        productApiService.configLogistics(
                request.getRmfId(),
                request.getCarrierId(),
                request.getProviderId(),
                request.getRegionCode(),
                request.getPrice().getFirstWeightPrice(),
                request.getPrice().getExtraWeightPrice()
        );
        return ApiResponse.success("success");
    }

}
