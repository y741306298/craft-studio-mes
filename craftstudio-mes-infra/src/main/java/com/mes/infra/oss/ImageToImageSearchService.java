package com.mes.infra.oss;

import java.io.IOException;
import java.util.List;

public interface ImageToImageSearchService {

    float[] generateImageEmbedding(String imageUrl) throws IOException;

    boolean upsertImageVector(String docId, String imageUrl, float[] vector, String productionPieceId, String manufacturerMetaId);

    void indexImage(String docId, String imageUrl, String productionPieceId, String manufacturerMetaId);

    int deleteImageVectors(List<String> docIds);
}
