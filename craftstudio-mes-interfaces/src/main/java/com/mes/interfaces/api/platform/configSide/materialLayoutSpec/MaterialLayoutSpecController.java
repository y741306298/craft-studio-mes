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
 * 可配置材料 Controller。
 * <p>
 * 该接口只维护“哪些材料可以由工厂角色选择后配置排版步进信息”。
 * 1m 到 10m 的阶梯内缩值不保存在这里，而是跟随具体工厂的
 * {@code ManufacturerMaterialLayoutSpecCfgController} 配置保存。
 */
@RestController
@RequestMapping("/api/configSide/materialLayoutSpec")
public class MaterialLayoutSpecController {

    @Autowired
    private AppMaterialLayoutSpecService appMaterialLayoutSpecService;

    /**
     * 分页查询可配置材料。
     *
     * @param request 分页与筛选条件，支持 materialId、materialName
     * @return 可配置材料分页结果
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
     * 查询可配置材料详情。
     *
     * @param id 可配置材料配置 ID
     * @return 可配置材料详情
     */
    @GetMapping("/{id}")
    public ApiResponse<MaterialLayoutSpecResponse> detail(@PathVariable String id) {
        return ApiResponse.success(MaterialLayoutSpecResponse.from(appMaterialLayoutSpecService.findById(id)));
    }

    /**
     * 新增可配置材料。
     * <p>
     * materialSnapshot 与 usageSize3D 参考 orderItem.material 中的同名配置，供工厂角色选择材料时带出默认材料信息。
     *
     * @param request 新增请求
     * @return 新增后的可配置材料
     */
    @PostMapping("/add")
    public ApiResponse<MaterialLayoutSpecResponse> add(@Valid @RequestBody MaterialLayoutSpecRequest request) {
        MaterialLayoutSpec spec = appMaterialLayoutSpecService.add(request.toDomainEntity());
        return ApiResponse.success(MaterialLayoutSpecResponse.from(spec));
    }

    /**
     * 编辑可配置材料。
     * <p>
     * 编辑时保留原始 ID 和创建时间，只更新材料、快照和尺寸信息。
     *
     * @param request 编辑请求，id 必填
     * @return 操作结果
     */
    @PostMapping("/edit")
    public ApiResponse<String> edit(@Valid @RequestBody MaterialLayoutSpecRequest request) {
        MaterialLayoutSpec existing = appMaterialLayoutSpecService.findById(request.getId());
        if (existing == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "可配置材料不存在");
        }
        MaterialLayoutSpec spec = request.toDomainEntity();
        spec.setId(existing.getId());
        spec.setCreateTime(existing.getCreateTime());
        appMaterialLayoutSpecService.update(spec);
        return ApiResponse.success("success");
    }

    /**
     * 删除可配置材料。
     * <p>
     * 兼容当前配置侧已有的 GET 删除调用，同时补充 DELETE 方法，方便后续前端按 REST 语义调用。
     *
     * @param id 可配置材料配置 ID
     * @return 操作结果
     */
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.GET, RequestMethod.DELETE})
    public ApiResponse<String> delete(@PathVariable String id) {
        appMaterialLayoutSpecService.delete(id);
        return ApiResponse.success("success");
    }
}
