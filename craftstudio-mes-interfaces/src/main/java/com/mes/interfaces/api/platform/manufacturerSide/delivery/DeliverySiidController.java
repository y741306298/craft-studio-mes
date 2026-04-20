package com.mes.interfaces.api.platform.manufacturerSide.delivery;

import com.mes.application.command.delivery.AppDeliverySiidService;
import com.mes.application.command.delivery.req.DeliverySiidRequest;
import com.mes.application.command.delivery.resp.DeliverySiidResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manufacturerSide/delivery/deliverySiid")
public class DeliverySiidController {

    @Autowired
    private AppDeliverySiidService appDeliverySiidService;

    /**
     * 新增SIID
     * @param request SIID信息
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> addDeliverySiid(@Valid @RequestBody DeliverySiidRequest request) {
        DeliverySiid deliverySiid = request.toDomainEntity();
        appDeliverySiidService.addDeliverySiid(deliverySiid);
        return ApiResponse.success("success");
    }

    /**
     * 更新SIID
     * @param request SIID信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public ApiResponse<String> updateDeliverySiid(@Valid @RequestBody DeliverySiidRequest request) {
        DeliverySiid existingDeliverySiid = appDeliverySiidService.findById(request.getId());
        if (existingDeliverySiid == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "SIID记录不存在");
        }

        DeliverySiid updatedDeliverySiid = request.toDomainEntity();
        updatedDeliverySiid.setId(existingDeliverySiid.getId());
        updatedDeliverySiid.setCreateTime(existingDeliverySiid.getCreateTime());

        appDeliverySiidService.updateDeliverySiid(updatedDeliverySiid);
        return ApiResponse.success("success");
    }

    /**
     * 删除SIID
     * @param id SIID记录ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteDeliverySiid(@PathVariable String id) {
        appDeliverySiidService.deleteDeliverySiid(id);
        return ApiResponse.success("success");
    }

    /**
     * 根据ID查询SIID
     * @param id SIID记录ID
     * @return SIID信息
     */
    @GetMapping("/{id}")
    public ApiResponse<DeliverySiidResponse> getDeliverySiidById(@PathVariable String id) {
        DeliverySiid deliverySiid = appDeliverySiidService.findById(id);
        DeliverySiidResponse response = DeliverySiidResponse.from(deliverySiid);
        return ApiResponse.success(response);
    }

    /**
     * 根据SIID查询
     * @param siid SIID值
     * @return SIID信息
     */
    @GetMapping("/getBySiid")
    public ApiResponse<DeliverySiidResponse> getBySiid(@RequestParam String siid) {
        DeliverySiid deliverySiid = appDeliverySiidService.findBySiid(siid);
        DeliverySiidResponse response = DeliverySiidResponse.from(deliverySiid);
        return ApiResponse.success(response);
    }

    /**
     * 根据用户ID查询SIID列表
     * @param userId 用户ID
     * @return SIID列表
     */
    @GetMapping("/listByUserId")
    public ApiResponse<List<DeliverySiidResponse>> listByUserId(@RequestParam String userId) {
        List<DeliverySiid> deliverySiids = appDeliverySiidService.findByUserId(userId);
        List<DeliverySiidResponse> responses = deliverySiids.stream()
                .map(DeliverySiidResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    /**
     * 查询所有SIID列表
     * @return SIID列表
     */
    @GetMapping("/list")
    public ApiResponse<List<DeliverySiidResponse>> listAll() {
        List<DeliverySiid> deliverySiids = appDeliverySiidService.listAll();
        List<DeliverySiidResponse> responses = deliverySiids.stream()
                .map(DeliverySiidResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }
}
