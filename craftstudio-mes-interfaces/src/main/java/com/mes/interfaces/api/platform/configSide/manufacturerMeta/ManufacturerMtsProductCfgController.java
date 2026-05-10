package com.mes.interfaces.api.platform.configSide.manufacturerMeta;

import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.command.api.req.ConfigMTSProductSpecRequest;
import com.mes.application.command.api.req.SearchMTSProductByNameRequest;
import com.mes.application.command.api.resp.MtsProductCategoryResponse;
import com.mes.application.command.api.resp.MtsProductListResponse;
import com.mes.application.command.api.resp.MtsProductSpecResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.piliofpala.craftstudio.shared.infra.http.HttpProxy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configSide/mtsProductCfg")
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

        byte[] requestBody = null;
        try {
            requestBody = com.alibaba.fastjson.JSON.toJSONString(configRequest).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Failed to serialize request: " + e.getMessage());
        }

        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(httpRequest, requestBody, targetUrl, paramMap);

        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    @PostMapping("/searchByName")
    public ResponseEntity<byte[]> searchMTSProductsByName(
            HttpServletRequest request,
            @Valid @RequestBody SearchMTSProductByNameRequest searchRequest) {

        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/product/mts/searchByName", productCoreUrl));

        byte[] requestBody = null;
        try {
            requestBody = com.alibaba.fastjson.JSON.toJSONString(searchRequest).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Failed to serialize request: " + e.getMessage());
        }

        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, requestBody, urlBuilder.toString(), paramMap);

        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    /**
     * 成品商品分-页列表
     *
     * Path: /api/internal/mes/product/mts/listMTSProduct
     * Method: GET
     *
     * 返回参数含义：
     * config：工厂在这个成品商品规格上的配置
     * mtsProductSpec：成品商品规格数据
     */
    @GetMapping("/listMTSProduct")
    public ResponseEntity<byte[]> listMTSProduct(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/product/mts/listMTSProduct", productCoreUrl));

        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, body, urlBuilder.toString(), paramMap);

        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    /**
     * 成品商品-规格分页列表
     *
     * Path: /api/internal/mes/rmfcfg/product/mts/listMTSProductSpecs
     * Method: GET
     *
     * 返回参数含义：
     * config：工厂在这个成品商品规格上的配置
     * mtsProductSpec：成品商品规格数据
     */
    @GetMapping("/listMTSProductSpecs")
    public ResponseEntity<byte[]> listMTSProductSpecs(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/product/mts/listMTSProductSpecsByProductId", productCoreUrl));

        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, body, urlBuilder.toString(), paramMap);

        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    /**
     * 【已配置】成品商品分-页列表
     *
     * Path: /api/internal/mes/product/mts/listConfiguredMTSProduct
     * Method: GET
     *
     * 返回参数含义：
     * config：工厂在这个成品商品规格上的配置
     * mtsProductSpec：成品商品规格数据
     */
    @GetMapping("/listConfiguredMTSProduct")
    public ResponseEntity<byte[]> listConfiguredMTSProduct(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/product/mts/listConfiguredMTSProduct", productCoreUrl));

        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, body, urlBuilder.toString(), paramMap);

        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    /**
     * 【已配置】成品商品-规格分页列表
     *
     * Path: /api/internal/mes/product/mts/listConfiguredMTSProductSpecsByProductId
     * Method: GET
     *
     * 返回参数含义：
     * config：工厂在这个成品商品规格上的配置
     * mtsProductSpec：成品商品规格数据
     */
    @GetMapping("/listConfiguredMTSProductSpecsByProductId")
    public ResponseEntity<byte[]> listConfiguredMTSProductSpecsByProductId(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/product/mts/listConfiguredMTSProductSpecsByProductId", productCoreUrl));

        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, body, urlBuilder.toString(), paramMap);

        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    /**
     * 【已配置】成品商品-按名字搜索
     *
     * Path: /api/internal/mes/product/mts/searchConfiguredMTSProduct
     * Method: POST
     */
    @PostMapping("/searchConfiguredMTSProduct")
    public ResponseEntity<byte[]> searchConfiguredMTSProduct(
            HttpServletRequest request,
            @Valid @RequestBody SearchMTSProductByNameRequest searchRequest) {

        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/product/mts/searchConfiguredMTSProduct", productCoreUrl));

        byte[] requestBody = null;
        try {
            requestBody = com.alibaba.fastjson.JSON.toJSONString(searchRequest).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Failed to serialize request: " + e.getMessage());
        }

        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, requestBody, urlBuilder.toString(), paramMap);

        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

}
