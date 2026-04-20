package com.mes.domain.delivery.deliveryPkg.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;

import java.util.List;

public interface DeliveryManRepository extends BaseRepository<DeliveryMan> {
    
    /**
     * 根据用户ID查询快递员列表
     */
    List<DeliveryMan> findByUserId(String userId);

    /**
     * 根据发货人ID和制造商ID查询
     */
    DeliveryMan findByDeliveryManIdAndManufacturerMetaId(String deliveryManId, String manufacturerMetaId);
}
