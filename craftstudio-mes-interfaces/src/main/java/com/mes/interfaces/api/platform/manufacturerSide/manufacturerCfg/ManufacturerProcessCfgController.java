package com.mes.interfaces.api.platform.manufacturerSide.manufacturerCfg;

import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.command.api.resp.ProcessMetaResponse;
import com.mes.application.dto.req.manufacturerMeta.UpdateProcessPriceRequest;
import com.mes.application.dto.resp.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manufacturerSide/processCfg")
public class ManufacturerProcessCfgController {

    @Autowired
    private ProductCoreApiService productApiService;

    /**
     * 分页查找工艺定义
     * @param rmfId 工厂 ID，不能为空
     * @param current 当前页码，从 1 开始
     * @param size 每页大小
     * @return 分页查询结果
     */
    @GetMapping("/list")
    public PagedApiResponse<ProcessMetaResponse> listProcessMetas(
            @RequestParam String rmfId,
            @RequestParam int current,
            @RequestParam int size) {
        
        ProductCoreApiService.PagedResult<ProcessMetaResponse> result =
            productApiService.listProcessMetas(rmfId, current, size);
        
        return PagedApiResponse.success(result.getItems(), result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 按名字模糊搜索工艺定义
     * @param rmfId 工厂 ID，不能为空
     * @param name 名字字段搜索内容，不能为空
     * @param current 当前页码，从 1 开始
     * @param size 每页大小
     * @return 分页查询结果
     */
    @GetMapping("/search")
    public PagedApiResponse<ProcessMetaResponse> searchProcessMetas(
            @RequestParam String rmfId,
            @RequestParam String name,
            @RequestParam int current,
            @RequestParam int size) {
        
        ProductCoreApiService.PagedResult<ProcessMetaResponse> result =
            productApiService.searchProcessMetas(rmfId, name, current, size);
        
        return PagedApiResponse.success(result.getItems(), result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 配置工艺定义

     * @return 操作结果
     */
    @PostMapping("/config")
    public ApiResponse<String> configProcessMeta(
            @RequestBody UpdateProcessPriceRequest request) {
        productApiService.configProcessMeta(request.getRmfId(), request.getProcessMetaId(), request.getProcessPrice());
        return ApiResponse.success("success");
    }

}
