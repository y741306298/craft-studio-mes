package com.mes.domain.delivery.deliveryRoute.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeliveryRouteService {

    @Autowired
    private DeliveryRouteRepository deliveryRouteRepository;

    /**
     * 根据路线名称查询配送路线（支持分页）
     */
    public List<DeliveryRoute> findDeliveryRoutesByName(String routeName, String manufacturerId, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(routeName)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线名称不能为空");
        }
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "厂商 ID 不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("routeName", routeName);
        searchFilters.put("manufacturerId", manufacturerId);
        return deliveryRouteRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 获取配送路线总数
     */
    public long getTotalCount(String routeName, String manufacturerId) {
        if (StringUtils.isNotBlank(routeName)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("routeName", routeName);
            searchFilters.put("manufacturerId", manufacturerId);
            return deliveryRouteRepository.totalByFuzzySearch(searchFilters);
        } else {
            return deliveryRouteRepository.totalByManufacturerId(manufacturerId);
        }
    }

    /**
     * 添加配送路线
     */
    public DeliveryRoute addDeliveryRoute(DeliveryRoute deliveryRoute) {
        if (deliveryRoute == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线不能为空");
        }
        if (StringUtils.isBlank(deliveryRoute.getRouteName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线名称不能为空");
        }
        if (StringUtils.isBlank(deliveryRoute.getManufacturerMetaId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "厂商 ID 不能为空");
        }
        
        String routeId = IdGenerator.generateId("ROUTE");
        deliveryRoute.setRouteId(routeId);
        
        if (!validateDeliveryRoute(deliveryRoute)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线配置不完整");
        }
        
        return deliveryRouteRepository.add(deliveryRoute);
    }

    /**
     * 更新配送路线
     */
    public void updateDeliveryRoute(DeliveryRoute deliveryRoute) {
        if (deliveryRoute == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线不能为空");
        }
        if (StringUtils.isBlank(deliveryRoute.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线 ID 不能为空");
        }
        if (StringUtils.isBlank(deliveryRoute.getRouteName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线名称不能为空");
        }
        
        deliveryRouteRepository.update(deliveryRoute);
    }

    /**
     * 删除配送路线
     */
    public void deleteDeliveryRoute(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        
        DeliveryRoute deliveryRoute = deliveryRouteRepository.findById(id);
        if (deliveryRoute != null) {
            deliveryRouteRepository.delete(deliveryRoute);
        }
    }

    /**
     * 根据ID获取配送路线
     */
    public DeliveryRoute findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        return deliveryRouteRepository.findById(id);
    }

    /**
     * 激活配送路线
     */
    public void activateDeliveryRoute(String id) {
        DeliveryRoute deliveryRoute = findById(id);
        if (deliveryRoute == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线不存在");
        }
        
        deliveryRoute.setStatus("ACTIVE");
        deliveryRouteRepository.update(deliveryRoute);
    }

    /**
     * 停用配送路线
     */
    public void deactivateDeliveryRoute(String id) {
        DeliveryRoute deliveryRoute = findById(id);
        if (deliveryRoute == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线不存在");
        }
        
        deliveryRoute.setStatus("INACTIVE");
        deliveryRouteRepository.update(deliveryRoute);
    }

    /**
     * 添加路线节点
     */
    public void addRouteNode(String routeId, DeliveryRouteNode node) {
        if (StringUtils.isBlank(routeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线 ID 不能为空");
        }
        if (node == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线节点不能为空");
        }
        
        DeliveryRoute deliveryRoute = findById(routeId);
        if (deliveryRoute == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线不存在");
        }
        
        if (!node.validateNodeInfo()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线节点信息不完整");
        }
        
        node.buildRegionPath();
        
        if (deliveryRoute.getDeliveryRouteNodes() == null) {
            deliveryRoute.setDeliveryRouteNodes(new ArrayList<>());
        }
        
        if (node.getNodeOrder() == null) {
            node.setNodeOrder(deliveryRoute.getDeliveryRouteNodes().size());
        }
        
        deliveryRoute.getDeliveryRouteNodes().add(node);
        deliveryRouteRepository.update(deliveryRoute);
    }

    /**
     * 移除路线节点
     */
    public void removeRouteNode(String routeId, String nodeId) {
        if (StringUtils.isBlank(routeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线 ID 不能为空");
        }
        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "节点 ID 不能为空");
        }
        
        DeliveryRoute deliveryRoute = findById(routeId);
        if (deliveryRoute == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送路线不存在");
        }
        
        if (deliveryRoute.getDeliveryRouteNodes() != null) {
            deliveryRoute.getDeliveryRouteNodes().removeIf(node -> nodeId.equals(node.getId()));
            
            int order = 0;
            for (DeliveryRouteNode node : deliveryRoute.getDeliveryRouteNodes()) {
                node.setNodeOrder(order++);
            }
            
            deliveryRouteRepository.update(deliveryRoute);
        }
    }

    /**
     * 验证配送路线配置是否完整
     */
    private boolean validateDeliveryRoute(DeliveryRoute deliveryRoute) {
        if (deliveryRoute.getDeliveryRouteNodes() == null || deliveryRoute.getDeliveryRouteNodes().isEmpty()) {
            return false;
        }
        
        boolean hasStart = false;
        boolean hasEnd = false;
        
        for (DeliveryRouteNode node : deliveryRoute.getDeliveryRouteNodes()) {
            if (!node.validateNodeInfo()) {
                return false;
            }
            
            if (node.isStartNode()) {
                hasStart = true;
            }
            if (node.isEndNode()) {
                hasEnd = true;
            }
        }
        
        return hasStart && hasEnd;
    }
}
