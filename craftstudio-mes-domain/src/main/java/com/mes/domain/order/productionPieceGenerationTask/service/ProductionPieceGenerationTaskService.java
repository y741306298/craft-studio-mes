package com.mes.domain.order.productionPieceGenerationTask.service;

import com.mes.domain.order.productionPieceGenerationTask.entity.ProductionPieceGenerationTask;
import com.mes.domain.order.productionPieceGenerationTask.repository.ProductionPieceGenerationTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class ProductionPieceGenerationTaskService {
    @Autowired
    private ProductionPieceGenerationTaskRepository repository;

    public ProductionPieceGenerationTask create(String orderId, Collection<String> orderItemIds) {
        ProductionPieceGenerationTask task = new ProductionPieceGenerationTask();
        task.setOrderId(orderId);
        task.setOrderItemIdList(orderItemIds == null ? Collections.emptyList() : orderItemIds.stream().distinct().toList());
        return repository.add(task);
    }

    public void markGenerated(String orderId, String orderItemId) {
        if (orderId != null && orderItemId != null) {
            repository.removeOrderItem(orderId, orderItemId);
        }
    }

    public List<ProductionPieceGenerationTask> findByOrderIds(Collection<String> orderIds) {
        return orderIds == null || orderIds.isEmpty() ? Collections.emptyList() : repository.findByOrderIds(orderIds);
    }
}
