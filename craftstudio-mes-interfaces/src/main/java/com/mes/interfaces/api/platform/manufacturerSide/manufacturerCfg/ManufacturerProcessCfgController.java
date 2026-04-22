package com.mes.interfaces.api.platform.manufacturerSide.manufacturerCfg;

import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.command.api.req.ConfigProcessMetaRequest;
import com.mes.application.dto.req.manufacturerMeta.UpdateProcessPriceRequest;
import com.piliofpala.craftstudio.shared.infra.http.HttpProxy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@RestController
@RequestMapping("/api/manufacturerSide/processCfg")
public class ManufacturerProcessCfgController {

    @Autowired
    private ProductCoreApiService productApiService;

    @Autowired
    private HttpProxy httpProxy;

    @Value("${external.api.productCoreUrl:}")
    private String productCoreUrl;

    /**
     * 分页查找工艺定义
     * @return 分页查询结果
     */
    @GetMapping("/list")
    public ResponseEntity<byte[]> listProcessMetas(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        
        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/listProcessMetas", productCoreUrl));

        
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
     * 按名字模糊搜索工艺定义
     * @return 分页查询结果
     */
    @GetMapping("/search")
    public ResponseEntity<byte[]> searchProcessMetas(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        
        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/searchProcessMetas", productCoreUrl));

        
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
     * 配置工艺定义
     * @param configRequest 配置请求参数
     * @return 操作结果
     */
    @PostMapping("/config")
    public ResponseEntity<byte[]> configProcessMeta(
            HttpServletRequest httpRequest,
            @RequestBody UpdateProcessPriceRequest configRequest) {
        
        String targetUrl = String.format("%s/api/internal/mes/rmfcfg/configProcessMeta", productCoreUrl);
        
        // 构建请求对象
        ConfigProcessMetaRequest request = new ConfigProcessMetaRequest();
        request.setRmfId(configRequest.getRmfId());
        request.setProcessMetaId(configRequest.getProcessMetaId());
        request.setUnitPrice(configRequest.getUnitPrice());
        
        // 将请求对象转换为 JSON 字节数组
        byte[] requestBody = null;
        try {
            requestBody = com.alibaba.fastjson.JSON.toJSONString(request).getBytes(StandardCharsets.UTF_8);
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
