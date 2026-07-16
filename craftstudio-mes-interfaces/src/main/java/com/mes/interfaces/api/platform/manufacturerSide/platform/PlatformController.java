package com.mes.interfaces.api.platform.manufacturerSide.platform;

import com.piliofpala.craftstudio.shared.infra.http.HttpProxy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@RestController
@RequestMapping("/api/manufacturerSide/platform")
public class PlatformController {

    @Autowired
    private HttpProxy httpProxy;

    @Value("${external.api.productCoreUrl:}")
    private String productCoreUrl;

    /**
     * 获取工厂所属平台列表
     *
     * Path: /api/internal/mes/platform/pageListPlatformsByMfId
     * Method: POST
     *
     * @return 分页查询结果
     */
    @PostMapping("/pageListPlatformsByMfId")
    public ResponseEntity<byte[]> pageListPlatformsByMfId(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        String targetUrl = String.format("%s/api/internal/mes/platform/pageListPlatformsByMfId", productCoreUrl);

        HashMap<String, Object> paramMap = new HashMap<>();
        ResponseEntity<byte[]> responseEntity = httpProxy.forwardRequest(request, body, targetUrl, paramMap);

        if (responseEntity.getBody() != null) {
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
            System.out.println("Response body: " + responseBody);
        }

        return responseEntity;
    }
}
