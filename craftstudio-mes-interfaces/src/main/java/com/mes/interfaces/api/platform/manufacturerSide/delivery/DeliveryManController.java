package com.mes.interfaces.api.platform.manufacturerSide.delivery;

import com.mes.application.command.delivery.AppDeliveryManService;
import com.mes.application.command.delivery.req.DeliveryManRequest;
import com.mes.application.command.delivery.resp.DeliveryManResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manufacturerSide/delivery/deliveryMan")
public class DeliveryManController {

    @Autowired
    private AppDeliveryManService appDeliveryManService;

    /**
     * 新增发货人
     * @param request 发货人信息
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> addDeliveryMan(@Valid @RequestBody DeliveryManRequest request) {
        DeliveryMan deliveryMan = request.toDomainEntity();
        appDeliveryManService.addDeliveryMan(deliveryMan);
        return ApiResponse.success("success");
    }

    /**
     * 更新发货人
     * @param request 发货人信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public ApiResponse<String> updateDeliveryMan(@Valid @RequestBody DeliveryManRequest request) {
        DeliveryMan existingDeliveryMan = appDeliveryManService.findById(request.getId());
        if (existingDeliveryMan == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "发货人不存在");
        }

        DeliveryMan updatedDeliveryMan = request.toDomainEntity();
        updatedDeliveryMan.setId(existingDeliveryMan.getId());
        updatedDeliveryMan.setCreateTime(existingDeliveryMan.getCreateTime());

        appDeliveryManService.updateDeliveryMan(updatedDeliveryMan);
        return ApiResponse.success("success");
    }

    /**
     * 删除发货人
     * @param id 发货人ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteDeliveryMan(@PathVariable String id) {
        appDeliveryManService.deleteDeliveryMan(id);
        return ApiResponse.success("success");
    }

    /**
     * 根据ID查询发货人
     * @param id 发货人ID
     * @return 发货人信息
     */
    @GetMapping("/{id}")
    public ApiResponse<DeliveryManResponse> getDeliveryManById(@PathVariable String id) {
        DeliveryMan deliveryMan = appDeliveryManService.findById(id);
        DeliveryManResponse response = DeliveryManResponse.from(deliveryMan);
        return ApiResponse.success(response);
    }

    /**
     * 根据用户ID查询发货人列表
     * @param userId 用户ID
     * @return 发货人列表
     */
    @GetMapping("/listByUserId")
    public ApiResponse<List<DeliveryManResponse>> listByUserId(@RequestParam String userId) {
        List<DeliveryMan> deliveryMen = appDeliveryManService.findByUserId(userId);
        List<DeliveryManResponse> responses = deliveryMen.stream()
                .map(DeliveryManResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

}
