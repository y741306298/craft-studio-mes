package com.mes.infra.dal.order;

import com.mes.domain.order.productionPieceGenerationTask.entity.ProductionPieceGenerationTask;
import com.mes.domain.order.productionPieceGenerationTask.repository.ProductionPieceGenerationTaskRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.po.ProductionPieceGenerationTaskPo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class ProductionPieceGenerationTaskRepositoryImp
        extends BaseRepositoryImp<ProductionPieceGenerationTask, ProductionPieceGenerationTaskPo>
        implements ProductionPieceGenerationTaskRepository {
    @Override
    public Class<ProductionPieceGenerationTaskPo> poClass() {
        return ProductionPieceGenerationTaskPo.class;
    }

    @Override
    public List<ProductionPieceGenerationTask> findByOrderIds(Collection<String> orderIds) {
        Query query = new Query(Criteria.where("orderId").in(orderIds).and("deleteAt").is(null));
        return mongoTemplate.find(query, poClass()).stream().map(ProductionPieceGenerationTaskPo::toDO).toList();
    }

    @Override
    public void removeOrderItem(String orderId, String orderItemId) {
        Query query = new Query(Criteria.where("orderId").is(orderId)
                .and("orderItemIdList").is(orderItemId).and("deleteAt").is(null));
        mongoTemplate.updateMulti(query, new Update().pull("orderItemIdList", orderItemId), poClass());
    }
}
