package com.mes.infra.dal.manufacurer.ProductionPiece;

import com.mes.infra.dal.manufacurer.ProductionPiece.po.ProductionPiecePo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductionPieceRepositoryImpTest {

    @Test
    void pendingPackagingBatchQueryPushesNodeQuantityFilterToMongoDb() {
        ProductionPieceRepositoryImp repository = new ProductionPieceRepositoryImp();
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ReflectionTestUtils.setField(repository, "mongoTemplate", mongoTemplate);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(mongoTemplate.find(any(Query.class), eq(ProductionPiecePo.class)))
                .thenReturn(Collections.emptyList());

        repository.findPendingPackagingPiecesByOrderItemIds(
                "manufacturer-1", List.of("item-1", "item-2"), "acrylic");

        verify(mongoTemplate).find(queryCaptor.capture(), eq(ProductionPiecePo.class));
        String queryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertThat(queryJson)
                .contains("manufacturerId", "orderItemId", "procedureFlow.nodes", "pieceQuantity", "$gt")
                .contains("NODE_PENDING_PACKING", "待打包")
                .contains("materialConfig.materialSnapshot.name", "acrylic");
    }
}
