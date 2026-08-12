package com.mes.interfaces.api.platform.configSide.wdt;

import com.mes.application.command.wdt.req.WdtConfigListRequest;
import com.mes.application.command.wdt.req.WdtConfigRequest;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.gatherplatform.wdt.entity.WdtConfig;
import com.mes.domain.gatherplatform.wdt.service.WdtService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 旺店通快递配置接口。
 */
@Validated
@RestController
@RequestMapping("/api/configSide/wdt")
public class WdtController {
    @Autowired
    private WdtService wdtService;

    /**
     * 新增旺店通快递配置。
     */
    @PostMapping("/add")
    public ApiResponse<WdtConfig> add(@Valid @RequestBody WdtConfigRequest request) {
        return ApiResponse.success(wdtService.add(request.toDomainEntity()));
    }

    /**
     * 更新旺店通快递配置。
     */
    @PostMapping("/update")
    public ApiResponse<String> update(@Valid @RequestBody WdtConfigRequest request) {
        wdtService.update(request.toDomainEntity());
        return ApiResponse.success();
    }

    /**
     * 删除指定配置。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable String id) {
        wdtService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 查询指定配置。
     */
    @GetMapping("/{id}")
    public ApiResponse<WdtConfig> findById(@PathVariable String id) {
        return ApiResponse.success(wdtService.findById(id));
    }

    /**
     * 分页查询配置。
     */
    @PostMapping("/list")
    public ApiResponse<PagedResult<WdtConfig>> list(@Valid @RequestBody WdtConfigListRequest request) {
        return ApiResponse.success(new PagedResult<>(
                wdtService.list(request.getCurrent(), request.getSize(),
                        request.getManufacturerMetaId(), request.getPresetType()),
                wdtService.total(request.getManufacturerMetaId(), request.getPresetType()),
                request.getSize(), request.getCurrent()));
    }
}
