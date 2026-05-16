package com.mes.infra.oss;

import com.aliyun.dashvector.DashVectorClient;
import com.aliyun.dashvector.DashVectorClientConfig;
import com.aliyun.dashvector.DashVectorCollection;
import com.aliyun.dashvector.common.DashVectorException;
import com.aliyun.dashvector.models.Doc;
import com.aliyun.dashvector.models.Vector;
import com.aliyun.dashvector.models.requests.CreateCollectionRequest;
import com.aliyun.dashvector.models.requests.DeleteDocRequest;
import com.aliyun.dashvector.models.requests.QueryDocRequest;
import com.aliyun.dashvector.models.requests.UpsertDocRequest;
import com.aliyun.dashvector.models.responses.Response;
import com.aliyun.dashvector.proto.CollectionInfo;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ImageToImageSearchServiceImp implements ImageToImageSearchService {

    @Value("${ali-cloud.dashscope_api_key}")
    private String dashscopeApiKey;

    @Value("${ali-cloud.access-key-id}")
    private String accessKeyId;

    @Value("${ali-cloud.access-key-secret}")
    private String accessKeySecret;

    @Value("${ali-cloud.oss.endpoint}")
    private String ossEndpoint;

    @Value("${ali-cloud.oss.raw-bucket}")
    private String bucketName;

    @Value("${ali-cloud.oss.region}")
    private String region;

    @Value("${ali-cloud.dashvector.endpoint}")
    private String dashvectorEndpoint;

    @Value("${ali-cloud.dashvector.api-key}")
    private String dashvectorApiKey;

    @Value("${ali-cloud.dashvector.collection-name}")
    private String collectionName;

    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final Gson gson = new Gson();

    private DashVectorClient dashvectorClient;
    private DashVectorCollection collection;
    private OSS ossClient;

    @PostConstruct
    public void init() {
        initializeOSS();
        initializeDashVector();
    }

    /**
     * 初始化 OSS 客户端
     */
    private void initializeOSS() {
        try {
            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
            
            ossClient = OSSClientBuilder.create()
                    .endpoint(ossEndpoint)
                    .credentialsProvider(new com.aliyun.oss.common.auth.DefaultCredentialProvider(accessKeyId, accessKeySecret))
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(region)
                    .build();
            
            System.out.println("OSS client initialized for bucket: " + bucketName);
        } catch (Exception e) {
            System.err.println("Failed to initialize OSS client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化 DashVector 客户端和集合
     */
    private void initializeDashVector() {
        try {
            System.out.println("Initializing DashVector with endpoint: " + dashvectorEndpoint);
            System.out.println("Collection name: " + collectionName);
            
            DashVectorClientConfig config = DashVectorClientConfig.builder()
                    .apiKey(dashvectorApiKey)
                    .endpoint(dashvectorEndpoint)
                    .build();

            dashvectorClient = new DashVectorClient(config);
            System.out.println("DashVector client created successfully");

            DashVectorCollection resp = dashvectorClient.get(collectionName);
            if (resp != null) {
                collection = resp;
                System.out.println("DashVector collection loaded: " + collectionName);
            } else {
                System.out.println("Collection not found, creating now...");
                createCollection();
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize DashVector: " + e.getMessage());
            System.err.println("DashVector functionality will be unavailable. Please check your configuration.");
            e.printStackTrace();
        }
    }

    /**
     * 创建 DashVector 集合
     */
    private void createCollection() {
        try {
            if (dashvectorClient == null) {
                System.err.println("DashVector client not initialized, cannot create collection");
                return;
            }
            
            System.out.println("Creating DashVector collection: " + collectionName);
            CreateCollectionRequest request = CreateCollectionRequest.builder()
                    .name(collectionName)
                    .dimension(1024)
                    .metric(CollectionInfo.Metric.cosine)
                    .build();

            Response<Void> resp = dashvectorClient.create(request);
            if (resp.isSuccess()) {
                System.out.println("DashVector collection created successfully: " + collectionName);
                
                // 等待一下让集合生效
                Thread.sleep(2000);
                
                DashVectorCollection getResp = dashvectorClient.get(collectionName);
                if (getResp != null) {
                    collection = getResp;
                    System.out.println("Collection loaded and ready to use");
                } else {
                    System.err.println("Failed to load collection after creation");
                }
            } else {
                System.err.println("Failed to create collection: " + resp.getMessage());
                System.err.println("Error code: " + resp.getCode());
            }
        } catch (Exception e) {
            System.err.println("Error creating collection: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void indexImage(String docId, String imageUrl, String productionPieceId, String manufacturerMetaId) {
        if (docId == null || docId.isEmpty() || imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        try {
            float[] vector = generateImageEmbedding(imageUrl);
            upsertImageVector(docId, imageUrl, vector, productionPieceId, manufacturerMetaId);
        } catch (Exception e) {
            System.err.println("Failed to index image vector, docId=" + docId + ", error=" + e.getMessage());
        }
    }

    /**
     * 从 OSS Bucket 的指定目录扫描图片并建立向量索引到 DashVector
     * @param prefix OSS 对象前缀（如 "pieceImg/"），为空则扫描整个 bucket
     * @return 成功处理的图片数量
     */
    public int scanAndIndexImagesFromOSS(String prefix) {
        System.out.println("=== Starting scanAndIndexImagesFromOSS ===");
        System.out.println("Prefix: " + prefix);
        
        if (ossClient == null) {
            System.err.println("OSS client not initialized");
            return 0;
        }

        if (collection == null) {
            System.out.println("Collection is null, attempting to create...");
            createCollection();
            if (collection == null) {
                System.err.println("Failed to create collection, aborting scan");
                return 0;
            }
        }
        
        System.out.println("Collection is ready: " + collection.getName());

        List<ImageVectorData> imageVectors = new ArrayList<>();
        AtomicInteger totalCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try {
            String scanPrefix = prefix != null && !prefix.isEmpty() ? prefix : "";
            System.out.println("Scanning OSS bucket: " + bucketName + ", prefix: " + (scanPrefix.isEmpty() ? "(all)" : scanPrefix));

            String nextMarker = null;
            int batchNum = 0;
            do {
                batchNum++;
                System.out.println("Fetching batch #" + batchNum + "...");
                
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
                listObjectsRequest.setPrefix(scanPrefix);
                listObjectsRequest.setMarker(nextMarker);
                listObjectsRequest.setMaxKeys(1000);
                
                ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
                List<OSSObjectSummary> objectSummaries = objectListing.getObjectSummaries();

                System.out.println("Found " + objectSummaries.size() + " objects in this batch");

                for (OSSObjectSummary summary : objectSummaries) {
                    String key = summary.getKey();
                    
                    // 只处理图片文件
                    if (isImageFile(key)) {
                        totalCount.incrementAndGet();
                        String imageUrl = buildImageUrl(key);
                        
                        try {
                            System.out.println("Processing image [" + totalCount.get() + "]: " + key);
                            System.out.println("  Image URL: " + imageUrl);
                            
                            // 生成向量
                            float[] vector = generateImageEmbedding(imageUrl);
                            System.out.println("  Vector generated, dimension: " + vector.length);
                            
                            // 生成文档ID（使用文件路径，替换特殊字符）
                            String docId = generateDocId(key);
                            System.out.println("  Doc ID: " + docId);
                            
                            imageVectors.add(new ImageVectorData(docId, imageUrl, vector));
                            
                            // 每处理10张图片批量上传一次（改为更小的批次以便调试）
                            if (imageVectors.size() >= 10) {
                                System.out.println("Batch size reached (" + imageVectors.size() + "), uploading...");
                                boolean batchSuccess = batchUpsertImageVectors(imageVectors, docId);
                                if (batchSuccess) {
                                    successCount.addAndGet(imageVectors.size());
                                    System.out.println("Batch uploaded successfully. Total success: " + successCount.get());
                                } else {
                                    failCount.addAndGet(imageVectors.size());
                                    System.err.println("Batch upload failed. Total failed: " + failCount.get());
                                }
                                imageVectors.clear();
                                
                                // 避免API限流，暂停一下
                                Thread.sleep(1000);
                            }
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            System.err.println("Failed to process image: " + key + ", error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

                nextMarker = objectListing.getNextMarker();
                if (nextMarker != null) {
                    System.out.println("More objects available, fetching next batch...");
                }
            } while (nextMarker != null);

            // 上传剩余的图片
            if (!imageVectors.isEmpty()) {
                System.out.println("Uploading remaining " + imageVectors.size() + " images...");
                boolean batchSuccess = batchUpsertImageVectors(imageVectors, imageVectors.get(0).getProductionPieceId());
                if (batchSuccess) {
                    successCount.addAndGet(imageVectors.size());
                    System.out.println("Remaining images uploaded successfully");
                } else {
                    failCount.addAndGet(imageVectors.size());
                    System.err.println("Failed to upload remaining images");
                }
            }

            System.out.println("=== Scan Completed ===");
            System.out.println("Total processed: " + totalCount.get());
            System.out.println("Success: " + successCount.get());
            System.out.println("Failed: " + failCount.get());

        } catch (Exception e) {
            System.err.println("Error scanning OSS bucket: " + e.getMessage());
            e.printStackTrace();
        }

        return successCount.get();
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg") || 
               lowerKey.endsWith(".png") || lowerKey.endsWith(".gif") || 
               lowerKey.endsWith(".bmp") || lowerKey.endsWith(".webp");
    }

    /**
     * 构建图片 URL
     */
    private String buildImageUrl(String key) {
        String cleanEndpoint = ossEndpoint.replace("https://", "").replace("http://", "");
        return "https://" + bucketName + "." + cleanEndpoint + "/" + key;
    }

    /**
     * 生成文档 ID
     */
    private String generateDocId(String key) {
        // 使用 MD5 哈希生成短 ID，确保符合 DashVector 的 ID 格式要求
        // 只保留字母、数字、下划线和连字符
        String hash = md5(key);
        
        // 返回前 32 位哈希值，确保长度不超过 64
        return hash.substring(0, Math.min(32, hash.length()));
    }

    /**
     * 计算 MD5 哈希值
     */
    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            // 如果 MD5 失败，使用 hashCode 并清理非法字符
            String simpleId = String.valueOf(Math.abs(input.hashCode()));
            return simpleId.replaceAll("[^a-zA-Z0-9_-]", "");
        }
    }

    /**
     * 使用DashScope API将图片转换为向量
     */
    @Override
    public float[] generateImageEmbedding(String imageUrl) throws IOException {
        String url = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding";

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "multimodal-embedding-v1");

        JsonObject inputObj = new JsonObject();
        JsonArray contentsArray = new JsonArray();

        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("image", imageUrl);
        contentsArray.add(contentObj);

        inputObj.add("contents", contentsArray);
        requestBody.add("input", inputObj);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(RequestBody.create(gson.toJson(requestBody), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + dashscopeApiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (okhttp3.Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("DashScope API error: " + response.code() + " - " + response.body().string());
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            JsonObject output = jsonResponse.getAsJsonObject("output");
            JsonArray embeddingsArray = output.getAsJsonArray("embeddings");

            if (embeddingsArray == null || embeddingsArray.size() == 0) {
                throw new IOException("No embeddings returned from DashScope API");
            }

            JsonObject embedding = embeddingsArray.get(0).getAsJsonObject();
            JsonArray vectorArray = embedding.getAsJsonArray("embedding");

            float[] vector = new float[vectorArray.size()];
            for (int i = 0; i < vectorArray.size(); i++) {
                vector[i] = vectorArray.get(i).getAsFloat();
            }

            return vector;
        }
    }



    public float[] generateImageEmbeddingByBase64(String imageBase64) throws IOException {
        String url = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding";

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "multimodal-embedding-v1");

        JsonObject inputObj = new JsonObject();
        JsonArray contentsArray = new JsonArray();

        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("image", imageBase64);
        contentsArray.add(contentObj);

        inputObj.add("contents", contentsArray);
        requestBody.add("input", inputObj);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(RequestBody.create(gson.toJson(requestBody), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + dashscopeApiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (okhttp3.Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("DashScope API error: " + response.code() + " - " + response.body().string());
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            JsonObject output = jsonResponse.getAsJsonObject("output");
            JsonArray embeddingsArray = output.getAsJsonArray("embeddings");

            if (embeddingsArray == null || embeddingsArray.size() == 0) {
                throw new IOException("No embeddings returned from DashScope API");
            }

            JsonObject embedding = embeddingsArray.get(0).getAsJsonObject();
            JsonArray vectorArray = embedding.getAsJsonArray("embedding");

            float[] vector = new float[vectorArray.size()];
            for (int i = 0; i < vectorArray.size(); i++) {
                vector[i] = vectorArray.get(i).getAsFloat();
            }

            return vector;
        }
    }
    /**
     * 上传图片向量到 DashVector
     */
    @Override
    public boolean upsertImageVector(String docId, String imageUrl, float[] vector, String productionPieceId, String manufacturerMetaId) {
        try {
            if (collection == null) {
                System.err.println("DashVector collection not initialized");
                return false;
            }

            List<Float> vectorList = new ArrayList<>();
            for (float v : vector) {
                vectorList.add(v);
            }

            Vector vec = Vector.builder().value(vectorList).build();

            Doc doc = Doc.builder()
                    .id(docId)
                    .vector(vec)
                    .field("imageUrl", imageUrl)
                    .field("uploadedAt", new Date().toString())
                    .field("productionPieceId", productionPieceId)
                    .field("manufacturerMetaId", manufacturerMetaId)
                    .build();

            UpsertDocRequest request = UpsertDocRequest.builder()
                    .doc(doc)
                    .build();

            Response<List<com.aliyun.dashvector.models.DocOpResult>> resp = collection.upsert(request);

            if (resp.isSuccess()) {
                System.out.println("Successfully upserted image vector: " + docId);
                return true;
            } else {
                System.err.println("Failed to upsert image vector: " + resp.getMessage());
                return false;
            }
        } catch (DashVectorException e) {
            System.err.println("Error upserting image vector: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 搜索相似图片（支持过滤条件）
     * @param queryVector 查询向量
     * @param topK 返回结果数量
     * @param filter 过滤表达式，如："manufacturerMetaId = 'xxx' and uploadedAt > '2024-01-01'"
     */
    public List<ImageSearchResult> searchSimilarImages(float[] queryVector, int topK, String filter) {
        try {
            if (collection == null) {
                System.err.println("DashVector collection not initialized");
                return Collections.emptyList();
            }

            List<Float> vectorList = new ArrayList<>();
            for (float v : queryVector) {
                vectorList.add(v);
            }

            Vector vector = Vector.builder().value(vectorList).build();

            // 构建请求
            var requestBuilder = QueryDocRequest.builder()
                    .vector(vector)
                    .topk(topK)
                    .includeVector(false);

            // 添加过滤条件
            if (filter != null && !filter.isEmpty()) {
                requestBuilder.filter(filter);
            }

            QueryDocRequest request = requestBuilder.build();

            Response<List<Doc>> resp = collection.query(request);

            if (resp.isSuccess() && resp.getOutput() != null) {
                List<ImageSearchResult> results = new ArrayList<>();

                for (Doc doc : resp.getOutput()) {
                    ImageSearchResult result = new ImageSearchResult();
                    result.setDocId(doc.getId());
                    result.setImageUrl((String) doc.getFields().get("imageUrl"));
                    result.setScore(doc.getScore());
                    result.setProductionPieceId((String) doc.getFields().get("productionPieceId"));
                    result.setManufacturerMetaId((String) doc.getFields().get("manufacturerMetaId"));
                    Object uploadedAt = doc.getFields().get("uploadedAt");
                    result.setUploadedAt(uploadedAt == null ? null : uploadedAt.toString());

                    results.add(result);
                }

                System.out.println("Found " + results.size() + " similar images with filter: " + filter);
                return results;
            } else {
                System.err.println("Search failed: " + resp.getMessage());
                return Collections.emptyList();
            }
        } catch (DashVectorException e) {
            System.err.println("Error searching similar images: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 批量上传图片向量到 DashVector
     */
    public boolean batchUpsertImageVectors(List<ImageVectorData> imageVectors,String productionPieceId) {
        try {
            if (collection == null) {
                System.err.println("DashVector collection not initialized");
                return false;
            }

            List<Doc> docs = new ArrayList<>();
            for (ImageVectorData data : imageVectors) {
                List<Float> vectorList = new ArrayList<>();
                for (float v : data.getVector()) {
                    vectorList.add(v);
                }

                Vector vec = Vector.builder().value(vectorList).build();

                Doc doc = Doc.builder()
                        .id(data.getDocId())
                        .vector(vec)
                        .field("imageUrl", data.getImageUrl())
                        .field("uploadedAt", new Date().toString())
                        .field("productionPieceId", productionPieceId)
                        .build();

                docs.add(doc);
            }

            UpsertDocRequest request = UpsertDocRequest.builder()
                    .docs(docs)
                    .build();

            Response<List<com.aliyun.dashvector.models.DocOpResult>> resp = collection.upsert(request);

            if (resp.isSuccess()) {
                System.out.println("Successfully batch upserted " + docs.size() + " image vectors");
                return true;
            } else {
                System.err.println("Failed to batch upsert: " + resp.getMessage());
                return false;
            }
        } catch (DashVectorException e) {
            System.err.println("Error batch upserting image vectors: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }




    /**
     * 搜索相似图片
     */
    public List<ImageSearchResult> searchSimilarImages(float[] queryVector, int topK) {
        try {
            if (collection == null) {
                System.err.println("DashVector collection not initialized");
                return Collections.emptyList();
            }

            List<Float> vectorList = new ArrayList<>();
            for (float v : queryVector) {
                vectorList.add(v);
            }

            Vector vector = Vector.builder().value(vectorList).build();

            QueryDocRequest request = QueryDocRequest.builder()
                    .vector(vector)
                    .topk(topK)
                    .includeVector(false)
                    .build();

            Response<List<Doc>> resp = collection.query(request);

            if (resp.isSuccess() && resp.getOutput() != null) {
                List<ImageSearchResult> results = new ArrayList<>();

                for (Doc doc : resp.getOutput()) {
                    ImageSearchResult result = new ImageSearchResult();
                    result.setDocId(doc.getId());
                    result.setImageUrl((String) doc.getFields().get("imageUrl"));
                    result.setScore(doc.getScore());
                    result.setProductionPieceId((String) doc.getFields().get("productionPieceId"));
                    result.setManufacturerMetaId((String) doc.getFields().get("manufacturerMetaId"));
                    Object uploadedAt = doc.getFields().get("uploadedAt");
                    result.setUploadedAt(uploadedAt == null ? null : uploadedAt.toString());

                    results.add(result);
                }

                System.out.println("Found " + results.size() + " similar images");
                return results;
            } else {
                System.err.println("Search failed: " + resp.getMessage());
                return Collections.emptyList();
            }
        } catch (DashVectorException e) {
            System.err.println("Error searching similar images: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 删除图片向量
     */
    public boolean deleteImageVector(String docId) {
        try {
            if (collection == null) {
                System.err.println("DashVector collection not initialized");
                return false;
            }

            DeleteDocRequest request = DeleteDocRequest.builder()
                    .ids(Collections.singletonList(docId))
                    .build();

            Response<List<com.aliyun.dashvector.models.DocOpResult>> resp = collection.delete(request);

            if (resp.isSuccess()) {
                System.out.println("Successfully deleted image vector: " + docId);
                return true;
            } else {
                System.err.println("Failed to delete image vector: " + resp.getMessage());
                return false;
            }
        } catch (DashVectorException e) {
            System.err.println("Error deleting image vector: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 图片向量数据
     */
    public static class ImageVectorData {
        private String docId;
        private String imageUrl;
        private String productionPieceId;
        private float[] vector;

        public ImageVectorData(String docId, String imageUrl, float[] vector) {
            this.docId = docId;
            this.imageUrl = imageUrl;
            this.vector = vector;
        }

        public ImageVectorData(String docId, String imageUrl, float[] vector, String productionPieceId) {
            this.docId = docId;
            this.imageUrl = imageUrl;
            this.vector = vector;
            this.productionPieceId = productionPieceId;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public float[] getVector() {
            return vector;
        }

        public void setVector(float[] vector) {
            this.vector = vector;
        }

        public String getProductionPieceId() {
            return productionPieceId;
        }

        public void setProductionPieceId(String productionPieceId) {
            this.productionPieceId = productionPieceId;
        }
    }

    /**
     * 图片搜索结果
     */
    public static class ImageSearchResult {
        private String docId;
        private String imageUrl;
        private float score;
        private String productionPieceId;
        private String manufacturerMetaId;
        private String uploadedAt;

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }

        public String getProductionPieceId() {
            return productionPieceId;
        }

        public void setProductionPieceId(String productionPieceId) {
            this.productionPieceId = productionPieceId;
        }

        public String getManufacturerMetaId() {
            return manufacturerMetaId;
        }

        public void setManufacturerMetaId(String manufacturerMetaId) {
            this.manufacturerMetaId = manufacturerMetaId;
        }

        public String getUploadedAt() {
            return uploadedAt;
        }

        public void setUploadedAt(String uploadedAt) {
            this.uploadedAt = uploadedAt;
        }

        @Override
        public String toString() {
            return "ImageSearchResult{" +
                    "docId='" + docId + '\'' +
                    ", imageUrl='" + imageUrl + '\'' +
                    ", score=" + score +
                    ", productionPieceId='" + productionPieceId + '\'' +
                    '}';
        }
    }
}
