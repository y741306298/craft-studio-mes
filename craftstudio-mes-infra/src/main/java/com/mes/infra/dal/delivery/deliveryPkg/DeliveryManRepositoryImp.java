package com.mes.infra.dal.delivery.deliveryPkg;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryManRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryPkg.po.DeliveryManPO;
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryManRepositoryImp extends BaseRepositoryImp<DeliveryMan, DeliveryManPO> implements DeliveryManRepository {

    @Override
    public Class<DeliveryManPO> poClass() {
        return DeliveryManPO.class;
    }
}
