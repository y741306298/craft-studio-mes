package com.mes.infra.dal.manufacurer.typesetting;

import com.mes.infra.dal.manufacurer.typesetting.po.TypesettingPo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TypesettingRepositoryImpTest {

    @Test
    void findsExistingLayoutsByProductionPieceSourceId() {
        TypesettingRepositoryImp repository = new TypesettingRepositoryImp();
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ReflectionTestUtils.setField(repository, "mongoTemplate", mongoTemplate);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(mongoTemplate.find(any(Query.class), eq(TypesettingPo.class))).thenReturn(List.of());

        repository.findByProductionPieceSourceIds(List.of("piece-1", "piece-2"));

        verify(mongoTemplate).find(queryCaptor.capture(), eq(TypesettingPo.class));
        assertThat(queryCaptor.getValue().getQueryObject().toJson())
                .contains("typesettingCells", "$elemMatch", "PRODUCTION_PIECE", "piece-1", "piece-2");
        assertThat(queryCaptor.getValue().getFieldsObject().toJson())
                .contains("typesettingId", "status", "typesettingCells");
    }
}
