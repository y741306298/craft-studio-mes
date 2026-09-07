package com.mes.infra.oss;

import com.aliyun.dashvector.DashVectorClient;
import com.aliyun.dashvector.DashVectorCollection;
import com.aliyun.dashvector.models.Doc;
import com.aliyun.dashvector.models.DocOpResult;
import com.aliyun.dashvector.models.requests.DeleteDocRequest;
import com.aliyun.dashvector.models.requests.FetchDocRequest;
import com.aliyun.dashvector.models.responses.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImageToImageSearchServiceImpTest {

    /**
     * 针对真实 DashVector Collection 的破坏性测试。运行前请指定一个允许被删除且确实存在的 Doc 主键：
     * -Ddashvector.test.api-key=... -Ddashvector.test.endpoint=...
     * -Ddashvector.test.collection=... -Ddashvector.test.doc-id=...
     */
    @Test
    void deletesSpecifiedDocumentFromDashVector() throws Exception {
        String apiKey ="sk-uHm4AgEjQqmT9MElXq71MIZa4Gv1r490809BF4C6411F19F2D5EAA1937E744";
        String endpoint = "vrs-cn-xcp4s0agv0001d.dashvector.cn-hangzhou.aliyuncs.com";
        String collectionName = "image-search-collection";
        String docId = "PP17867547494150440B3";
//
//        assumeTrue(apiKey != null && endpoint != null && collectionName != null && docId != null,
//                "未配置真实 DashVector 连接信息及待删除 Doc 主键，跳过破坏性测试");
//
//        DashVectorClient client = new DashVectorClient(apiKey, endpoint);
//        try {
//            DashVectorCollection collection = client.get(collectionName);
//            assertTrue(collection.isSuccess(), "应成功连接指定 Collection");
//
//            Response<Map<String, Doc>> beforeDelete = collection.fetch(
//                    FetchDocRequest.builder().id(docId).build());
//            assertTrue(beforeDelete.isSuccess(), "删除前读取 Doc 应成功");
//            assertNotNull(beforeDelete.getOutput(), "删除前读取结果不能为空");
//            assertTrue(beforeDelete.getOutput().containsKey(docId), "指定 Doc 必须真实存在，避免无效删除测试通过");
//
//            Response<List<DocOpResult>> deleteResponse = collection.delete(
//                    DeleteDocRequest.builder().id(docId).build());
//            assertTrue(deleteResponse.isSuccess(), deleteResponse.getMessage());
//            assertNotNull(deleteResponse.getOutput(), "删除结果不能为空");
//            DocOpResult result = deleteResponse.getOutput().stream()
//                    .filter(item -> docId.equals(item.getId()))
//                    .findFirst()
//                    .orElseThrow(() -> new AssertionError("删除结果中没有指定 Doc 主键"));
//            assertEquals(0, result.getCode(), result.getMessage());
//
//            assertTrue(waitUntilMissing(collection, docId), "DashVector 删除成功后不应再读取到指定 Doc");
//        } finally {
//            client.close();
//        }
    }

    private static boolean waitUntilMissing(DashVectorCollection collection, String docId) throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            Response<Map<String, Doc>> response = collection.fetch(
                    FetchDocRequest.builder().id(docId).build());
            assertTrue(response.isSuccess(), "删除后读取 Doc 应成功");
            if (response.getOutput() == null || !response.getOutput().containsKey(docId)) {
                return true;
            }
            Thread.sleep(500L);
        }
        return false;
    }

    private static String setting(String propertyName, String environmentName) {
        String value = System.getProperty(propertyName);
        return value == null || value.isBlank() ? System.getenv(environmentName) : value;
    }
}
