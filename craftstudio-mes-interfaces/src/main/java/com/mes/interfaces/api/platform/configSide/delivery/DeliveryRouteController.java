package com.mes.interfaces.api.platform.configSide.delivery;

import com.mes.application.command.delivery.AppDeliveryRouteService;
import com.mes.application.dto.req.delivery.DeliveryRouteListRequest;
import com.mes.application.dto.req.delivery.DeliveryRouteRequest;
import com.mes.application.dto.resp.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.delivery.DeliveryRouteListResponse;
import com.mes.application.dto.resp.delivery.DeliveryRouteNodeRequest;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configSide/delivery/deliveryRoute")
public class DeliveryRouteController {

    @Autowired
    private AppDeliveryRouteService appDeliveryRouteService;

    /**
     * 分页查询配送路线列表
     * @param request 分页请求参数
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<DeliveryRouteListResponse> listDeliveryRoutes(
            @Valid @RequestBody DeliveryRouteListRequest request) {
        
        PagedQuery query = request.toPagedQuery();
        String routeName = request.getRouteName();
        
        PagedResult<DeliveryRoute> result = appDeliveryRouteService.findDeliveryRoutes(routeName, query);
        
        List<DeliveryRouteListResponse> responses = result.items().stream()
                .map(DeliveryRouteListResponse::from)
                .collect(Collectors.toList());
        
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    /**
     * 根据 ID 获取配送路线详情
     * @param id 配送路线 ID
     * @return 配送路线详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DeliveryRouteListResponse> getDeliveryRouteById(@PathVariable String id) {
        DeliveryRoute deliveryRoute = appDeliveryRouteService.findById(id);
        DeliveryRouteListResponse response = DeliveryRouteListResponse.from(deliveryRoute);
        return ApiResponse.success(response);
    }

    /**
     * 新增配送路线
     * @param request 新增请求参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> addDeliveryRoute(@Valid @RequestBody DeliveryRouteRequest request) {
        DeliveryRoute deliveryRoute = request.toDomainEntity();
        appDeliveryRouteService.addDeliveryRoute(deliveryRoute);
        
        return ApiResponse.success("success");
    }

    /**
     * 编辑配送路线
     * @param request 编辑请求参数
     * @return 操作结果
     */
    @PutMapping("/edit")
    public ApiResponse<String> updateDeliveryRoute(@Valid @RequestBody DeliveryRouteRequest request) {
        DeliveryRoute existingDeliveryRoute = appDeliveryRouteService.findById(request.getId());
        if (existingDeliveryRoute == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "配送路线不存在");
        }

        DeliveryRoute updatedDeliveryRoute = request.toDomainEntity();
        updatedDeliveryRoute.setId(existingDeliveryRoute.getId());
        updatedDeliveryRoute.setCreateTime(existingDeliveryRoute.getCreateTime());

        appDeliveryRouteService.updateDeliveryRoute(updatedDeliveryRoute);

        return ApiResponse.success("success");
    }

    /**
     * 删除配送路线
     * @param id 配送路线 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteDeliveryRoute(@PathVariable String id) {
        appDeliveryRouteService.deleteDeliveryRoute(id);
        return ApiResponse.success("success");
    }

    /**
     * 激活配送路线
     * @param id 配送路线 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/activate")
    public ApiResponse<String> activateDeliveryRoute(@PathVariable String id) {
        appDeliveryRouteService.activateDeliveryRoute(id);
        return ApiResponse.success("success");
    }

    /**
     * 停用配送路线
     * @param id 配送路线 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/deactivate")
    public ApiResponse<String> deactivateDeliveryRoute(@PathVariable String id) {
        appDeliveryRouteService.deactivateDeliveryRoute(id);
        return ApiResponse.success("success");
    }

    /**
     * 添加路线节点
     * @param routeId 路线 ID
     * @param request 节点信息
     * @return 操作结果
     */
    @PostMapping("/{routeId}/nodes")
    public ApiResponse<String> addRouteNode(
            @PathVariable String routeId,
            @Valid @RequestBody DeliveryRouteNodeRequest request) {
        
        DeliveryRouteNode node = request.toDomainEntity();
        appDeliveryRouteService.addRouteNode(routeId, node);
        
        return ApiResponse.success("success");
    }

    /**
     * 移除路线节点
     * @param routeId 路线 ID
     * @param nodeId 节点 ID
     * @return 操作结果
     */
    @DeleteMapping("/{routeId}/nodes/{nodeId}")
    public ApiResponse<String> removeRouteNode(
            @PathVariable String routeId,
            @PathVariable String nodeId) {
        
        appDeliveryRouteService.removeRouteNode(routeId, nodeId);
        
        return ApiResponse.success("success");
    }
}
