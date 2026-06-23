package com.mes.interfaces.api.platform.configSide.materialLayoutSpec;

import com.mes.application.command.materialLayoutSpec.AppMaterialLayoutSpecService;
import com.mes.application.dto.req.materialLayoutSpec.MaterialLayoutSpecListRequest;
import com.mes.application.dto.req.materialLayoutSpec.MaterialLayoutSpecRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.materialLayoutSpec.MaterialLayoutSpecResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpec;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configSide/materialLayoutSpec")
public class MaterialLayoutSpecController {

    @Autowired
    private AppMaterialLayoutSpecService appMaterialLayoutSpecService;

    @PostMapping("/list")
    public PagedApiResponse<MaterialLayoutSpecResponse> list(@Valid @RequestBody MaterialLayoutSpecListRequest request) {
        PagedQuery query = request.toPagedQuery();
        PagedResult<MaterialLayoutSpec> result = appMaterialLayoutSpecService.list(
                request.getMaterialId(), request.getMaterialName(), query);
        List<MaterialLayoutSpecResponse> responses = result.items().stream()
                .map(MaterialLayoutSpecResponse::from)
                .toList();
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    @GetMapping("/{id}")
    public ApiResponse<MaterialLayoutSpecResponse> detail(@PathVariable String id) {
        return ApiResponse.success(MaterialLayoutSpecResponse.from(appMaterialLayoutSpecService.findById(id)));
    }

    @PostMapping("/add")
    public ApiResponse<MaterialLayoutSpecResponse> add(@Valid @RequestBody MaterialLayoutSpecRequest request) {
        MaterialLayoutSpec spec = appMaterialLayoutSpecService.add(request.toDomainEntity());
        return ApiResponse.success(MaterialLayoutSpecResponse.from(spec));
    }

    @PostMapping("/edit")
    public ApiResponse<String> edit(@Valid @RequestBody MaterialLayoutSpecRequest request) {
        MaterialLayoutSpec existing = appMaterialLayoutSpecService.findById(request.getId());
        if (existing == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "材料排版规格配置不存在");
        }
        MaterialLayoutSpec spec = request.toDomainEntity();
        spec.setId(existing.getId());
        spec.setCreateTime(existing.getCreateTime());
        appMaterialLayoutSpecService.update(spec);
        return ApiResponse.success("success");
    }

    @GetMapping("/delete/{id}")
    public ApiResponse<String> delete(@PathVariable String id) {
        appMaterialLayoutSpecService.delete(id);
        return ApiResponse.success("success");
    }
}
