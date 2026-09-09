package com.mes.domain.order.orderInfo.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.enums.OrderStatus;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface OrderInfoRepository extends BaseRepository<OrderInfo> {

    List<OrderInfo> findByOrderIds(Collection<String> orderIds);

    List<OrderInfo> findByAddressesAndStatuses(Collection<Address> addresses, Collection<OrderStatus> statuses);

    boolean tryAcquireTransferLock(String id, String lockToken, Date expiredBefore);

    void releaseTransferLock(String id, String lockToken);

}
