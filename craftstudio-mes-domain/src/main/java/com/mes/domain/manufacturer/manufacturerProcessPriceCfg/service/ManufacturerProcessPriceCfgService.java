package com.mes.domain.manufacturer.manufacturerProcessPriceCfg.service;

import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity.ManufacturerProcessPriceCfg;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.repository.ManufacturerProcessPriceCfgRepository;
import com.mes.domain.shared.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@Service
public class ManufacturerProcessPriceCfgService {

    private final RestTemplate restTemplate;
    private final ManufacturerProcessPriceCfgRepository repository;

    @Value("${external.api.baseUrl:}")
    private String externalApiBaseUrl;

    public ManufacturerProcessPriceCfgService(RestTemplate restTemplate, ManufacturerProcessPriceCfgRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
    }

    /**
     * 根据制造商 ID 查询工艺价格配置列表（从外部系统获取）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param current 当前页码，从 1 开始，必须大于 0
     * @param size 每页大小，范围 1-100
     * @return 工艺价格配置列表
     * @throws BusinessNotAllowException 当参数不合法或外部系统调用失败时抛出此异常
     */
    public List<ManufacturerProcessPriceCfg> findByManufacturerId(String manufacturerId, int current, int size) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException("制造商 ID 不能为空");
        }

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }

        try {
            String url = String.format("%s/process-prices?manufacturerId=%s&current=%d&size=%d",
                    externalApiBaseUrl, manufacturerId, current, size);
            
            ResponseEntity<ApiResponse<List<ManufacturerProcessPriceCfg>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    (Class<ApiResponse<List<ManufacturerProcessPriceCfg>>>) (Class<?>) ApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new BusinessNotAllowException("获取工艺价格配置失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new BusinessNotAllowException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 更新单个工艺价格配置的价格和状态信息（调用外部系统接口）
     * 
     * @param processPriceId 工艺价格配置 ID，不能为空
     * @param price 新的价格信息，可为 null（为 null 时不更新价格）
     * @param status 新的状态信息，可为 null（为 null 时不更新状态）
     * @throws BusinessNotAllowException 当参数不合法或外部系统调用失败时抛出此异常
     */
    public void updateProcessPriceAndStatus(String processPriceId, UnitPrice price, String status) {
        if (StringUtils.isBlank(processPriceId)) {
            throw new BusinessNotAllowException("工艺价格配置 ID 不能为空");
        }

        if (price == null && status == null) {
            throw new BusinessNotAllowException("至少需要提供价格或状态中的一个");
        }

        if (price != null && price.getPrice() == null) {
            throw new BusinessNotAllowException("价格不能为空");
        }

        try {
            String url = String.format("%s/process-prices/%s/price-status",
                    externalApiBaseUrl, processPriceId);

            UpdatePriceStatusRequest request = new UpdatePriceStatusRequest();
            request.setPrice(price);
            request.setStatus(status);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UpdatePriceStatusRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    (Class<ApiResponse<Void>>) (Class<?>) ApiResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessNotAllowException("更新工艺价格配置失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new BusinessNotAllowException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 根据 ID 查询工艺价格配置
     */
    public ManufacturerProcessPriceCfg findById(String id) {
        return repository.findById(id);
    }

    /**
     * 保存工艺价格配置
     */
    public ManufacturerProcessPriceCfg save(ManufacturerProcessPriceCfg cfg) {
        return repository.add(cfg);
    }

    /**
     * 更新工艺价格配置
     */
    public void update(ManufacturerProcessPriceCfg cfg) {
        repository.update(cfg);
    }

    /**
     * 删除工艺价格配置
     */
    public void delete(ManufacturerProcessPriceCfg cfg) {
        repository.delete(cfg);
    }

    /**
     * 根据制造商 ID 查询工艺价格配置列表（从数据库获取）
     */
    public List<ManufacturerProcessPriceCfg> listByManufacturerId(String manufacturerId, long current, int size) {
        return repository.filterList(current, size, java.util.Map.of("manufacturerId", manufacturerId));
    }

    /**
     * 批量保存工艺价格配置
     */
    public java.util.Collection<ManufacturerProcessPriceCfg> batchSave(List<ManufacturerProcessPriceCfg> items) {
        return repository.batchAdd(items);
    }

    // 内部类用于接收外部 API 响应
    private static class ApiResponse<T> {
        private Integer code;
        private String message;
        private T data;

        public Integer getCode() { return code; }
        public void setCode(Integer code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }

    // 内部类用于发送更新请求
    private static class UpdatePriceStatusRequest {
        private UnitPrice price;
        private String status;

        public UnitPrice getPrice() { return price; }
        public void setPrice(UnitPrice price) { this.price = price; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
