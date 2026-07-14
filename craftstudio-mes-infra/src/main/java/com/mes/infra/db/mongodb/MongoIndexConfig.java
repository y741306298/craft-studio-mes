package com.mes.infra.db.mongodb;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * MongoDB index definitions required by application queries.
 */
@Configuration
public class MongoIndexConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void ensureIndexes() {
        ensureProductionPieceIndexes();
        ensureTypesettingIndexes();
    }

    private void ensureProductionPieceIndexes() {
        mongoTemplate.indexOps("productionPiece")
                .ensureIndex(new Index()
                        .on("manufacturerId", Sort.Direction.ASC)
                        .named("idx_production_piece_manufacturer_id"));
    }

    private void ensureTypesettingIndexes() {
        mongoTemplate.indexOps("typesetting")
                .ensureIndex(new Index()
                        .on("manufacturerMetaId", Sort.Direction.ASC)
                        .named("idx_typesetting_manufacturer_meta_id"));
    }
}
