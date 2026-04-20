package com.mes.domain.delivery.deliveryPkg.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;

public interface DeliverySiidRepository extends BaseRepository<DeliverySiid> {

    /**
     * 根据SIID查询
     */
    DeliverySiid findBySiid(String siid);

    /**
     * 根据用户ID查询列表
     */
    java.util.List<DeliverySiid> findByUserId(String userId);

    /**
     * 根据SIID ID和制造商ID查询
     */
    DeliverySiid findByDeliverySiidIdAndManufacturerMetaId(String deliverySiidId, String manufacturerMetaId);
}
