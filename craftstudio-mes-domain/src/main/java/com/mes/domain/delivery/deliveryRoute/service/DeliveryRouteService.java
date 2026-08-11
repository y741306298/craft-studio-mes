package com.mes.domain.delivery.deliveryRoute.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecordStatus;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNodeBinding;
import com.mes.domain.delivery.deliveryRoute.entity.RouteNode;
import com.mes.domain.delivery.deliveryRoute.repository.AddressRecognitionRecordRepository;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeBindingRepository;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeRepository;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.repository.ProductionPieceRepository;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.repository.OrderInfoRepository;
import com.mes.domain.order.orderInfo.repository.OrderItemRepository;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeliveryRouteService {

    @Autowired
    private DeliveryRouteRepository deliveryRouteRepository;
    @Autowired
    private DeliveryRouteNodeRepository deliveryRouteNodeRepository;
    @Autowired
    private DeliveryRouteNodeBindingRepository deliveryRouteNodeBindingRepository;
    @Autowired
    private AddressRecognitionRecordRepository addressRecognitionRecordRepository;
    @Autowired
    private OrderInfoRepository orderInfoRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductionPieceRepository productionPieceRepository;

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
        searchFilters.put("manufacturerMetaId", manufacturerId);
        List<DeliveryRoute> routes = deliveryRouteRepository.fuzzySearch(searchFilters, current, size);
        return routes;
    }

    /**
     * 获取配送路线总数
     */
    public long getTotalCount(String routeName, String manufacturerId) {
        if (StringUtils.isNotBlank(routeName)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("routeName", routeName);
            searchFilters.put("manufacturerMetaId", manufacturerId);
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
        
        List<RouteNode> routeNodes = deliveryRoute.getRouteNodes();
        prepareRouteNodes(routeNodes);
        deliveryRoute.setDeliveryRouteNodes(null);
        return deliveryRouteRepository.add(deliveryRoute);
    }


    private void prepareRouteNodes(List<RouteNode> routeNodes) {
        if (routeNodes == null || routeNodes.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线节点不能为空");
        }
        for (int i = 0; i < routeNodes.size(); i++) {
            RouteNode routeNode = routeNodes.get(i);
            if (routeNode == null || StringUtils.isBlank(routeNode.getName())) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线节点名称不能为空");
            }
            routeNode.setId(String.valueOf(i + 1));
        }
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
        
        List<String> removedSimpleNodeIds = List.of();
        if (deliveryRoute.getRouteNodes() != null) {
            removedSimpleNodeIds = resolveRemovedSimpleRouteNodeIds(deliveryRoute.getId(), deliveryRoute.getRouteNodes());
            prepareRouteNodes(deliveryRoute.getRouteNodes());
        }
        List<DeliveryRouteNode> routeNodes = deliveryRoute.getDeliveryRouteNodes();
        List<String> removedNodeIds = new ArrayList<>(removedSimpleNodeIds);
        for (String removedRouteNodeId : resolveRemovedRouteNodeIds(deliveryRoute.getId(), routeNodes)) {
            addIfNotBlank(removedNodeIds, removedRouteNodeId);
        }
        assertNoInProductionOrderBinding(deliveryRoute.getId(), removedNodeIds, "存在生产中的订单绑定了被减少的路线节点，不能删除节点");

        deliveryRoute.setDeliveryRouteNodes(null);
        deliveryRouteRepository.update(deliveryRoute);
        if (routeNodes != null) {
            deliveryRouteNodeRepository.removeByRouteId(deliveryRoute.getId());
            saveRouteNodes(deliveryRoute.getId(), routeNodes);
        }
        unbindAddressesByRouteNodes(deliveryRoute.getId(), removedNodeIds);
        clearRouteBindings(deliveryRoute.getId(), removedNodeIds);
        deleteRouteNodeBindings(removedNodeIds);
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
            List<DeliveryRouteNode> routeNodes = deliveryRouteNodeRepository.listByRouteId(deliveryRoute.getId());
            List<String> routeNodeIds = collectRouteNodeIdentifiers(routeNodes);
            clearRouteBindings(deliveryRoute.getId(), null);
            deliveryRouteNodeRepository.removeByRouteId(deliveryRoute.getId());
            deliveryRouteRepository.delete(deliveryRoute);
            unbindAddressesByRoute(deliveryRoute.getId());
            deleteRouteNodeBindings(routeNodeIds);
        }
    }

    private List<String> resolveRemovedSimpleRouteNodeIds(String routeId, List<RouteNode> newNodes) {
        DeliveryRoute existingRoute = deliveryRouteRepository.findById(routeId);
        if (existingRoute == null || existingRoute.getRouteNodes() == null || existingRoute.getRouteNodes().isEmpty()) {
            return List.of();
        }
        Set<String> retainedNodeIds = new HashSet<>();
        if (newNodes != null) {
            for (RouteNode node : newNodes) {
                if (node != null && StringUtils.isNotBlank(node.getId())) {
                    retainedNodeIds.add(node.getId());
                }
            }
        }
        List<String> removedNodeIds = new ArrayList<>();
        for (RouteNode node : existingRoute.getRouteNodes()) {
            if (node != null && StringUtils.isNotBlank(node.getId()) && !retainedNodeIds.contains(node.getId())) {
                removedNodeIds.add(node.getId());
            }
        }
        return removedNodeIds;
    }

    private List<String> resolveRemovedRouteNodeIds(String routeId, List<DeliveryRouteNode> newNodes) {
        if (newNodes == null) {
            return List.of();
        }
        List<DeliveryRouteNode> existingNodes = deliveryRouteNodeRepository.listByRouteId(routeId);
        if (existingNodes == null || existingNodes.isEmpty()) {
            return List.of();
        }
        Set<String> retainedNodeIds = new HashSet<>();
        for (DeliveryRouteNode node : newNodes) {
            if (node == null) {
                continue;
            }
            if (StringUtils.isNotBlank(node.getId())) {
                retainedNodeIds.add(node.getId());
            }
            if (StringUtils.isNotBlank(node.getRouteNodeId())) {
                retainedNodeIds.add(node.getRouteNodeId());
            }
        }
        List<String> removedNodeIds = new ArrayList<>();
        for (DeliveryRouteNode node : existingNodes) {
            if (node == null) {
                continue;
            }
            boolean retained = StringUtils.isNotBlank(node.getId()) && retainedNodeIds.contains(node.getId())
                    || StringUtils.isNotBlank(node.getRouteNodeId()) && retainedNodeIds.contains(node.getRouteNodeId());
            if (!retained) {
                addIfNotBlank(removedNodeIds, node.getId());
                addIfNotBlank(removedNodeIds, node.getRouteNodeId());
            }
        }
        return removedNodeIds;
    }

    private List<String> collectRouteNodeIdentifiers(List<DeliveryRouteNode> routeNodes) {
        List<String> routeNodeIds = new ArrayList<>();
        if (routeNodes == null) {
            return routeNodeIds;
        }
        for (DeliveryRouteNode node : routeNodes) {
            if (node == null) {
                continue;
            }
            addIfNotBlank(routeNodeIds, node.getId());
            addIfNotBlank(routeNodeIds, node.getRouteNodeId());
        }
        return routeNodeIds;
    }

    private void addIfNotBlank(List<String> values, String value) {
        if (StringUtils.isNotBlank(value) && !values.contains(value)) {
            values.add(value);
        }
    }

    private void assertNoInProductionOrderBinding(String routeId, List<String> nodeIds, String message) {
        if (hasInProductionOrderBinding(orderInfoRepository, routeId, nodeIds)
                || hasInProductionOrderBinding(orderItemRepository, routeId, nodeIds)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, message);
        }
    }

    private void clearRouteBindings(String routeId, List<String> nodeIds) {
        clearOrderBindings(orderInfoRepository, routeId, nodeIds);
        clearOrderBindings(orderItemRepository, routeId, nodeIds);
        clearProductionPieceBindings(routeId, nodeIds);
    }

    private <T extends com.mes.domain.base.BaseEntity> void clearOrderBindings(com.mes.domain.base.repository.BaseRepository<T> repository, String routeId, List<String> nodeIds) {
        if (nodeIds != null && nodeIds.isEmpty()) {
            return;
        }
        Map<String, Object> filters = buildRouteBindingFilters(routeId, nodeIds);
        while (true) {
            List<T> records = repository.filterList(1, 100, filters);
            if (records == null || records.isEmpty()) {
                break;
            }
            for (com.mes.domain.base.BaseEntity record : records) {
                if (record instanceof OrderInfo orderInfo) {
                    orderInfo.setRouteId(null);
                    orderInfo.setRouteNodeId(null);
                } else if (record instanceof OrderItem orderItem) {
                    orderItem.setRouteId(null);
                    orderItem.setRouteNodeId(null);
                }
            }
            repository.batchUpdate(records);
        }
    }

    private void clearProductionPieceBindings(String routeId, List<String> nodeIds) {
        if (nodeIds != null && nodeIds.isEmpty()) {
            return;
        }
        Map<String, Object> filters = buildRouteBindingFilters(routeId, nodeIds);
        while (true) {
            List<ProductionPiece> pieces = productionPieceRepository.filterList(1, 100, filters);
            if (pieces == null || pieces.isEmpty()) {
                break;
            }
            for (ProductionPiece piece : pieces) {
                piece.setRouteId(null);
                piece.setRouteNodeId(null);
            }
            productionPieceRepository.batchUpdate(pieces);
        }
    }

    private Map<String, Object> buildRouteBindingFilters(String routeId, List<String> nodeIds) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("routeId", routeId);
        if (nodeIds != null && !nodeIds.isEmpty()) {
            filters.put("routeNodeId_in", nodeIds);
        }
        return filters;
    }

    private boolean hasInProductionOrderBinding(com.mes.domain.base.repository.BaseRepository<?> repository, String routeId, List<String> nodeIds) {
        if (nodeIds != null && nodeIds.isEmpty()) {
            return false;
        }
        Map<String, Object> filters = new HashMap<>();
        filters.put("status", OrderStatus.IN_PRODUCTION.getCode());
        filters.put("routeId", routeId);
        if (nodeIds != null && !nodeIds.isEmpty()) {
            filters.put("routeNodeId_in", nodeIds);
        }
        return repository.filterTotal(filters) > 0;
    }

    private void unbindAddressesByRoute(String routeId) {
        long current = 1;
        int size = 100;
        while (true) {
            List<AddressRecognitionRecord> records = addressRecognitionRecordRepository.listAssignedByRoute(routeId, current, size);
            if (records == null || records.isEmpty()) {
                break;
            }
            records.forEach(this::unbindAddressRecognitionRecord);
            if (records.size() < size) {
                break;
            }
        }
    }

    private void unbindAddressesByRouteNodes(String routeId, List<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return;
        }
        for (String nodeId : nodeIds) {
            long current = 1;
            int size = 100;
            while (true) {
                List<AddressRecognitionRecord> records = addressRecognitionRecordRepository.listAssignedByRouteNode(null, routeId, nodeId, null, current, size);
                if (records == null || records.isEmpty()) {
                    break;
                }
                records.forEach(this::unbindAddressRecognitionRecord);
                if (records.size() < size) {
                    break;
                }
            }
        }
    }

    private void deleteRouteNodeBindings(List<String> routeNodeIds) {
        if (routeNodeIds == null || routeNodeIds.isEmpty()) {
            return;
        }
        List<DeliveryRouteNodeBinding> bindings = deliveryRouteNodeBindingRepository.listByRouteNodeIds(routeNodeIds);
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        for (DeliveryRouteNodeBinding binding : bindings) {
            deliveryRouteNodeBindingRepository.delete(binding);
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

    public Map<String, DeliveryRoute> findByIds(java.util.Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return deliveryRouteRepository.findByIds(ids);
    }

    /**
     * 根据路线ID获取配送路线
     */
    public DeliveryRoute findByRouteId(String routeId) {
        if (StringUtils.isBlank(routeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线ID不能为空");
        }
        DeliveryRoute route = deliveryRouteRepository.findByRouteId(routeId);
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
        if (StringUtils.isBlank(node.getRouteNodeId())) {
            node.setRouteNodeId(IdGenerator.generateId("RN"));
        }

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
            List<String> removedNodeIds = collectRouteNodeIdentifiers(List.of(removedNode));
            assertNoInProductionOrderBinding(routeId, removedNodeIds, "存在生产中的订单绑定了该路线节点，不能删除节点");
            nodes.removeIf(node -> nodeId.equals(node.getId()));
            deliveryRouteNodeRepository.delete(removedNode);
            unbindAddressesByRouteNodes(routeId, removedNodeIds);
            clearRouteBindings(routeId, removedNodeIds);
            deleteRouteNodeBindings(removedNodeIds);

            int order = 0;
            for (DeliveryRouteNode node : nodes) {
                node.setNodeOrder(order++);
            }

            if (!nodes.isEmpty()) {
                deliveryRouteNodeRepository.batchUpdate(nodes);
            }
        }
    }


    public List<AddressRecognitionRecord> listUnassignedAddressRecognitionRecords(String manufacturerMetaId, String detailAddress, long current, int size) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId不能为空");
        }
        if (current <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "页码必须大于 0");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        return addressRecognitionRecordRepository.listByStatus(AddressRecognitionRecordStatus.UNASSIGNED.getValue(), manufacturerMetaId, detailAddress, current, size);
    }

    public long countUnassignedAddressRecognitionRecords(String manufacturerMetaId, String detailAddress) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId不能为空");
        }
        return addressRecognitionRecordRepository.totalByStatus(AddressRecognitionRecordStatus.UNASSIGNED.getValue(), manufacturerMetaId, detailAddress);
    }

    public List<AddressRecognitionRecord> listAssignedAddressRecognitionRecords(String manufacturerMetaId, String routeId, String nodeId, String detailAddress, long current, int size) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId不能为空");
        }
        if (current <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "页码必须大于 0");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        return addressRecognitionRecordRepository.listAssignedByRouteNode(manufacturerMetaId, routeId, nodeId, detailAddress, current, size);
    }

    public long countAssignedAddressRecognitionRecords(String manufacturerMetaId, String routeId, String nodeId, String detailAddress) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId不能为空");
        }
        return addressRecognitionRecordRepository.totalAssignedByRouteNode(manufacturerMetaId, routeId, nodeId, detailAddress);
    }

    public void bindAddressRecognitionRecords(List<String> recordIds, String routeId, String nodeId, Integer order) {
        if (recordIds == null || recordIds.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "地址识别记录不能为空");
        }
        if (StringUtils.isBlank(routeId) || StringUtils.isBlank(nodeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "路线和节点不能为空");
        }
        for (String recordId : recordIds) {
            bindAddressRecognitionRecord(recordId, routeId, nodeId, order);
        }
    }

    public void bindAddressRecognitionRecord(String recordId, String routeId, String nodeId, Integer order) {
        if (StringUtils.isBlank(recordId) || StringUtils.isBlank(routeId) || StringUtils.isBlank(nodeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "绑定参数不能为空");
        }
        AddressRecognitionRecord record = addressRecognitionRecordRepository.findById(recordId);
        if (record == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "地址识别记录不存在");
        }
        record.setRouteId(routeId);
        record.setNodeId(nodeId);
        record.setOrder(resolveAddressRecognitionRecordOrder(routeId, nodeId, order));
        record.setStatus(AddressRecognitionRecordStatus.ASSIGNED);
        addressRecognitionRecordRepository.update(record);
        syncAddressRecognitionRouteBinding(record, routeId, nodeId);
    }


    private Integer resolveAddressRecognitionRecordOrder(String routeId, String nodeId, Integer order) {
        if (order != null) {
            return order;
        }
        Integer maxOrder = addressRecognitionRecordRepository.findMaxOrderByRouteNode(routeId, nodeId);
        return maxOrder == null ? 0 : maxOrder + 1;
    }

    private void syncAddressRecognitionRouteBinding(AddressRecognitionRecord record, String routeId, String nodeId) {
        if (record == null) {
            return;
        }

        Set<String> orderIds = new HashSet<>();
        if (StringUtils.isNotBlank(record.getOrderId())) {
            orderIds.add(record.getOrderId());
        }
        orderIds.addAll(listProductionOrderIdsByAddress(record));

        for (String orderId : orderIds) {
            syncOrderRouteBinding(orderId, routeId, nodeId);
        }
    }

    private List<String> listProductionOrderIdsByAddress(AddressRecognitionRecord record) {
        if (record == null || record.getAddress() == null
                || StringUtils.isBlank(record.getAddress().getTerminalRegionCode())
                || StringUtils.isBlank(record.getAddress().getDetailAddress())) {
            return List.of();
        }

        Map<String, Object> orderFilters = new HashMap<>();
        orderFilters.put("customer.address.terminalRegionCode", record.getAddress().getTerminalRegionCode());
        orderFilters.put("customer.address.detailAddress", record.getAddress().getDetailAddress());
        orderFilters.put("status_in", List.of(
                OrderStatus.PENDING.getCode(),
                OrderStatus.IN_PRODUCTION.getCode()
        ));

        List<String> orderIds = new ArrayList<>();
        long current = 1;
        int size = 100;
        while (true) {
            List<OrderInfo> orderInfos = orderInfoRepository.filterList(current, size, orderFilters);
            if (orderInfos == null || orderInfos.isEmpty()) {
                break;
            }
            for (OrderInfo orderInfo : orderInfos) {
                if (orderInfo != null && StringUtils.isNotBlank(orderInfo.getOrderId())) {
                    orderIds.add(orderInfo.getOrderId());
                }
            }
            if (orderInfos.size() < size) {
                break;
            }
            current++;
        }
        return orderIds;
    }

    private void syncOrderRouteBinding(String orderId, String routeId, String nodeId) {
        if (StringUtils.isBlank(orderId)) {
            return;
        }

        Map<String, Object> orderFilters = new HashMap<>();
        orderFilters.put("orderId", orderId);
        List<OrderInfo> orderInfos = orderInfoRepository.filterList(1, 1, orderFilters);
        if (orderInfos == null || orderInfos.isEmpty()) {
            return;
        }
        OrderInfo orderInfo = orderInfos.get(0);
        // Pending orders can already have route-managed pieces, so both active
        // states participate in hierarchy synchronization. Final/inactive
        // states must keep their existing hierarchy untouched.
        if (orderInfo == null || (orderInfo.getStatus() != OrderStatus.PENDING
                && orderInfo.getStatus() != OrderStatus.IN_PRODUCTION)) {
            return;
        }
        orderInfo.setRouteId(routeId);
        orderInfo.setRouteNodeId(nodeId);
        orderInfoRepository.update(orderInfo);

        Map<String, Object> itemFilters = new HashMap<>();
        itemFilters.put("orderId", orderId);
        long current = 1;
        int size = 100;
        while (true) {
            List<OrderItem> orderItems = orderItemRepository.filterList(current, size, itemFilters);
            if (orderItems == null || orderItems.isEmpty()) {
                break;
            }
            for (OrderItem orderItem : orderItems) {
                if (orderItem == null) {
                    continue;
                }
                orderItem.setRouteId(routeId);
                orderItem.setRouteNodeId(nodeId);
                syncProductionPiecesRouteBinding(orderItem.getOrderItemId(), routeId, nodeId);
            }
            orderItemRepository.batchUpdate(orderItems);
            if (orderItems.size() < size) {
                break;
            }
            current++;
        }
    }

    private void syncProductionPiecesRouteBinding(String orderItemId, String routeId, String nodeId) {
        if (StringUtils.isBlank(orderItemId)) {
            return;
        }

        Map<String, Object> pieceFilters = new HashMap<>();
        pieceFilters.put("orderItemId", orderItemId);
        long current = 1;
        int size = 100;
        while (true) {
            List<ProductionPiece> productionPieces = productionPieceRepository.filterList(current, size, pieceFilters);
            if (productionPieces == null || productionPieces.isEmpty()) {
                break;
            }
            List<ProductionPiece> productionPiecesToUpdate = new ArrayList<>();
            for (ProductionPiece productionPiece : productionPieces) {
                if (productionPiece == null) {
                    continue;
                }
                productionPiece.setRouteId(routeId);
                productionPiece.setRouteNodeId(nodeId);
                productionPiecesToUpdate.add(productionPiece);
            }
            if (!productionPiecesToUpdate.isEmpty()) {
                productionPieceRepository.batchUpdate(productionPiecesToUpdate);
            }
            if (productionPieces.size() < size) {
                break;
            }
            current++;
        }
    }

    public void unbindAddressRecognitionRecords(List<String> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "地址识别记录 ID 不能为空");
        }
        for (String recordId : recordIds) {
            unbindAddressRecognitionRecord(recordId);
        }
    }

    public void unbindAddressRecognitionRecord(String recordId) {
        if (StringUtils.isBlank(recordId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "地址识别记录 ID 不能为空");
        }
        AddressRecognitionRecord record = addressRecognitionRecordRepository.findById(recordId);
        if (record == null) {
            return;
        }
        unbindAddressRecognitionRecord(record);
    }

    private void unbindAddressRecognitionRecord(AddressRecognitionRecord record) {
        record.setRouteId(null);
        record.setNodeId(null);
        record.setOrder(null);
        record.setStatus(AddressRecognitionRecordStatus.UNASSIGNED);
        addressRecognitionRecordRepository.update(record);
        clearAddressRecognitionRouteBinding(record);
    }

    private void clearAddressRecognitionRouteBinding(AddressRecognitionRecord record) {
        if (record == null) {
            return;
        }

        Set<String> orderIds = new HashSet<>();
        if (StringUtils.isNotBlank(record.getOrderId())) {
            orderIds.add(record.getOrderId());
        }
        orderIds.addAll(listProductionOrderIdsByAddress(record));

        for (String orderId : orderIds) {
            clearOrderRouteBinding(orderId);
        }
    }

    private void clearOrderRouteBinding(String orderId) {
        if (StringUtils.isBlank(orderId)) {
            return;
        }

        Map<String, Object> orderFilters = new HashMap<>();
        orderFilters.put("orderId", orderId);
        List<OrderInfo> orderInfos = orderInfoRepository.filterList(1, 1, orderFilters);
        if (orderInfos != null && !orderInfos.isEmpty()) {
            OrderInfo orderInfo = orderInfos.get(0);
            orderInfo.setRouteId(null);
            orderInfo.setRouteNodeId(null);
            orderInfoRepository.update(orderInfo);
        }

        Map<String, Object> itemFilters = new HashMap<>();
        itemFilters.put("orderId", orderId);
        long current = 1;
        int size = 100;
        while (true) {
            List<OrderItem> orderItems = orderItemRepository.filterList(current, size, itemFilters);
            if (orderItems == null || orderItems.isEmpty()) {
                break;
            }
            for (OrderItem orderItem : orderItems) {
                orderItem.setRouteId(null);
                orderItem.setRouteNodeId(null);
                clearProductionPiecesRouteBinding(orderItem.getOrderItemId());
            }
            orderItemRepository.batchUpdate(orderItems);
            if (orderItems.size() < size) {
                break;
            }
            current++;
        }
    }

    private void clearProductionPiecesRouteBinding(String orderItemId) {
        syncProductionPiecesRouteBinding(orderItemId, null, null);
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
        Map<String, List<DeliveryRouteNode>> nodesByRouteId = deliveryRouteNodeRepository.listByRouteIds(
                        routes.stream().filter(Objects::nonNull).map(DeliveryRoute::getId)
                                .filter(StringUtils::isNotBlank).distinct().toList())
                .stream().collect(Collectors.groupingBy(DeliveryRouteNode::getRouteId));
        for (DeliveryRoute route : routes) {
            if (route != null && StringUtils.isNotBlank(route.getId())) {
                route.setDeliveryRouteNodes(nodesByRouteId.getOrDefault(route.getId(), Collections.emptyList()));
            }
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

        List<DeliveryRouteNode> linkedNodes = new ArrayList<>();
        for (DeliveryRouteNode node : routeNodes) {
            if (node != null) {
                linkedNodes.add(node);
            }
        }
        if (linkedNodes.size() < 2) {
            return;
        }

        List<DeliveryRouteNode> nodesToSave = new ArrayList<>();
        for (int i = 0; i < linkedNodes.size() - 1; i++) {
            DeliveryRouteNode currentNode = linkedNodes.get(i);
            DeliveryRouteNode nextNode = linkedNodes.get(i + 1);

            currentNode.setDestCountryCode(nextNode.getCountryCode());
            currentNode.setDestCountryName(nextNode.getCountryName());
            currentNode.setDestProvinceCode(nextNode.getProvinceCode());
            currentNode.setDestProvinceName(nextNode.getProvinceName());
            currentNode.setDestCityCode(nextNode.getCityCode());
            currentNode.setDestCityName(nextNode.getCityName());
            currentNode.setDestDistrictCode(nextNode.getDistrictCode());
            currentNode.setDestDistrictName(nextNode.getDistrictName());
            currentNode.setDestTownCode(nextNode.getTownCode());
            currentNode.setDestTownName(nextNode.getTownName());
            currentNode.setDestDetailAddress(nextNode.getDetailAddress());

            currentNode.setRouteId(routeId);
            currentNode.setId(null);
            if (StringUtils.isBlank(currentNode.getRouteNodeId())) {
                currentNode.setRouteNodeId(IdGenerator.generateId("RN"));
            }
            if (currentNode.getNodeOrder() == null) {
                currentNode.setNodeOrder(i);
            }
            currentNode.buildRegionPath();
            nodesToSave.add(currentNode);
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
