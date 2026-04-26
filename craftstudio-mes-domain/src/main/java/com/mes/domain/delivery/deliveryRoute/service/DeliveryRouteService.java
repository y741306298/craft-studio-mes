package com.mes.domain.delivery.deliveryRoute.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNodeBinding;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeBindingRepository;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeRepository;
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
    @Autowired
    private DeliveryRouteNodeRepository deliveryRouteNodeRepository;
    @Autowired
    private DeliveryRouteNodeBindingRepository deliveryRouteNodeBindingRepository;

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
        List<DeliveryRoute> routes = deliveryRouteRepository.fuzzySearch(searchFilters, current, size);
        fillRouteNodes(routes);
        return routes;
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

    public void hydrateRouteNodes(List<DeliveryRoute> routes) {
        fillRouteNodes(routes);
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
        
        List<DeliveryRouteNode> routeNodes = deliveryRoute.getDeliveryRouteNodes();
        deliveryRoute.setDeliveryRouteNodes(null);
        DeliveryRoute savedRoute = deliveryRouteRepository.add(deliveryRoute);
        saveRouteNodes(savedRoute.getId(), routeNodes);
        savedRoute.setDeliveryRouteNodes(routeNodes);
        return savedRoute;
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
        
        List<DeliveryRouteNode> routeNodes = deliveryRoute.getDeliveryRouteNodes();
        deliveryRoute.setDeliveryRouteNodes(null);
        deliveryRouteRepository.update(deliveryRoute);
        if (routeNodes != null) {
            deliveryRouteNodeRepository.removeByRouteId(deliveryRoute.getId());
            saveRouteNodes(deliveryRoute.getId(), routeNodes);
        }
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
            deliveryRouteNodeRepository.removeByRouteId(deliveryRoute.getId());
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
        DeliveryRoute route = deliveryRouteRepository.findById(id);
        fillRouteNodes(route);
        return route;
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
        node.setRouteId(routeId);

        if (node.getNodeOrder() == null) {
            node.setNodeOrder(deliveryRouteNodeRepository.listByRouteId(routeId).size());
        }

        node.setId(null);
        deliveryRouteNodeRepository.add(node);
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
        
        List<DeliveryRouteNode> nodes = deliveryRouteNodeRepository.listByRouteId(routeId);
        if (!nodes.isEmpty()) {
            DeliveryRouteNode removedNode = null;
            for (DeliveryRouteNode node : nodes) {
                if (nodeId.equals(node.getId())) {
                    removedNode = node;
                    break;
                }
            }
            if (removedNode == null) {
                return;
            }
            nodes.removeIf(node -> nodeId.equals(node.getId()));
            deliveryRouteNodeRepository.delete(removedNode);

            int order = 0;
            for (DeliveryRouteNode node : nodes) {
                node.setNodeOrder(order++);
            }

            if (!nodes.isEmpty()) {
                deliveryRouteNodeRepository.batchUpdate(nodes);
            }
        }
    }

    public void bindTerminalAddressToRouteNode(String terminalRegionCode, String detailAddress, String routeNodeId) {
        if (StringUtils.isBlank(terminalRegionCode) || StringUtils.isBlank(detailAddress) || StringUtils.isBlank(routeNodeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "绑定参数不能为空");
        }

        DeliveryRouteNode routeNode = deliveryRouteNodeRepository.findById(routeNodeId);
        if (routeNode == null) {
            routeNode = deliveryRouteNodeRepository.findByRouteNodeId(routeNodeId);
        }
        if (routeNode == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线节点不存在");
        }

        if (StringUtils.isBlank(routeNode.getRouteId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线节点未关联路线");
        }
        DeliveryRoute route = deliveryRouteRepository.findById(routeNode.getRouteId());
        if (route == null || StringUtils.isBlank(route.getManufacturerMetaId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线或厂商信息不存在");
        }

        DeliveryRouteNodeBinding binding = deliveryRouteNodeBindingRepository.findByManufacturerAndAddress(
                route.getManufacturerMetaId(), terminalRegionCode, detailAddress
        );
        if (binding == null) {
            binding = new DeliveryRouteNodeBinding();
            binding.setManufacturerMetaId(route.getManufacturerMetaId());
            binding.setTerminalRegionCode(terminalRegionCode);
            binding.setDetailAddress(detailAddress);
            binding.setRouteNodeId(routeNodeId);
            deliveryRouteNodeBindingRepository.add(binding);
            return;
        }

        binding.setRouteNodeId(routeNodeId);
        deliveryRouteNodeBindingRepository.update(binding);
    }

    public RouteNodeMatchResult matchRouteNodeByAddress(String manufacturerMetaId, String terminalRegionCode, String detailAddress) {
        if (StringUtils.isBlank(manufacturerMetaId) || StringUtils.isBlank(terminalRegionCode) || StringUtils.isBlank(detailAddress)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "查询参数不能为空");
        }

        List<DeliveryRouteNodeBinding> candidates = deliveryRouteNodeBindingRepository
                .listByManufacturerAndTerminalRegion(manufacturerMetaId, terminalRegionCode);
        if (candidates == null || candidates.isEmpty()) {
            return RouteNodeMatchResult.unmatched();
        }

        DeliveryRouteNodeBinding matchedBinding = null;
        for (DeliveryRouteNodeBinding candidate : candidates) {
            if (detailAddress.equals(candidate.getDetailAddress())) {
                matchedBinding = candidate;
                break;
            }
        }
        if (matchedBinding == null) {
            return RouteNodeMatchResult.unmatched();
        }

        DeliveryRouteNode routeNode = deliveryRouteNodeRepository.findById(matchedBinding.getRouteNodeId());
        if (routeNode == null || StringUtils.isBlank(routeNode.getRouteId())) {
            return RouteNodeMatchResult.unmatched();
        }

        DeliveryRoute route = deliveryRouteRepository.findById(routeNode.getRouteId());
        if (route == null) {
            return RouteNodeMatchResult.unmatched();
        }
        fillRouteNodes(route);
        return RouteNodeMatchResult.matched(route, routeNode);
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

    private void fillRouteNodes(List<DeliveryRoute> routes) {
        if (routes == null || routes.isEmpty()) {
            return;
        }
        for (DeliveryRoute route : routes) {
            fillRouteNodes(route);
        }
    }

    private void fillRouteNodes(DeliveryRoute route) {
        if (route == null || StringUtils.isBlank(route.getId())) {
            return;
        }
        route.setDeliveryRouteNodes(deliveryRouteNodeRepository.listByRouteId(route.getId()));
    }

    private void saveRouteNodes(String routeId, List<DeliveryRouteNode> routeNodes) {
        if (routeNodes == null || routeNodes.isEmpty()) {
            return;
        }
        List<DeliveryRouteNode> nodesToSave = new ArrayList<>();
        for (int i = 0; i < routeNodes.size(); i++) {
            DeliveryRouteNode node = routeNodes.get(i);
            if (node == null) {
                continue;
            }
            node.setRouteId(routeId);
            node.setId(null);
            if (node.getNodeOrder() == null) {
                node.setNodeOrder(i);
            }
            node.buildRegionPath();
            nodesToSave.add(node);
        }
        if (!nodesToSave.isEmpty()) {
            deliveryRouteNodeRepository.batchAdd(nodesToSave);
        }
    }

    @lombok.Data
    public static class RouteNodeMatchResult {
        private boolean matched;
        private DeliveryRoute deliveryRoute;
        private DeliveryRouteNode deliveryRouteNode;

        public static RouteNodeMatchResult unmatched() {
            RouteNodeMatchResult result = new RouteNodeMatchResult();
            result.setMatched(false);
            return result;
        }

        public static RouteNodeMatchResult matched(DeliveryRoute route, DeliveryRouteNode node) {
            RouteNodeMatchResult result = new RouteNodeMatchResult();
            result.setMatched(true);
            result.setDeliveryRoute(route);
            result.setDeliveryRouteNode(node);
            return result;
        }
    }
}
