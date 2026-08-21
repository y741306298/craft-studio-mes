package com.mes.domain.delivery.deliveryRoute.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;

import java.util.Collection;
import java.util.List;

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

    /**
     * 批量根据 MongoDB 主键或业务路线 ID 查询配送路线。
     */
    List<DeliveryRoute> findByIdsOrRouteIds(Collection<String> routeIds);
}
