package com.mes.application.command.api;


import com.alibaba.fastjson.JSON;
import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.api.req.ImageMaskRequest;
import com.mes.application.command.api.req.ImpositionRequest;
import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.api.req.SvgToPltRequest;
import com.mes.application.command.api.resp.FormeGenerationResponse;
import com.mes.application.command.api.resp.ImageMaskResponse;
import com.mes.application.command.api.resp.ImpositionResponse;
import com.mes.application.command.api.resp.NestingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AlgorithmCoreApiService {

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 同步调用算法服务
     * 请求发起方需一直等处理结束才释放连接
     *
     * @param baseUrl 算法服务基础URL
     * @param apiPath API路径
     * @param requestBody 请求体参数
     * @param responseType 响应类型
     * @return 算法处理结果
     */
    public <T> T callAlgorithmSync(String baseUrl, String apiPath, Object requestBody, Class<T> responseType) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new RuntimeException("算法服务地址未配置");
        }
        if (apiPath == null || apiPath.isEmpty()) {
            throw new RuntimeException("API路径不能为空");
        }

        try {
            String url = baseUrl + apiPath;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<T> response = restTemplate.postForEntity(url, requestEntity, responseType);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("算法服务调用失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用算法服务失败：" + e.getMessage());
        }
    }

    /**
     * 异步调用算法服务
     * 算法服务会立刻返回202，表示接收任务，最终处理结果在callback中传递
     *
     * @param baseUrl 算法服务基础URL
     * @param apiPath API路径
     * @param requestBody 请求体参数
     * @param callbackUrl 回调地址
     * @return 任务接受响应
     */
    public <T> T callAlgorithmAsync(String baseUrl, String apiPath, Object requestBody, String callbackUrl, Class<T> responseType) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new RuntimeException("算法服务地址未配置");
        }
        if (apiPath == null || apiPath.isEmpty()) {
            throw new RuntimeException("API路径不能为空");
        }
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            throw new RuntimeException("回调地址不能为空");
        }

        try {
            String url = baseUrl + apiPath;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Fc-Invocation-Type", "Async");

            HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<T> response = restTemplate.postForEntity(url, requestEntity, responseType);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("算法服务异步调用失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用算法服务失败：" + e.getMessage());
        }
    }


    /**
     * 图片遮罩抠图 - 异步模式
     * 适用于耗时较长的场景，建议优先使用此模式
     * 算法服务会立即返回202，处理结果通过回调接口返回
     *
     * @param request 抠图请求参数，必须配置callbackConfig
     * @return 任务接受响应
     */
    public ImageMaskResponse generateMaskFilesAsync(ImageMaskRequest request) {
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (request.getRawImage() == null || request.getRawImage().getUrl() == null || request.getRawImage().getUrl().isEmpty()) {
            throw new RuntimeException("原始图片URL不能为空");
        }
        if (request.getMaskSvgUrl() == null || request.getMaskSvgUrl().isEmpty()) {
            throw new RuntimeException("蒙版SVG URL不能为空");
        }
        if (request.getCallbackConfig() == null || request.getCallbackConfig().getCallbackUrl() == null || request.getCallbackConfig().getCallbackUrl().isEmpty()) {
            throw new RuntimeException("异步模式下回调地址不能为空");
        }
        String jsonString = JSON.toJSONString(request);
        System.out.println(jsonString);
        return callAlgorithmAsync("http://craftstg-masker-qvsnfcgkck.cn-hangzhou.fcapp.run", "/generate_mask_files", request,
                request.getCallbackConfig().getCallbackUrl(), ImageMaskResponse.class);
    }


    /**
     * 排版算法 - 异步模式（推荐）
     * 将多个零件SVG进行智能排版，生成优化的排版方案
     * 适用于耗时较长的场景，建议优先使用此模式
     * 算法服务会立即返回202，处理结果通过回调接口返回
     *
     * @param request 排版请求参数，必须配置callbackConfig
     * @return 任务接受响应
     */
    public NestingResponse generateNestedFilesAsync(NestingRequest request) {
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (request.getNestManifest() == null) {
            throw new RuntimeException("排版清单不能为空");
        }
        if (request.getNestManifest().getContainers() == null || request.getNestManifest().getContainers().isEmpty()) {
            throw new RuntimeException("排版容器列表不能为空");
        }
        if (request.getNestManifest().getElements() == null || request.getNestManifest().getElements().isEmpty()) {
            throw new RuntimeException("排版元素列表不能为空");
        }
        if (request.getCallbackConfig() == null || request.getCallbackConfig().getCallbackUrl() == null || request.getCallbackConfig().getCallbackUrl().isEmpty()) {
            throw new RuntimeException("异步模式下回调地址不能为空");
        }

        return callAlgorithmAsync("http://craftstsvg-nest-lmadlddfst.cn-hangzhou.fcapp.run", "/generate_nested_files", request,
                request.getCallbackConfig().getCallbackUrl(), NestingResponse.class);
    }

    /**
     * 网格排版算法 - 异步模式（推荐）
     * 将多个零件SVG进行智能网格排版，生成优化的排版方案和网格线信息
     * 适用于耗时较长的场景，建议优先使用此模式
     * 算法服务会立即返回202，处理结果通过回调接口返回
     *
     * @param request 排版请求参数，必须配置callbackConfig
     * @return 任务接受响应
     */
    public NestingResponse generateGridNestedFilesAsync(NestingRequest request) {
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (request.getNestManifest() == null) {
            throw new RuntimeException("排版清单不能为空");
        }
        if (request.getNestManifest().getContainers() == null || request.getNestManifest().getContainers().isEmpty()) {
            throw new RuntimeException("排版容器列表不能为空");
        }
        if (request.getNestManifest().getElements() == null || request.getNestManifest().getElements().isEmpty()) {
            throw new RuntimeException("排版元素列表不能为空");
        }
        if (request.getCallbackConfig() == null || request.getCallbackConfig().getCallbackUrl() == null || request.getCallbackConfig().getCallbackUrl().isEmpty()) {
            throw new RuntimeException("异步模式下回调地址不能为空");
        }

        return callAlgorithmAsync("http://craftstrid-nest-diczqdtwfj.cn-hangzhou.fcapp.run", "/generate_nested_files", request,
                request.getCallbackConfig().getCallbackUrl(), NestingResponse.class);
    }


    /**
     * 印版生成 - 异步模式（推荐）
     * 根据排版SVG生成印版文件，包括JSON、PLT和印版SVG格式
     * 支持添加标记信息（二维码、辅助线等）和定位点
     * 适用于耗时较长的场景，建议优先使用此模式
     * 算法服务会立即返回202，处理结果通过回调接口返回
     *
     * @param request 印版生成请求参数，必须配置callbackConfig
     * @return 任务接受响应
     */
    public FormeGenerationResponse generateFormeAsync(FormeGenerationRequest request) {
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (request.getForme() == null) {
            throw new RuntimeException("印版信息不能为空");
        }
        if (request.getForme().getSvgUrl() == null || request.getForme().getSvgUrl().isEmpty()) {
            throw new RuntimeException("印版SVG URL不能为空");
        }
        if (request.getOutputs() == null) {
            throw new RuntimeException("输出配置不能为空");
        }
        if (request.getCallbackConfig() == null || request.getCallbackConfig().getCallbackUrl() == null || request.getCallbackConfig().getCallbackUrl().isEmpty()) {
            throw new RuntimeException("异步模式下回调地址不能为空");
        }

        return callAlgorithmAsync("http://craftstenerator-ygfncedtli.cn-hangzhou.fcapp.run", "/generate_forme", request,
                request.getCallbackConfig().getCallbackUrl(), FormeGenerationResponse.class);
    }

    /**
     * 印版生成 - 同步模式
     * 根据排版SVG生成印版文件，包括JSON、PLT和印版SVG格式
     * 支持添加标记信息（二维码、辅助线等）和定位点
     * 请求发起方需一直等处理结束才释放连接
     *
     * @param request 印版生成请求参数
     * @return 印版生成结果，包含JSON、PLT和formeSvg的URL
     */
    public FormeGenerationResponse generateForme(FormeGenerationRequest request) {
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (request.getForme() == null) {
            throw new RuntimeException("印版信息不能为空");
        }
        if (request.getForme().getSvgUrl() == null || request.getForme().getSvgUrl().isEmpty()) {
            throw new RuntimeException("印版SVG URL不能为空");
        }
        if (request.getOutputs() == null) {
            throw new RuntimeException("输出配置不能为空");
        }

        return callAlgorithmSync("http://craftstenerator-ygfncedtli.cn-hangzhou.fcapp.run", "/generate_forme", request,
                FormeGenerationResponse.class);
    }


    /**
     * SVG转PLT格式转换 - 同步模式（仅支持同步）
     * 将SVG矢量图形转换为PLT格式的切割路径文件
     * 直接返回PLT文件的字符串数据
     *
     * @param request 转换请求参数
     * @return PLT文件数据字符串，包含HPGL/2切割指令
     */
    public String convertSvgToPlt(SvgToPltRequest request) {
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (request.getSvgUrl() == null || request.getSvgUrl().isEmpty()) {
            throw new RuntimeException("SVG文件URL不能为空");
        }

        return callAlgorithmSync("http://craftstonverter-zjjuwhyrfr.cn-hangzhou.fcapp.run", "/svg_to_plt", request, String.class);
    }

    
}
