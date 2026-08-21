package com.mes.application.command.delivery;

import com.mes.application.dto.resp.delivery.AddressRecognitionRecordResponse;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import com.mes.domain.delivery.deliveryRoute.entity.RouteNode;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeRepository;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
import com.mes.domain.delivery.deliveryRoute.service.DeliveryRouteService;
import com.mes.application.dto.resp.delivery.DeliveryRouteNodeBindingMatchResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import com.piliofpala.craftstudio.shared.domain.geo.world.repository.WorldRepository;
import com.piliofpala.craftstudio.shared.domain.geo.world.vo.World;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppDeliveryRouteService {

    @Autowired
    private DeliveryRouteService domainDeliveryRouteService;

    @Autowired
    private DeliveryRouteRepository deliveryRouteRepository;

    @Autowired
    private DeliveryRouteNodeRepository deliveryRouteNodeRepository;

    @Autowired
    private WorldRepository worldRepository;

    public PagedResult<DeliveryRoute> findDeliveryRoutes(String routeName, String manufacturerId, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }

        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        if (StringUtils.isBlank(manufacturerId)) {
            throw new IllegalArgumentException("厂商 ID 不能为空");
        }

        List<DeliveryRoute> items;
        long total;

        if (StringUtils.isBlank(routeName)) {
            items = deliveryRouteRepository.listByManufacturerId(manufacturerId, query.getCurrent(), query.getSize());
            total = deliveryRouteRepository.totalByManufacturerId(manufacturerId);
        } else {
            items = domainDeliveryRouteService.findDeliveryRoutesByName(routeName, manufacturerId, (int) query.getCurrent(), query.getSize());
            total = domainDeliveryRouteService.getTotalCount(routeName, manufacturerId);
        }
        return new PagedResult<DeliveryRoute>(items, total, query.getSize(), query.getCurrent());
    }

    public DeliveryRoute addDeliveryRoute(DeliveryRoute command) {
        if (command == null) {
            throw new IllegalArgumentException("配送路线不能为空");
        }
        if (StringUtils.isBlank(command.getManufacturerMetaId())) {
            throw new IllegalArgumentException("厂商 ID 不能为空");
        }
        return domainDeliveryRouteService.addDeliveryRoute(command);
    }

    public void updateDeliveryRoute(DeliveryRoute command) {
        if (command == null) {
            throw new IllegalArgumentException("配送路线不能为空");
        }
        if (StringUtils.isBlank(command.getId())) {
            throw new IllegalArgumentException("配送路线 ID 不能为空");
        }
        domainDeliveryRouteService.updateDeliveryRoute(command);
    }

    public void deleteDeliveryRoute(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainDeliveryRouteService.deleteDeliveryRoute(id);
    }

    public DeliveryRoute findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        return domainDeliveryRouteService.findById(id);
    }

    public void activateDeliveryRoute(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainDeliveryRouteService.activateDeliveryRoute(id);
    }

    public void deactivateDeliveryRoute(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainDeliveryRouteService.deactivateDeliveryRoute(id);
    }

    public void addRouteNode(String routeId, DeliveryRouteNode node) {
        if (StringUtils.isBlank(routeId)) {
            throw new IllegalArgumentException("路线 ID 不能为空");
        }
        if (node == null) {
            throw new IllegalArgumentException("路线节点不能为空");
        }
        domainDeliveryRouteService.addRouteNode(routeId, node);
    }

    public void removeRouteNode(String routeId, String nodeId) {
        if (StringUtils.isBlank(routeId)) {
            throw new IllegalArgumentException("路线 ID 不能为空");
        }
        if (StringUtils.isBlank(nodeId)) {
            throw new IllegalArgumentException("节点 ID 不能为空");
        }
        domainDeliveryRouteService.removeRouteNode(routeId, nodeId);
    }

    public PagedResult<AddressRecognitionRecordResponse> listAddressRecognitionRecords(
            String manufacturerMetaId, String routeId, String nodeId, String status, Boolean assigned, String detailAddress, PagedQuery query) {
        boolean queryAssigned = Boolean.TRUE.equals(assigned)
                || "ASSIGNED".equalsIgnoreCase(status)
                || "已分配".equals(status);
        if (queryAssigned) {
            return listAssignedAddressRecognitionRecords(manufacturerMetaId, routeId, nodeId, detailAddress, query);
        }
        return listUnassignedAddressRecognitionRecords(manufacturerMetaId, routeId, detailAddress, query);
    }


    public PagedResult<AddressRecognitionRecordResponse> listUnassignedAddressRecognitionRecords(String manufacturerMetaId, String routeId, String detailAddress, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("厂商 ID 不能为空");
        }

        World world = worldRepository.loadWorld();
        if (StringUtils.isBlank(routeId)) {
            List<AddressRecognitionRecord> records = domainDeliveryRouteService.listUnassignedAddressRecognitionRecords(
                    manufacturerMetaId, detailAddress, query.getCurrent(), query.getSize()
            );
            long total = domainDeliveryRouteService.countUnassignedAddressRecognitionRecords(manufacturerMetaId, detailAddress);
            List<AddressRecognitionRecordResponse> responses = records.stream()
                    .map(record -> toAddressRecognitionRecordResponse(record, world))
                    .toList();
            return new PagedResult<>(responses, total, query.getSize(), query.getCurrent());
        }

        List<AddressRecognitionRecordResponse> matchedResponses = listMatchedUnassignedAddressRecognitionRecords(
                manufacturerMetaId, routeId, detailAddress
        );
        long total = matchedResponses.size();
        int fromIndex = (int) Math.min((query.getCurrent() - 1) * query.getSize(), total);
        int toIndex = (int) Math.min(fromIndex + query.getSize(), total);
        return new PagedResult<>(matchedResponses.subList(fromIndex, toIndex), total, query.getSize(), query.getCurrent());
    }


    public PagedResult<AddressRecognitionRecordResponse> listAssignedAddressRecognitionRecords(
            String manufacturerMetaId, String routeId, String nodeId, String detailAddress, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("厂商 ID 不能为空");
        }

        List<AddressRecognitionRecord> records = domainDeliveryRouteService.listAssignedAddressRecognitionRecords(
                manufacturerMetaId, routeId, nodeId, detailAddress, query.getCurrent(), query.getSize()
        );
        long total = domainDeliveryRouteService.countAssignedAddressRecognitionRecords(manufacturerMetaId, routeId, nodeId, detailAddress);
        World world = worldRepository.loadWorld();
        List<AddressRecognitionRecordResponse> responses = records.stream()
                .map(record -> toAddressRecognitionRecordResponse(record, world))
                .toList();
        return new PagedResult<>(responses, total, query.getSize(), query.getCurrent());
    }

    private List<AddressRecognitionRecordResponse> listMatchedUnassignedAddressRecognitionRecords(
            String manufacturerMetaId, String routeId, String detailAddress) {
        List<AddressRecognitionRecordResponse> matchedResponses = new ArrayList<>();
        long current = 1;
        int size = 100;
        World world = worldRepository.loadWorld();
        while (true) {
            List<AddressRecognitionRecord> records = domainDeliveryRouteService.listUnassignedAddressRecognitionRecords(
                    manufacturerMetaId, detailAddress, current, size
            );
            if (records.isEmpty()) {
                break;
            }
            records.stream()
                    .map(record -> toAddressRecognitionRecordResponse(record, world))
                    .filter(response -> response != null && routeId.equals(response.getRouteId()))
                    .forEach(matchedResponses::add);
            if (records.size() < size) {
                break;
            }
            current++;
        }
        return matchedResponses;
    }

    private AddressRecognitionRecordResponse toAddressRecognitionRecordResponse(AddressRecognitionRecord record, World world) {
        AddressRecognitionRecordResponse response = AddressRecognitionRecordResponse.from(record, world);
        if (response == null || record == null) {
            return response;
        }

        if (StringUtils.isBlank(record.getRouteId())) {
            fillMatchedRouteNodeInfo(response, record);
            return response;
        }

        fillBoundRouteNodeInfo(response, record.getRouteId(), record.getNodeId());
        return response;
    }

    private void fillMatchedRouteNodeInfo(AddressRecognitionRecordResponse response, AddressRecognitionRecord record) {
        if (record.getAddress() == null
                || StringUtils.isBlank(record.getManufacturerMetaId())
                || StringUtils.isBlank(record.getAddress().getTerminalRegionCode())
                || StringUtils.isBlank(record.getAddress().getDetailAddress())) {
            return;
        }

        DeliveryRouteService.RouteNodeMatchResult matchResult = domainDeliveryRouteService.matchRouteNodeByAddress(
                record.getManufacturerMetaId(),
                record.getAddress().getTerminalRegionCode(),
                record.getAddress().getDetailAddress()
        );
        if (matchResult == null || !matchResult.isMatched()) {
            return;
        }

        DeliveryRoute route = matchResult.getDeliveryRoute();
        DeliveryRouteNode node = matchResult.getDeliveryRouteNode();
        if (route != null) {
            response.setRouteId(route.getRouteId());
            response.setRouteName(route.getRouteName());
        }
        if (node != null) {
            response.setNodeId(node.getId());
            response.setNodeName(resolveDeliveryRouteNodeName(node));
        }
    }

    private void fillBoundRouteNodeInfo(AddressRecognitionRecordResponse response, String routeId, String nodeId) {
        DeliveryRoute route = findDeliveryRoute(routeId);
        if (route != null) {
            response.setRouteName(route.getRouteName());
            response.setNodeName(resolveRouteNodeName(route, nodeId));
        }
        if (StringUtils.isBlank(response.getNodeName())) {
            response.setNodeName(resolveDeliveryRouteNodeName(nodeId));
        }
    }

    private DeliveryRoute findDeliveryRoute(String routeId) {
        DeliveryRoute route = deliveryRouteRepository.findById(routeId);
        if (route == null) {
            route = deliveryRouteRepository.findByRouteId(routeId);
        }
        return route;
    }

    private String resolveRouteNodeName(DeliveryRoute route, String nodeId) {
        if (route == null || route.getRouteNodes() == null || StringUtils.isBlank(nodeId)) {
            return null;
        }
        for (RouteNode node : route.getRouteNodes()) {
            if (node != null && nodeId.equals(node.getId())) {
                return node.getName();
            }
        }
        return null;
    }

    private String resolveDeliveryRouteNodeName(String nodeId) {
        if (StringUtils.isBlank(nodeId)) {
            return null;
        }
        DeliveryRouteNode node = deliveryRouteNodeRepository.findById(nodeId);
        if (node == null) {
            node = deliveryRouteNodeRepository.findByRouteNodeId(nodeId);
        }
        if (node == null) {
            return null;
        }
        return resolveDeliveryRouteNodeName(node);
    }

    private String resolveDeliveryRouteNodeName(DeliveryRouteNode node) {
        if (node == null) {
            return null;
        }
        String startName = node.getStartFullRegionName();
        String destName = node.getDestFullRegionName();
        if (StringUtils.isBlank(destName) || destName.equals(startName)) {
            return startName;
        }
        if (StringUtils.isBlank(startName)) {
            return destName;
        }
        return startName + "-" + destName;
    }

    public void bindAddressRecognitionRecord(String recordId, String routeId, String nodeId, Integer order) {
        domainDeliveryRouteService.bindAddressRecognitionRecord(recordId, routeId, nodeId, order);
    }

    public void batchBindAddressRecognitionRecords(List<String> recordIds, String routeId, String nodeId, Integer order) {
        domainDeliveryRouteService.bindAddressRecognitionRecords(recordIds, routeId, nodeId, order);
    }

    public void batchChangeAddressRecognitionRecordBinding(List<String> recordIds, String routeId, String nodeId, Integer order) {
        domainDeliveryRouteService.bindAddressRecognitionRecords(recordIds, routeId, nodeId, order);
    }

    public void unbindAddressRecognitionRecord(String recordId) {
        domainDeliveryRouteService.unbindAddressRecognitionRecord(recordId);
    }

    public void batchUnbindAddressRecognitionRecords(List<String> recordIds) {
        domainDeliveryRouteService.unbindAddressRecognitionRecords(recordIds);
    }

    public void bindTerminalAddressToRouteNode(String terminalRegionCode, String detailAddress, String routeNodeId) {
        if (StringUtils.isBlank(terminalRegionCode) || StringUtils.isBlank(detailAddress) || StringUtils.isBlank(routeNodeId)) {
            throw new IllegalArgumentException("绑定参数不能为空");
        }
        domainDeliveryRouteService.bindTerminalAddressToRouteNode(terminalRegionCode, detailAddress, routeNodeId);
    }

    public DeliveryRouteNodeBindingMatchResponse matchRouteByAddress(String manufacturerMetaId, String terminalRegionCode, String detailAddress) {
        if (StringUtils.isBlank(manufacturerMetaId) || StringUtils.isBlank(terminalRegionCode) || StringUtils.isBlank(detailAddress)) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
        DeliveryRouteService.RouteNodeMatchResult result = domainDeliveryRouteService
                .matchRouteNodeByAddress(manufacturerMetaId, terminalRegionCode, detailAddress);
        if (!result.isMatched()) {
            return DeliveryRouteNodeBindingMatchResponse.unmatched();
        }
        return DeliveryRouteNodeBindingMatchResponse.matched(result.getDeliveryRoute(), result.getDeliveryRouteNode());
    }
}
