package com.mes.domain.delivery.deliveryNet.service;

import com.mes.domain.delivery.deliveryNet.enums.DeliveryWayNUM;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DeliveryWayService {

    /**
     * 获取所有快递类型枚举
     * @return 所有快递类型枚举列表
     */
    public List<DeliveryWayNUM> getAllDeliveryWayNUMs() {
        return Arrays.asList(DeliveryWayNUM.values());
    }
}
