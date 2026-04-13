package com.mes.domain.delivery.deliveryPkg.service;

import com.mes.application.dto.resp.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryManRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeliveryManService {

    @Autowired
    private DeliveryManRepository deliveryManRepository;

    public DeliveryMan addDeliveryMan(DeliveryMan deliveryMan) {
        if (deliveryMan == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "快递员信息不能为空");
        }
        if (StringUtils.isBlank(deliveryMan.getName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "快递员姓名不能为空");
        }

        return deliveryManRepository.add(deliveryMan);
    }

    public void updateDeliveryMan(DeliveryMan deliveryMan) {
        if (deliveryMan == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "快递员信息不能为空");
        }
        if (StringUtils.isBlank(deliveryMan.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "快递员ID不能为空");
        }

        deliveryManRepository.update(deliveryMan);
    }

    public void deleteDeliveryMan(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }

        DeliveryMan deliveryMan = deliveryManRepository.findById(id);
        if (deliveryMan != null) {
            deliveryManRepository.delete(deliveryMan);
        }
    }

    public DeliveryMan findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }
        return deliveryManRepository.findById(id);
    }

    public List<DeliveryMan> list(int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在1-100之间");
        }
        return deliveryManRepository.list(current, size);
    }

    public long total() {
        return deliveryManRepository.total();
    }
}
