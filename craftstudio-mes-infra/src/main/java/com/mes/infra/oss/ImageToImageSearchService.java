package com.mes.infra.oss;

import java.io.IOException;

public interface ImageToImageSearchService {

    float[] generateImageEmbedding(String imageUrl) throws IOException;

    boolean upsertImageVector(String docId, String imageUrl, float[] vector);

    void indexImage(String docId, String imageUrl);
}
