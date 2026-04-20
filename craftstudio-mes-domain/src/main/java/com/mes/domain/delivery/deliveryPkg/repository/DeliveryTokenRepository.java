package com.mes.domain.delivery.deliveryPkg.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;

/**
 * 电子面单令牌仓储接口
 */
public interface DeliveryTokenRepository extends BaseRepository<DeliveryToken> {

    /**
     * 根据承运商ID和制造商ID查询
     */
    DeliveryToken findByCarrierIdAndManufacturerMetaId(String carrierId, String manufacturerMetaId);
}
