package com.mes.application.command.delivery;

import com.mes.domain.delivery.deliveryNet.entity.DeliveryNet;
import com.mes.domain.delivery.deliveryNet.repository.DeliveryNetRepository;
import com.mes.domain.delivery.deliveryNet.service.DeliveryNetService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppDeliveryNetService {

    @Autowired
    private DeliveryNetService domainDeliveryNetService;

    @Autowired
    private DeliveryNetRepository deliveryNetRepository;

    public PagedResult<DeliveryNet> findDeliveryNets(String deliveryNetName, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }

        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        List<DeliveryNet> items;
        long total;

        if (StringUtils.isBlank(deliveryNetName)) {
            items = deliveryNetRepository.list(query.getCurrent(), query.getSize());
            total = deliveryNetRepository.total();
        } else {
            items = domainDeliveryNetService.findDeliveryNetsByName(deliveryNetName, (int) query.getCurrent(), query.getSize());
            total = domainDeliveryNetService.getTotalCount(deliveryNetName);
        }

        return new PagedResult<DeliveryNet>(items, total, query.getSize(), query.getCurrent());
    }

    public DeliveryNet addDeliveryNet(DeliveryNet command) {
        if (command == null) {
            throw new IllegalArgumentException("配送网络不能为空");
        }
        return domainDeliveryNetService.addDeliveryNet(command);
    }

    public void updateDeliveryNet(DeliveryNet command) {
        if (command == null) {
            throw new IllegalArgumentException("配送网络不能为空");
        }
        if (StringUtils.isBlank(command.getId())) {
            throw new IllegalArgumentException("配送网络 ID 不能为空");
        }
        domainDeliveryNetService.updateDeliveryNet(command);
    }

    public void deleteDeliveryNet(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainDeliveryNetService.deleteDeliveryNet(id);
    }

    public DeliveryNet findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        return domainDeliveryNetService.findById(id);
    }

    public void activateDeliveryNet(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainDeliveryNetService.activateDeliveryNet(id);
    }

    public void deactivateDeliveryNet(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainDeliveryNetService.deactivateDeliveryNet(id);
    }

    public void suspendDeliveryNet(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainDeliveryNetService.suspendDeliveryNet(id);
    }
}
