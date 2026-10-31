package com.mes.application.command.delivery.packaging;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOneModel;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PackagingTaskWorkerTest {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test void replayAfterSuccessfulQuantityWriteSkipsTransferAndStillRefreshesCompletion() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        MongoTemplate mongo = mock(MongoTemplate.class);
        MongoCollection<Document> pieces = mock(MongoCollection.class), items = mock(MongoCollection.class);
        when(store.mongo()).thenReturn(mongo);
        when(mongo.getCollection("productionPiece")).thenReturn(pieces);
        when(mongo.getCollection("orderItem")).thenReturn(items);
        Document before = new Document("_id", "mongo-piece").append("productionPieceId", "piece-1").append("orderItemId", "item-1")
                .append("quantity", 5).append("procedureFlow", new Document("nodes", List.of(
                        new Document("nodeName", "待打包").append("pieceQuantity", 5),
                        new Document("nodeName", "已打包").append("pieceQuantity", 0))));
        Document after = Document.parse(before.toJson());
        after.get("procedureFlow", Document.class).getList("nodes", Document.class).get(0).put("pieceQuantity", 3);
        after.get("procedureFlow", Document.class).getList("nodes", Document.class).get(1).put("pieceQuantity", 2);
        after.put("packagingAppliedJobIds", List.of("job-1"));
        when(items.find(any(Document.class))).thenReturn(iterable(List.of(new Document("_id", "mongo-item")
                .append("orderItemId", "item-1").append("quantity", 5).append("status", "IN_PRODUCTION"))));
        when(pieces.find(any(Document.class))).thenReturn(iterable(List.of(before)), iterable(List.of(after)),
                iterable(List.of(after)), iterable(List.of(after)));
        BulkWriteResult written = mock(BulkWriteResult.class); when(written.getMatchedCount()).thenReturn(1);
        when(pieces.bulkWrite(anyList(), any(com.mongodb.client.model.BulkWriteOptions.class))).thenReturn(written);
        Document job = new Document("_id", "job-1").append("manufacturerId", "factory").append("orderId", "order-1")
                .append("items", List.of(new Document("pieceId", "piece-1").append("orderItemId", "item-1").append("quantity", 2)))
                .append("result", new Document("carrierId", "carrier"));
        PackagingTaskWorker worker = new PackagingTaskWorker(store);
        worker.apply(job);
        worker.apply(job);
        ArgumentCaptor<List> writes = ArgumentCaptor.forClass(List.class);
        verify(pieces, times(1)).bulkWrite(writes.capture(), any(com.mongodb.client.model.BulkWriteOptions.class));
        UpdateOneModel<Document> transfer = (UpdateOneModel<Document>) writes.getValue().getFirst();
        Document update = (Document) transfer.getUpdate();
        assertEquals("job-1", update.get("$addToSet", Document.class).get("packagingAppliedJobIds"));
        assertEquals(3, update.get("$set", Document.class).get("procedureFlow.nodes.0.pieceQuantity"));
        assertEquals(new Document("$ne", "job-1"), ((Document) transfer.getFilter()).get("packagingAppliedJobIds"));
        assertFalse(update.get("$set", Document.class).containsKey("procedureFlow"));
        verify(items, never()).bulkWrite(anyList(), any(com.mongodb.client.model.BulkWriteOptions.class));
        // Aggregate reads happen on both attempts even though the second quantity transfer is skipped.
        verify(pieces, times(4)).find(any(Document.class));
    }

    @SuppressWarnings("unchecked")
    private static FindIterable<Document> iterable(List<Document> docs) {
        FindIterable<Document> iterable = mock(FindIterable.class);
        when(iterable.projection(any(Document.class))).thenReturn(iterable);
        when(iterable.iterator()).thenAnswer(call -> {
            Iterator<Document> source = docs.iterator();
            MongoCursor<Document> cursor = mock(MongoCursor.class);
            when(cursor.hasNext()).thenAnswer(i -> source.hasNext());
            when(cursor.next()).thenAnswer(i -> source.next());
            return cursor;
        });
        return iterable;
    }
}
