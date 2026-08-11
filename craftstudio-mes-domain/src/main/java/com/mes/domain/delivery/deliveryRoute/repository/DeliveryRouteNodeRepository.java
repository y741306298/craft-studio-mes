package com.mes.domain.delivery.deliveryRoute.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;

import java.util.Collection;
import java.util.List;

public interface DeliveryRouteNodeRepository extends BaseRepository<DeliveryRouteNode> {

    List<DeliveryRouteNode> listByRouteId(String routeId);

    List<DeliveryRouteNode> listByRouteIds(Collection<String> routeIds);

    void removeByRouteId(String routeId);

    DeliveryRouteNode findByRouteNodeId(String routeNodeId);
}
