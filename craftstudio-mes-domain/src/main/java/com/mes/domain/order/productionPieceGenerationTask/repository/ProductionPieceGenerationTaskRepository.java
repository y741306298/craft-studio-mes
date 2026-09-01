package com.mes.domain.order.productionPieceGenerationTask.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.order.productionPieceGenerationTask.entity.ProductionPieceGenerationTask;

import java.util.Collection;
import java.util.List;

public interface ProductionPieceGenerationTaskRepository extends BaseRepository<ProductionPieceGenerationTask> {
    List<ProductionPieceGenerationTask> findByOrderIds(Collection<String> orderIds);

    void removeOrderItem(String orderId, String orderItemId);
}
