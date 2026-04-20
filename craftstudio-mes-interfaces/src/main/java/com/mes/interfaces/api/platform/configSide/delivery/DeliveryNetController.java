package com.mes.interfaces.api.platform.configSide.delivery;

import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.command.api.req.ConfigLogisticsRequest;
import com.mes.application.command.api.resp.LogisticsConfigOptionsResponse;
import com.mes.application.command.api.resp.LogisticsConfigResponse;
import com.mes.application.command.delivery.AppDeliveryNetService;
import com.mes.domain.base.repository.ApiResponse;
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
@RequestMapping("/api/configSide/delivery/deliveryNet")
public class DeliveryNetController {

    @Autowired
    private AppDeliveryNetService appDeliveryNetService;

    @Autowired
    private ProductCoreApiService productApiService;

    @Autowired
    private HttpProxy httpProxy;

    @Value("${external.api.productCoreUrl:}")
    private String productCoreUrl;

    /**
     * 查询物流配置列表（按父地区）
     * @return 物流配置映射，key 为地区编码，value 为该地区的物流商列表
     */
    @GetMapping("/list")
    public ResponseEntity<byte[]> listDeliveryNets(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        
        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/findLogisticsConfigsByParentRegionCode", productCoreUrl));

        
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
     * 获取物流配置选项（物流供应商和承运商列表）
     * @return 物流配置选项，包含 providers（物流供应商）和 carriers（承运商）
     */
    @GetMapping("/options")
    public ResponseEntity<byte[]> getLogisticsConfigOptions(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        
        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/logistics/rmfConfigOptions", productCoreUrl));

        
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
     * 配置物流
     * @param httpRequest HTTP请求
     * @param configRequest 配置请求参数
     * @return 操作结果
     */
    @PostMapping("/config")
    public ResponseEntity<byte[]> configLogistics(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ConfigLogisticsRequest configRequest) {
        
        String targetUrl = String.format("%s/api/internal/mes/rmfcfg/configLogistics", productCoreUrl);
        
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

    /**
     * 删除物流配置
     * @param httpRequest HTTP请求
     * @param body 请求体，包含配置id
     * @return 操作结果
     */
    @PostMapping("/delete")
    public ResponseEntity<byte[]> deleteLogisticsConfig(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) byte[] body) {
        String sss = new String(body, StandardCharsets.UTF_8);
        String targetUrl = String.format("%s/api/internal/mes/rmfcfg/deleteLogisticsConfig", productCoreUrl);
        
        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(httpRequest, body, targetUrl, paramMap);

        // 调试：打印响应内容
        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }
    
}
