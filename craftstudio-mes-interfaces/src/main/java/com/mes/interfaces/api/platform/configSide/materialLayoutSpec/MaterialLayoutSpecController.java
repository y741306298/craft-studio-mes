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

/**
 * 材料排版规格配置 Controller。
 * <p>
 * 该接口只维护“材料维度”的公共排版规格，不直接保存 manufacturerMetaId。
 * 工厂很多、材料较少时，材料规格只需要配置一次；工厂侧通过
 * {@code ManufacturerMaterialLayoutSpecCfgController} 绑定到这里的规格即可复用。
 */
@RestController
@RequestMapping("/api/configSide/materialLayoutSpec")
public class MaterialLayoutSpecController {

    @Autowired
    private AppMaterialLayoutSpecService appMaterialLayoutSpecService;

    /**
     * 分页查询材料排版规格。
     *
     * @param request 分页与筛选条件，支持 materialId、materialName
     * @return 材料排版规格分页结果
     */
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

    /**
     * 查询材料排版规格详情。
     *
     * @param id 材料排版规格 ID
     * @return 材料排版规格详情
     */
    @GetMapping("/{id}")
    public ApiResponse<MaterialLayoutSpecResponse> detail(@PathVariable String id) {
        return ApiResponse.success(MaterialLayoutSpecResponse.from(appMaterialLayoutSpecService.findById(id)));
    }

    /**
     * 新增材料排版规格。
     * <p>
     * insetSteps 必须包含 1m 到 10m 的完整阶梯内缩配置，单位为厘米。
     * materialSnapshot 与 usageSize3D 参考 orderItem.material 中的同名配置，便于后续排版计算复用。
     *
     * @param request 新增请求
     * @return 新增后的材料排版规格
     */
    @PostMapping("/add")
    public ApiResponse<MaterialLayoutSpecResponse> add(@Valid @RequestBody MaterialLayoutSpecRequest request) {
        MaterialLayoutSpec spec = appMaterialLayoutSpecService.add(request.toDomainEntity());
        return ApiResponse.success(MaterialLayoutSpecResponse.from(spec));
    }

    /**
     * 编辑材料排版规格。
     * <p>
     * 编辑时保留原始 ID 和创建时间，只更新材料、尺寸与阶梯内缩配置。
     *
     * @param request 编辑请求，id 必填
     * @return 操作结果
     */
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

    /**
     * 删除材料排版规格。
     * <p>
     * 兼容当前配置侧已有的 GET 删除调用，同时补充 DELETE 方法，方便后续前端按 REST 语义调用。
     *
     * @param id 材料排版规格 ID
     * @return 操作结果
     */
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.GET, RequestMethod.DELETE})
    public ApiResponse<String> delete(@PathVariable String id) {
        appMaterialLayoutSpecService.delete(id);
        return ApiResponse.success("success");
    }
}
