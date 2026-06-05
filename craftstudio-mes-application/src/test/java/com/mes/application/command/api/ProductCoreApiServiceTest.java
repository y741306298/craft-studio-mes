package com.mes.application.command.api;

import com.mes.application.command.api.resp.MaterialDevelopedSizeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProductCoreApiServiceTest {

    @Test
    void findDevelopedSizeMapCallsProductCoreOncePerMaterialId() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ProductCoreApiService service = new ProductCoreApiService(restTemplate);
        ReflectionTestUtils.setField(service, "productCoreUrl", "http://product-core");

        server.expect(requestTo("http://product-core/api/internal/mes/product/mto/mat/wm/findDevelopedSizeMap?materialId=mat-1"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"code":200,"data":{"mat-1":{"width":100.0,"height":200.0,"depth":5.0}}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://product-core/api/internal/mes/product/mto/mat/wm/findDevelopedSizeMap?materialId=mat-2"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"code":200,"data":{"mat-2":{"width":300.0,"height":400.0,"depth":6.0}}}
                        """, MediaType.APPLICATION_JSON));

        Map<String, MaterialDevelopedSizeResponse> result = service.findDevelopedSizeMap(
                List.of(" mat-1 ", "mat-2", "mat-1"));

        assertEquals(2, result.size());
        assertEquals(1000.0, result.get("mat-1").getWidth());
        assertEquals(50.0, result.get("mat-1").getDepth());
        assertEquals(4000.0, result.get("mat-2").getHeight());
        server.verify();
    }
}
