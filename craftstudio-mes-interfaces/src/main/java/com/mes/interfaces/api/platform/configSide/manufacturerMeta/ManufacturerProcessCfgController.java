package com.mes.interfaces.api.platform.configSide.manufacturerMeta;

import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.command.api.resp.ProcessMetaResponse;
import com.mes.application.command.api.req.ConfigProcessMetaRequest;
import com.mes.application.dto.req.manufacturerMeta.UpdateProcessPriceRequest;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.piliofpala.craftstudio.shared.infra.http.HttpProxy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@RestController
@RequestMapping("/api/configSide/processCfg")
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
     * @return 操作结果
     */
    @PostMapping("/config")
    public ResponseEntity<byte[]> configProcessMeta(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) byte[] body) {
        
        String targetUrl = String.format("%s/api/internal/mes/rmfcfg/configProcessMeta", productCoreUrl);

        
        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(httpRequest, body, targetUrl, paramMap);

        // 调试：打印响应内容
        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    /**
     * 配置平台工艺价格
     *
     * Path: /api/internal/mes/platform/cfg/price/process
     * Method: POST
     *
     * @return 操作结果
     */
    @PostMapping("/price/process")
    public ResponseEntity<byte[]> configPlatformProcessPrice(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        String targetUrl = String.format("%s/api/internal/mes/platform/cfg/price/process", productCoreUrl);

        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, body, targetUrl, paramMap);

        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

    /**
     * 修改上下架状态-工艺定义
     *
     * Path: /api/internal/mes/rmfcfg/changeProcessMetaConfigState
     * Method: POST
     *
     * @return 操作结果
     */
    @PostMapping("/changeProcessMetaConfigState")
    public ResponseEntity<byte[]> changeProcessMetaConfigState(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/changeProcessMetaConfigState", productCoreUrl));

        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, body, urlBuilder.toString(), paramMap);

        // 调试：打印响应内容
        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }

}
