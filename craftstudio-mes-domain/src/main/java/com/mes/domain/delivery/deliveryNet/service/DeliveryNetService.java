package com.mes.domain.delivery.deliveryNet.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryNet.entity.DeliveryNet;
import com.mes.domain.delivery.deliveryNet.repository.DeliveryNetRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeliveryNetService {

    @Autowired
    private DeliveryNetRepository deliveryNetRepository;

    /**
     * 根据名称查询配送网络（支持分页）
     */
    public List<DeliveryNet> findDeliveryNetsByName(String deliveryNetName, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(deliveryNetName)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送网络名称不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("deliveryNetName", deliveryNetName);
        return deliveryNetRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 获取配送网络总数
     */
    public long getTotalCount(String deliveryNetName) {
        if (StringUtils.isNotBlank(deliveryNetName)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("deliveryNetName", deliveryNetName);
            return deliveryNetRepository.totalByFuzzySearch(searchFilters);
        } else {
            return deliveryNetRepository.total();
        }
    }

    /**
     * 添加配送网络
     */
    public DeliveryNet addDeliveryNet(DeliveryNet deliveryNet) {
        if (deliveryNet == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送网络不能为空");
        }
        if (StringUtils.isBlank(deliveryNet.getDeliveryNetName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送网络名称不能为空");
        }

        return deliveryNetRepository.add(deliveryNet);
    }

    /**
     * 更新配送网络
     */
    public void updateDeliveryNet(DeliveryNet deliveryNet) {
        if (deliveryNet == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送网络不能为空");
        }
        if (StringUtils.isBlank(deliveryNet.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送网络 ID 不能为空");
        }
        if (StringUtils.isBlank(deliveryNet.getDeliveryNetName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送网络名称不能为空");
        }
        
        deliveryNetRepository.update(deliveryNet);
    }

    /**
     * 删除配送网络
     */
    public void deleteDeliveryNet(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        
        DeliveryNet deliveryNet = deliveryNetRepository.findById(id);
        if (deliveryNet != null) {
            deliveryNetRepository.delete(deliveryNet);
        }
    }

    /**
     * 根据ID获取配送网络
     */
    public DeliveryNet findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        return deliveryNetRepository.findById(id);
    }

    /**
     * 激活配送网络
     */
    public void activateDeliveryNet(String id) {
        DeliveryNet deliveryNet = findById(id);
        if (deliveryNet == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送网络不存在");
        }
        
        deliveryNet.setStatus("ACTIVE");
        deliveryNetRepository.update(deliveryNet);
    }

    /**
     * 停用配送网络
     */
    public void deactivateDeliveryNet(String id) {
        DeliveryNet deliveryNet = findById(id);
        if (deliveryNet == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送网络不存在");
        }
        
        deliveryNet.setStatus("INACTIVE");
        deliveryNetRepository.update(deliveryNet);
    }

    /**
     * 暂停配送网络
     */
    public void suspendDeliveryNet(String id) {
        DeliveryNet deliveryNet = findById(id);
        if (deliveryNet == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "配送网络不存在");
        }
        
        deliveryNet.setStatus("SUSPENDED");
        deliveryNetRepository.update(deliveryNet);
    }
}
