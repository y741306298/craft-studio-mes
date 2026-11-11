package com.mes.infra.dal.order;

import com.mes.infra.dal.order.po.OrderItemPo;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderItemNameFilterTest {
    @Test
    void filtersActiveFactoryItemsAndActiveOrdersWithLiteralAndConditionsInOneQuery() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        OrderItemRepositoryImp repository = new OrderItemRepositoryImp();
        ReflectionTestUtils.setField(repository, "mongoTemplate", mongo);
        ObjectId id = new ObjectId();
        when(mongo.aggregate(any(Aggregation.class), eq(OrderItemPo.class), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(
                        new Document("_id", id).append("orderItemId", "item")), new Document()));

        List<String> ids = repository.findItemIdsByOrderNames("factory", " 张.* ", " 公司 ");

        assertEquals(List.of(id.toString(), "item"), ids);
        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongo).aggregate(captor.capture(), eq(OrderItemPo.class), eq(Document.class));
        verifyNoMoreInteractions(mongo);
        List<Document> pipeline = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        Document itemMatch = pipeline.get(0).get("$match", Document.class);
        assertEquals("factory", itemMatch.getString("manufacturerId"));
        assertTrue(itemMatch.containsKey("deleteAt"));
        assertNull(itemMatch.get("deleteAt"));
        Document lookup = pipeline.get(2).get("$lookup", Document.class);
        assertEquals("orderInfo", lookup.getString("from"));
        assertEquals("$orderId", lookup.get("let", Document.class).getString("itemOrderId"));
        Document orderMatch = lookup.getList("pipeline", Document.class).get(0).get("$match", Document.class);
        assertEquals(Pattern.quote("张.*"), orderMatch.get("customer.customerName", Document.class).getString("$regex"));
        assertEquals(Pattern.quote("公司"), orderMatch.get("orgInfo.name", Document.class).getString("$regex"));
        assertEquals(List.of("$orderId", "$$itemOrderId"),
                orderMatch.get("$expr", Document.class).get("$eq"));
        assertTrue(orderMatch.containsKey("deleteAt"));
        assertNull(orderMatch.get("deleteAt"));
    }
}
