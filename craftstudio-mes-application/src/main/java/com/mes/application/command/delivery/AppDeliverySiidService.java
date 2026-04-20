package com.mes.application.command.delivery;

import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;
import com.mes.domain.delivery.deliveryPkg.service.DeliverySiidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppDeliverySiidService {

    @Autowired
    private DeliverySiidService domainDeliverySiidService;

    public DeliverySiid addDeliverySiid(DeliverySiid deliverySiid) {
        if (deliverySiid == null) {
            throw new IllegalArgumentException("SIID信息不能为空");
        }
        return domainDeliverySiidService.addDeliverySiid(deliverySiid);
    }

    public void updateDeliverySiid(DeliverySiid deliverySiid) {
        if (deliverySiid == null) {
            throw new IllegalArgumentException("SIID信息不能为空");
        }
        domainDeliverySiidService.updateDeliverySiid(deliverySiid);
    }

    public void deleteDeliverySiid(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID不能为空");
        }
        domainDeliverySiidService.deleteDeliverySiid(id);
    }

    public DeliverySiid findById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID不能为空");
        }
        return domainDeliverySiidService.findById(id);
    }

    public DeliverySiid findBySiid(String siid) {
        if (siid == null || siid.isEmpty()) {
            throw new IllegalArgumentException("SIID不能为空");
        }
        return domainDeliverySiidService.findBySiid(siid);
    }

    public List<DeliverySiid> findByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return domainDeliverySiidService.findByUserId(userId);
    }

    public List<DeliverySiid> listAll() {
        return domainDeliverySiidService.list(0, 1000);
    }
}
