package com.mes.interfaces.api.platform.configSide.delivery;

import com.mes.application.command.delivery.AppDeliveryNetService;
import com.mes.domain.delivery.deliveryNet.entity.DeliveryNet;
import com.mes.interfaces.api.dto.req.delivery.DeliveryNetListRequest;
import com.mes.interfaces.api.dto.req.delivery.DeliveryNetRequest;
import com.mes.interfaces.api.dto.resp.ApiResponse;
import com.mes.interfaces.api.dto.resp.PagedApiResponse;
import com.mes.interfaces.api.dto.resp.delivery.DeliveryNetListResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configSide/delivery/deliveryNet")
public class DeliveryController {

    @Autowired
    private AppDeliveryNetService appDeliveryNetService;

    /**
     * 分页查询配送网络列表
     * @param request 分页请求参数
     * @param deliveryNetName 配送网络名称（可选）
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<DeliveryNetListResponse> listDeliveryNets(
            @Valid @RequestBody DeliveryNetListRequest request) {
        
        PagedQuery query = request.toPagedQuery();
        String deliveryNetName = request.getDeliveryNetName();
        
        PagedResult<DeliveryNet> result = appDeliveryNetService.findDeliveryNets(deliveryNetName, query);
        
        List<DeliveryNetListResponse> responses = result.items().stream()
                .map(DeliveryNetListResponse::from)
                .collect(Collectors.toList());
        
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    /**
     * 根据 ID 获取配送网络详情
     * @param id 配送网络 ID
     * @return 配送网络详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DeliveryNetListResponse> getDeliveryNetById(@PathVariable String id) {
        DeliveryNet deliveryNet = appDeliveryNetService.findById(id);
        DeliveryNetListResponse response = DeliveryNetListResponse.from(deliveryNet);
        return ApiResponse.success(response);
    }

    /**
     * 新增配送网络
     * @param request 新增请求参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> addDeliveryNet(@Valid @RequestBody DeliveryNetRequest request) {
        DeliveryNet deliveryNet = request.toDomainEntity();
        appDeliveryNetService.addDeliveryNet(deliveryNet);
        
        return ApiResponse.success("success");
    }

    /**
     * 编辑配送网络
     * @param request 编辑请求参数
     * @return 操作结果
     */
    @PutMapping("/edit")
    public ApiResponse<String> updateDeliveryNet(@Valid @RequestBody DeliveryNetRequest request) {
        DeliveryNet existingDeliveryNet = appDeliveryNetService.findById(request.getId());
        if (existingDeliveryNet == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "配送网络不存在");
        }

        DeliveryNet updatedDeliveryNet = request.toDomainEntity();
        updatedDeliveryNet.setId(existingDeliveryNet.getId());
        updatedDeliveryNet.setCreateTime(existingDeliveryNet.getCreateTime());

        appDeliveryNetService.updateDeliveryNet(updatedDeliveryNet);

        return ApiResponse.success("success");
    }

    /**
     * 删除配送网络
     * @param id 配送网络 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteDeliveryNet(@PathVariable String id) {
        appDeliveryNetService.deleteDeliveryNet(id);
        return ApiResponse.success("success");
    }

    /**
     * 激活配送网络
     * @param id 配送网络 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/activate")
    public ApiResponse<String> activateDeliveryNet(@PathVariable String id) {
        appDeliveryNetService.activateDeliveryNet(id);
        return ApiResponse.success("success");
    }

    /**
     * 停用配送网络
     * @param id 配送网络 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/deactivate")
    public ApiResponse<String> deactivateDeliveryNet(@PathVariable String id) {
        appDeliveryNetService.deactivateDeliveryNet(id);
        return ApiResponse.success("success");
    }

    /**
     * 暂停配送网络
     * @param id 配送网络 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/suspend")
    public ApiResponse<String> suspendDeliveryNet(@PathVariable String id) {
        appDeliveryNetService.suspendDeliveryNet(id);
        return ApiResponse.success("success");
    }
}
