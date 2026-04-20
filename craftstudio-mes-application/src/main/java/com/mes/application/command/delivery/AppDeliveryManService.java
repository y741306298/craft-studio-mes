package com.mes.application.command.delivery;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.delivery.deliveryPkg.service.DeliveryManService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppDeliveryManService {

    @Autowired
    private DeliveryManService domainDeliveryManService;

    public DeliveryMan addDeliveryMan(DeliveryMan deliveryMan) {
        if (deliveryMan == null) {
            throw new IllegalArgumentException("快递员信息不能为空");
        }
        return domainDeliveryManService.addDeliveryMan(deliveryMan);
    }

    public void updateDeliveryMan(DeliveryMan deliveryMan) {
        if (deliveryMan == null) {
            throw new IllegalArgumentException("快递员信息不能为空");
        }
        domainDeliveryManService.updateDeliveryMan(deliveryMan);
    }

    public void deleteDeliveryMan(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID不能为空");
        }
        domainDeliveryManService.deleteDeliveryMan(id);
    }

    public DeliveryMan findById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID不能为空");
        }
        return domainDeliveryManService.findById(id);
    }

    public List<DeliveryMan> findByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return domainDeliveryManService.findByUserId(userId);
    }

    public List<DeliveryMan> listAll() {
        return domainDeliveryManService.list(0, 1000);
    }
}
