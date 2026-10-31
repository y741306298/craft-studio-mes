package com.mes.infra.db.mongodb;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * MongoDB index definitions required by application queries.
 */
@Configuration
public class MongoIndexConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void ensureIndexes() {
        mongoTemplate.indexOps("deliveryPackagingTask").ensureIndex(new Index().on("state", Sort.Direction.ASC)
                .on("nextAttemptAt", Sort.Direction.ASC).named("idx_packaging_task_poll"));
        mongoTemplate.indexOps("deliveryPackagingTask").ensureIndex(new Index().on("state", Sort.Direction.ASC)
                .on("leaseUntil", Sort.Direction.ASC).named("idx_packaging_task_lease"));
        mongoTemplate.indexOps("deliveryPackagingTask").ensureIndex(new Index().on("state", Sort.Direction.ASC)
                .on("updatedAt", Sort.Direction.ASC).named("idx_packaging_task_recovery"));
        ensureOrderItemIndexes();
        ensureProductionPieceIndexes();
        ensureTypesettingIndexes();
    }

    private void ensureOrderItemIndexes() {
        mongoTemplate.indexOps("orderItem").ensureIndex(new Index().on("orderItemId", Sort.Direction.ASC)
                .on("deleteAt", Sort.Direction.ASC).named("idx_order_item_business_id_active"));
        mongoTemplate.indexOps("orderItem")
                .ensureIndex(new Index()
                        .on("orderId", Sort.Direction.ASC)
                        .on("orderItemId", Sort.Direction.ASC)
                        .on("manufacturerId", Sort.Direction.ASC)
                        .named("idx_order_item_import_deduplication"));
    }

    private void ensureProductionPieceIndexes() {
        mongoTemplate.indexOps("productionPiece").ensureIndex(new Index().on("productionPieceId", Sort.Direction.ASC)
                .on("deleteAt", Sort.Direction.ASC).named("idx_piece_business_id_active"));
        mongoTemplate.indexOps("productionPiece").ensureIndex(new Index().on("orderItemId", Sort.Direction.ASC)
                .on("deleteAt", Sort.Direction.ASC).named("idx_piece_order_item_active"));
        mongoTemplate.indexOps("productionPiece")
                .ensureIndex(new Index()
                        .on("manufacturerId", Sort.Direction.ASC)
                        .named("idx_production_piece_manufacturer_id"));
        mongoTemplate.indexOps("productionPiece")
                .ensureIndex(new Index()
                        .on("manufacturerId", Sort.Direction.ASC)
                        .on("orderItemId", Sort.Direction.ASC)
                        .on("deleteAt", Sort.Direction.ASC)
                        .on("updateTime", Sort.Direction.DESC)
                        .named("idx_production_piece_manufacturer_order_item_active_update"));
        mongoTemplate.indexOps("productionPiece")
                .ensureIndex(new Index()
                        .on("manufacturerId", Sort.Direction.ASC)
                        .on("orderItemId", Sort.Direction.ASC)
                        .on("deleteAt", Sort.Direction.ASC)
                        .on("procedureFlow.nodes.nodeName", Sort.Direction.ASC)
                        .on("procedureFlow.nodes.pieceQuantity", Sort.Direction.ASC)
                        .named("idx_production_piece_order_item_pending_packaging"));
        mongoTemplate.indexOps("productionPiece")
                .ensureIndex(new Index()
                        .on("manufacturerId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .named("idx_production_piece_manufacturer_status"));
        mongoTemplate.indexOps("productionPiece")
                .ensureIndex(new Index()
                        .on("manufacturerId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .on("procedureFlow.nodes.nodeName", Sort.Direction.ASC)
                        .on("procedureFlow.nodes.pieceQuantity", Sort.Direction.ASC)
                        .named("idx_production_piece_pending_node"));
        mongoTemplate.indexOps("productionPiece")
                .ensureIndex(new Index()
                        .on("manufacturerId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .on("isUrgent", Sort.Direction.DESC)
                        .on("createTime", Sort.Direction.ASC)
                        .named("idx_production_piece_pending_sort"));
        mongoTemplate.indexOps("productionPiece")
                .ensureIndex(new Index()
                        .on("manufacturerId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .on("materialConfig.materialSnapshot.name", Sort.Direction.ASC)
                        .on("isUrgent", Sort.Direction.DESC)
                        .on("createTime", Sort.Direction.ASC)
                        .named("idx_production_piece_pending_material_sort"));
    }

    private void ensureTypesettingIndexes() {
        mongoTemplate.indexOps("typesetting")
                .ensureIndex(new Index()
                        .on("manufacturerMetaId", Sort.Direction.ASC)
                        .named("idx_typesetting_manufacturer_meta_id"));
        mongoTemplate.indexOps("typesetting")
                .ensureIndex(new Index()
                        .on("manufacturerMetaId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .named("idx_typesetting_manufacturer_status"));
        mongoTemplate.indexOps("typesetting")
                .ensureIndex(new Index()
                        .on("manufacturerMetaId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .on("isUrgent", Sort.Direction.DESC)
                        .on("createTime", Sort.Direction.ASC)
                        .partial(PartialIndexFilter.of(Criteria.where("leaveQuantity").gt(0)))
                        .named("idx_typesetting_pending_sort"));
        mongoTemplate.indexOps("typesetting")
                .ensureIndex(new Index()
                        .on("manufacturerMetaId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .on("materialConfig.materialSnapshot.name", Sort.Direction.ASC)
                        .on("isUrgent", Sort.Direction.DESC)
                        .on("createTime", Sort.Direction.ASC)
                        .partial(PartialIndexFilter.of(Criteria.where("leaveQuantity").gt(0)))
                        .named("idx_typesetting_pending_material_sort"));
    }
}
