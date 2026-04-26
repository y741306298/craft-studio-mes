package com.mes.application.command.delivery;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
import com.mes.domain.delivery.deliveryRoute.service.DeliveryRouteService;
import com.mes.application.dto.resp.delivery.DeliveryRouteNodeBindingMatchResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppDeliveryRouteService {

    @Autowired
    private DeliveryRouteService domainDeliveryRouteService;

    @Autowired
    private DeliveryRouteRepository deliveryRouteRepository;

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
        domainDeliveryRouteService.hydrateRouteNodes(items);

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
