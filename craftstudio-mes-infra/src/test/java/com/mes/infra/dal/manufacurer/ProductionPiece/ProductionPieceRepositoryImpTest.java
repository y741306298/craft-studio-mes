package com.mes.infra.dal.manufacurer.ProductionPiece;

import com.mes.infra.dal.manufacurer.ProductionPiece.po.ProductionPiecePo;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    void channelUpdateDoesNotReplaceProcedureFlow() {
        ProductionPieceRepositoryImp repository = new ProductionPieceRepositoryImp();
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ReflectionTestUtils.setField(repository, "mongoTemplate", mongoTemplate);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(ProductionPiecePo.class)))
                .thenReturn(UpdateResult.acknowledged(2, 2L, null));

        repository.updateChannelByIds(List.of("piece-1", "piece-2"), new OrderChannelInfo());

        verify(mongoTemplate).updateMulti(queryCaptor.capture(), updateCaptor.capture(), eq(ProductionPiecePo.class));
        assertThat(queryCaptor.getValue().getQueryObject().toJson()).contains("piece-1", "piece-2");
        assertThat(updateCaptor.getValue().getUpdateObject().toJson())
                .contains("channel", "updateTime")
                .doesNotContain("procedureFlow");
    }

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
