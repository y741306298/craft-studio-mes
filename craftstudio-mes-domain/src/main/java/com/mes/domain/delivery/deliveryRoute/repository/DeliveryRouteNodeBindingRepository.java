package com.mes.domain.delivery.deliveryRoute.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNodeBinding;

import java.util.List;

public interface DeliveryRouteNodeBindingRepository extends BaseRepository<DeliveryRouteNodeBinding> {

    List<DeliveryRouteNodeBinding> listByManufacturerAndTerminalRegion(String manufacturerMetaId, String terminalRegionCode);

    DeliveryRouteNodeBinding findByManufacturerAndAddress(String manufacturerMetaId, String terminalRegionCode, String detailAddress);
}
