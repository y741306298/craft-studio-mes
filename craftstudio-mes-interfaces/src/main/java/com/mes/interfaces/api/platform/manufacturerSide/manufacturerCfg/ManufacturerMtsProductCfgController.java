package com.mes.interfaces.api.platform.manufacturerSide.manufacturerCfg;

import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.command.api.req.ConfigMTSProductSpecRequest;
import com.piliofpala.craftstudio.shared.infra.http.HttpProxy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@RestController
@RequestMapping("/api/manufacturerSide/mtsProductCfg")
public class ManufacturerMtsProductCfgController {

    @Autowired
    private ProductCoreApiService productApiService;

    @Autowired
    private HttpProxy httpProxy;

    @Value("${external.api.productCoreUrl:}")
    private String productCoreUrl;

    /**
     * 根据父分类 ID 查询成品商品分类列表
     */
    @GetMapping("/categories")
    public ResponseEntity<byte[]> findCategoriesByParentId(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/product/mts/listCategories", productCoreUrl));

        
        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, body, urlBuilder.toString(), paramMap);

        // 调试：打印响应内容
        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    /**
     * 分页查询成品商品列表
     *
     * @return 分页查询结果
     */
    @GetMapping("/products")
    public ResponseEntity<byte[]> findMTSProducts(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/product/mts/listMTSProducts", productCoreUrl));

        
        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, body, urlBuilder.toString(), paramMap);

        // 调试：打印响应内容
        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    /**
     * 分页查询成品商品规格列表
     *
     * @return 分页查询结果
     */
    @GetMapping("/product-specs")
    public ResponseEntity<byte[]> findMTSProductSpecs(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/product/mts/listMTSProductSpecs", productCoreUrl));

        
        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, body, urlBuilder.toString(), paramMap);

        // 调试：打印响应内容
        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    /**
     * 配置成品商品规格
     *
     * @param configRequest 配置请求参数
     * @return 操作结果
     */
    @PostMapping("/product-specs/config")
    public ResponseEntity<byte[]> configMTSProductSpec(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ConfigMTSProductSpecRequest configRequest) {

        String targetUrl = String.format("%s/api/internal/mes/rmfcfg/configMTSProductSpec", productCoreUrl);
        
        // 将请求对象转换为 JSON 字节数组
        byte[] requestBody = null;
        try {
            requestBody = com.alibaba.fastjson.JSON.toJSONString(configRequest).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Failed to serialize request: " + e.getMessage());
        }
        
        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(httpRequest, requestBody, targetUrl, paramMap);

        // 调试：打印响应内容
        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

}
