package com.mes.interfaces.api.platform.manufacturerSide.manufacturerMaterialLayoutSpecCfg;

import com.mes.application.command.manufacturerMaterialLayoutSpecCfg.AppManufacturerMaterialLayoutSpecCfgService;
import com.mes.application.dto.req.manufacturerMaterialLayoutSpecCfg.ManufacturerMaterialLayoutSpecCfgDetailRequest;
import com.mes.application.dto.req.manufacturerMaterialLayoutSpecCfg.ManufacturerMaterialLayoutSpecCfgListRequest;
import com.mes.application.dto.req.manufacturerMaterialLayoutSpecCfg.ManufacturerMaterialLayoutSpecCfgRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.manufacturerMaterialLayoutSpecCfg.ManufacturerMaterialLayoutSpecCfgResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工厂材料步进配置 Controller。
 * <p>
 * 工厂角色直接选择一个可配置材料后，在这里维护该工厂、该材料自己的 1m 到 10m 阶梯内缩值。
 * 因此步进配置跟随 manufacturerMetaId + materialId 保存，而不是先配到材料再绑定工厂。
 */
@RestController
@RequestMapping("/api/manufacturerSide/manufacturerMaterialLayoutSpecCfg")
public class ManufacturerSideManufacturerMaterialLayoutSpecCfgController {

    @Autowired
    private AppManufacturerMaterialLayoutSpecCfgService appCfgService;

    /**
     * 分页查询工厂材料步进配置。
     *
     * @param request 分页与筛选条件，支持 manufacturerMetaId、materialId
     * @return 工厂材料步进配置分页结果
     */
    @PostMapping("/list")
    public PagedApiResponse<ManufacturerMaterialLayoutSpecCfgResponse> list(
            @Valid @RequestBody ManufacturerMaterialLayoutSpecCfgListRequest request) {
        PagedQuery query = request.toPagedQuery();
        PagedResult<ManufacturerMaterialLayoutSpecCfg> result = appCfgService.list(
                request.getManufacturerMetaId(), request.getMaterialId(), query);
        List<ManufacturerMaterialLayoutSpecCfgResponse> responses = result.items().stream()
                .map(ManufacturerMaterialLayoutSpecCfgResponse::from)
                .toList();
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    /**
     * 按材料 ID 和工厂 ID 查询工厂材料步进配置详情。
     *
     * @param request 查询条件，包含 material_layout_spec 中的材料 ID 和工厂元数据 ID
     * @return 工厂材料步进配置详情
     */
    @PostMapping("/detail")
    public ApiResponse<ManufacturerMaterialLayoutSpecCfgResponse> detail(
            @Valid @RequestBody ManufacturerMaterialLayoutSpecCfgDetailRequest request) {
        return ApiResponse.success(ManufacturerMaterialLayoutSpecCfgResponse.from(
                appCfgService.findByManufacturerMetaIdAndMaterialId(
                        request.getManufacturerMetaId(), request.getMaterialId())));
    }

    /**
     * 新增工厂材料步进配置。
     * <p>
     * 新增前会校验 manufacturerMetaId、materialId 必填，材料必须在可配置材料清单中存在，
     * 且 insetSteps 必须包含 1m 到 10m 的完整阶梯内缩配置。
     *
     * @param request 新增请求
     * @return 新增后的工厂材料步进配置
     */
    @PostMapping("/add")
    public ApiResponse<ManufacturerMaterialLayoutSpecCfgResponse> add(
            @Valid @RequestBody ManufacturerMaterialLayoutSpecCfgRequest request) {
        ManufacturerMaterialLayoutSpecCfg cfg = appCfgService.add(request.toDomainEntity());
        return ApiResponse.success(ManufacturerMaterialLayoutSpecCfgResponse.from(cfg));
    }

    /**
     * 编辑工厂材料步进配置。
     * <p>
     * 编辑时保留原始 ID 和创建时间，只更新工厂、材料和阶梯内缩配置。
     *
     * @param request 编辑请求，id 必填
     * @return 操作结果
     */
    @PostMapping("/edit")
    public ApiResponse<String> edit(@Valid @RequestBody ManufacturerMaterialLayoutSpecCfgRequest request) {
        ManufacturerMaterialLayoutSpecCfg existing = appCfgService.findById(request.getId());
        if (existing == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "工厂材料步进配置不存在");
        }
        ManufacturerMaterialLayoutSpecCfg cfg = request.toDomainEntity();
        cfg.setId(existing.getId());
        cfg.setCreateTime(existing.getCreateTime());
        appCfgService.update(cfg);
        return ApiResponse.success("success");
    }

    /**
     * 删除工厂材料步进配置。
     * <p>
     * 兼容当前配置侧已有的 GET 删除调用，同时补充 DELETE 方法，方便后续前端按 REST 语义调用。
     *
     * @param id 工厂材料步进配置 ID
     * @return 操作结果
     */
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.GET, RequestMethod.DELETE})
    public ApiResponse<String> delete(@PathVariable String id) {
        appCfgService.delete(id);
        return ApiResponse.success("success");
    }
}
