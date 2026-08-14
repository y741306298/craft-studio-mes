package com.mes.domain.delivery.deliveryRoute.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;

import java.util.List;
import java.util.Collection;

public interface DeliveryRouteRepository extends BaseRepository<DeliveryRoute> {

    /**
     * 根据厂商ID查询配送路线列表
     */
    List<DeliveryRoute> listByManufacturerId(String manufacturerId, long current, int size);

    /**
     * 根据厂商ID统计配送路线总数
     */
    long totalByManufacturerId(String manufacturerId);

    /**
     * 根据路线ID查询配送路线
     */
    DeliveryRoute findByRouteId(String routeId);

    /** Batch query used by list responses to avoid one route lookup per item. */
    List<DeliveryRoute> findByRouteIds(Collection<String> routeIds);
}
