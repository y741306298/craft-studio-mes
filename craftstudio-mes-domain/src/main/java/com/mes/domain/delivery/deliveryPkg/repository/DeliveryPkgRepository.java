package com.mes.domain.delivery.deliveryPkg.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.delivery.deliveryPkg.enums.DeliveryPkgStatus;

import java.util.List;
import java.util.Map;

/**
 * 包裹仓储接口
 */
public interface DeliveryPkgRepository extends BaseRepository<DeliveryPkg> {

    /**
     * 根据包裹编码查询
     */
    List<DeliveryPkg> findByDeliveryPkgCode(String deliveryPkgCode);

    /**
     * 根据包裹ID查询
     */
    List<DeliveryPkg> findByDeliveryPkgId(String deliveryPkgId);

    /**
     * 根据状态查询包裹
     */
    List<DeliveryPkg> findByStatus(DeliveryPkgStatus status);

    /**
     * 根据运单号查询
     */
    List<DeliveryPkg> findByTrackingNumber(String trackingNumber);

    /**
     * 根据收件人姓名模糊查询
     */
    List<DeliveryPkg> findByRecipientName(String recipientName);

    /**
     * 查询待打包的包裹
     */
    List<DeliveryPkg> findPendingPacking();

    /**
     * 查询已发货的包裹
     */
    List<DeliveryPkg> findDelivered();

    /**
     * 根据条件全量查询包裹，不应用分页限制。
     */
    List<DeliveryPkg> findAllByConditions(Map<String, Object> filters);
}
