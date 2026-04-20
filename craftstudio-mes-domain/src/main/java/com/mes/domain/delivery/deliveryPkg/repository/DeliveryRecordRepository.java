package com.mes.domain.delivery.deliveryPkg.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryRecord;

import java.util.List;

public interface DeliveryRecordRepository extends BaseRepository<DeliveryRecord> {

    /**
     * 根据订单ID查询发货记录
     */
    List<DeliveryRecord> findByOrderId(String orderId);

    /**
     * 根据运单号查询发货记录
     */
    DeliveryRecord findByTrackingNumber(String trackingNumber);

    /**
     * 根据制造商ID查询发货记录列表
     */
    List<DeliveryRecord> findByManufacturerMetaId(String manufacturerMetaId, int current, int size);
}
