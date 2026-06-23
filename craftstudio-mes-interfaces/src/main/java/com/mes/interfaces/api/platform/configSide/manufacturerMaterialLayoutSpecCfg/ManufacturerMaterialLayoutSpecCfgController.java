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

/**
 * 工厂材料排版规格绑定 Controller。
 * <p>
 * 该接口只维护“工厂 -> 材料排版规格”的关联关系，不重复保存材料快照、使用尺寸和阶梯数据。
 * 这样可以避免工厂数量增长时重复配置同一材料规格。
 */
@RestController
@RequestMapping("/api/configSide/manufacturerMaterialLayoutSpecCfg")
public class ManufacturerMaterialLayoutSpecCfgController {

    @Autowired
    private AppManufacturerMaterialLayoutSpecCfgService appCfgService;

    @Autowired
    private AppMaterialLayoutSpecService appMaterialLayoutSpecService;

    /**
     * 分页查询工厂材料排版规格绑定关系。
     *
     * @param request 分页与筛选条件，支持 manufacturerMetaId、materialLayoutSpecId
     * @return 绑定关系分页结果，响应中会带出关联的材料排版规格详情
     */
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

    /**
     * 查询工厂材料排版规格绑定详情。
     *
     * @param id 绑定配置 ID
     * @return 绑定详情，包含关联的材料排版规格详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ManufacturerMaterialLayoutSpecCfgResponse> detail(@PathVariable String id) {
        return ApiResponse.success(toResponse(appCfgService.findById(id)));
    }

    /**
     * 新增工厂材料排版规格绑定。
     * <p>
     * 新增前会校验 manufacturerMetaId、materialLayoutSpecId 必填，并校验材料排版规格存在。
     *
     * @param request 新增请求
     * @return 新增后的绑定关系
     */
    @PostMapping("/add")
    public ApiResponse<ManufacturerMaterialLayoutSpecCfgResponse> add(
            @Valid @RequestBody ManufacturerMaterialLayoutSpecCfgRequest request) {
        ManufacturerMaterialLayoutSpecCfg cfg = appCfgService.add(request.toDomainEntity());
        return ApiResponse.success(toResponse(cfg));
    }

    /**
     * 编辑工厂材料排版规格绑定。
     * <p>
     * 编辑时保留原始 ID 和创建时间，只更新工厂与材料排版规格的关联关系。
     *
     * @param request 编辑请求，id 必填
     * @return 操作结果
     */
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

    /**
     * 删除工厂材料排版规格绑定。
     * <p>
     * 兼容当前配置侧已有的 GET 删除调用，同时补充 DELETE 方法，方便后续前端按 REST 语义调用。
     *
     * @param id 绑定配置 ID
     * @return 操作结果
     */
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.GET, RequestMethod.DELETE})
    public ApiResponse<String> delete(@PathVariable String id) {
        appCfgService.delete(id);
        return ApiResponse.success("success");
    }

    /**
     * 组装绑定响应。
     * <p>
     * 绑定表只保存 materialLayoutSpecId，这里按需查询材料排版规格，便于前端列表和详情页直接展示材料名称、尺寸和阶梯数据。
     */
    private ManufacturerMaterialLayoutSpecCfgResponse toResponse(ManufacturerMaterialLayoutSpecCfg cfg) {
        MaterialLayoutSpecResponse specResponse = null;
        if (cfg != null) {
            specResponse = MaterialLayoutSpecResponse.from(appMaterialLayoutSpecService.findById(cfg.getMaterialLayoutSpecId()));
        }
        return ManufacturerMaterialLayoutSpecCfgResponse.from(cfg, specResponse);
    }
}
