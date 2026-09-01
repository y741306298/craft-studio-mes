package com.mes.infra.dal.order.po;

import com.mes.domain.order.productionPieceGenerationTask.entity.ProductionPieceGenerationTask;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "productionPieceGenerationTask")
public class ProductionPieceGenerationTaskPo extends BasePO<ProductionPieceGenerationTask> {
    private String orderId;
    private List<String> orderItemIdList;

    @Override
    public ProductionPieceGenerationTask toDO() {
        ProductionPieceGenerationTask task = new ProductionPieceGenerationTask();
        copyBaseFieldsToDO(task);
        task.setOrderId(orderId);
        task.setOrderItemIdList(orderItemIdList);
        return task;
    }

    @Override
    protected BasePO<ProductionPieceGenerationTask> fromDO(ProductionPieceGenerationTask task) {
        orderId = task.getOrderId();
        orderItemIdList = task.getOrderItemIdList();
        return this;
    }
}
