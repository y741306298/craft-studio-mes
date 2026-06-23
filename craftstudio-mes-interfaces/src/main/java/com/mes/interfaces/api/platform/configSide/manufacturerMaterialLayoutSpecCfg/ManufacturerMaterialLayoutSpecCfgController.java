package com.mes.interfaces.api.platform.configSide.manufacturerMaterialLayoutSpecCfg;

import com.mes.application.command.manufacturerMaterialLayoutSpecCfg.AppManufacturerMaterialLayoutSpecCfgService;
import com.mes.application.command.materialLayoutSpec.AppMaterialLayoutSpecService;
import com.mes.application.dto.req.manufacturerMaterialLayoutSpecCfg.ManufacturerMaterialLayoutSpecCfgListRequest;
import com.mes.application.dto.req.manufacturerMaterialLayoutSpecCfg.ManufacturerMaterialLayoutSpecCfgRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.manufacturerMaterialLayoutSpecCfg.ManufacturerMaterialLayoutSpecCfgResponse;
import com.mes.application.dto.resp.materialLayoutSpec.MaterialLayoutSpecResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configSide/manufacturerMaterialLayoutSpecCfg")
public class ManufacturerMaterialLayoutSpecCfgController {

    @Autowired
    private AppManufacturerMaterialLayoutSpecCfgService appCfgService;

    @Autowired
    private AppMaterialLayoutSpecService appMaterialLayoutSpecService;

    @PostMapping("/list")
    public PagedApiResponse<ManufacturerMaterialLayoutSpecCfgResponse> list(
            @Valid @RequestBody ManufacturerMaterialLayoutSpecCfgListRequest request) {
        PagedQuery query = request.toPagedQuery();
        PagedResult<ManufacturerMaterialLayoutSpecCfg> result = appCfgService.list(
                request.getManufacturerMetaId(), request.getMaterialLayoutSpecId(), query);
        List<ManufacturerMaterialLayoutSpecCfgResponse> responses = result.items().stream()
                .map(this::toResponse)
                .toList();
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    @GetMapping("/{id}")
    public ApiResponse<ManufacturerMaterialLayoutSpecCfgResponse> detail(@PathVariable String id) {
        return ApiResponse.success(toResponse(appCfgService.findById(id)));
    }

    @PostMapping("/add")
    public ApiResponse<ManufacturerMaterialLayoutSpecCfgResponse> add(
            @Valid @RequestBody ManufacturerMaterialLayoutSpecCfgRequest request) {
        ManufacturerMaterialLayoutSpecCfg cfg = appCfgService.add(request.toDomainEntity());
        return ApiResponse.success(toResponse(cfg));
    }

    @PostMapping("/edit")
    public ApiResponse<String> edit(@Valid @RequestBody ManufacturerMaterialLayoutSpecCfgRequest request) {
        ManufacturerMaterialLayoutSpecCfg existing = appCfgService.findById(request.getId());
        if (existing == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "工厂材料排版规格配置不存在");
        }
        ManufacturerMaterialLayoutSpecCfg cfg = request.toDomainEntity();
        cfg.setId(existing.getId());
        cfg.setCreateTime(existing.getCreateTime());
        appCfgService.update(cfg);
        return ApiResponse.success("success");
    }

    @GetMapping("/delete/{id}")
    public ApiResponse<String> delete(@PathVariable String id) {
        appCfgService.delete(id);
        return ApiResponse.success("success");
    }

    private ManufacturerMaterialLayoutSpecCfgResponse toResponse(ManufacturerMaterialLayoutSpecCfg cfg) {
        MaterialLayoutSpecResponse specResponse = null;
        if (cfg != null) {
            specResponse = MaterialLayoutSpecResponse.from(appMaterialLayoutSpecService.findById(cfg.getMaterialLayoutSpecId()));
        }
        return ManufacturerMaterialLayoutSpecCfgResponse.from(cfg, specResponse);
    }
}
