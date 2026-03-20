package com.mes.domain.manufacturer.manufacturerMtsProductCfg.service;

import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity.ManufacturerMtsProductCfg;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.repository.ManufacturerMtsProductCfgRepository;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.vo.ManufacturerMtsProductSpec;
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

import java.util.Arrays;
import java.util.List;

@Service
public class ManufacturerMtsProductCfgService {

    private final RestTemplate restTemplate;
    private final ManufacturerMtsProductCfgRepository repository;

    @Value("${external.api.baseUrl:}")
    private String externalApiBaseUrl;

    public ManufacturerMtsProductCfgService(RestTemplate restTemplate, ManufacturerMtsProductCfgRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
    }

    /**
     * 根据制造商 ID 和产品名称分页查询标品商品配置（从外部系统获取）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productName 产品名称，可为空
     * @param current 当前页码，从 1 开始，必须大于 0
     * @param size 每页大小，范围 1-100
     * @return 标品商品配置列表
     * @throws BusinessNotAllowException 当参数不合法或外部系统调用失败时抛出此异常
     */
    public List<ManufacturerMtsProductCfg> findByManufacturerIdAndProductName(String manufacturerId, String productName, int current, int size) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException("制造商 ID 不能为空");
        }
        if (current <= 0) {
            throw new BusinessNotAllowException("当前页码必须大于 0");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(String.format("%s/mts-products?manufacturerId=%s&current=%d&size=%d",
                    externalApiBaseUrl, manufacturerId, current, size));
            
            if (StringUtils.isNotBlank(productName)) {
                urlBuilder.append("&productName=").append(productName);
            }
            
            ResponseEntity<ApiResponse<List<ManufacturerMtsProductCfg>>> response = restTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    null,
                    (Class<ApiResponse<List<ManufacturerMtsProductCfg>>>) (Class<?>) ApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new BusinessNotAllowException("获取标品商品配置失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new BusinessNotAllowException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 更新单个标品商品的价格和状态信息（调用外部系统接口）
     * 
     * @param productCfgId 标品商品配置 ID，不能为空
     * @param price 新的价格信息，可为 null（为 null 时不更新价格）
     * @param status 新的状态信息，可为 null（为 null 时不更新状态）
     * @throws BusinessNotAllowException 当参数不合法或外部系统调用失败时抛出此异常
     */
    public void updateProductPriceAndStatus(String productCfgId, UnitPrice price, String status) {
        if (StringUtils.isBlank(productCfgId)) {
            throw new BusinessNotAllowException("标品商品配置 ID 不能为空");
        }

        if (price == null && status == null) {
            throw new BusinessNotAllowException("至少需要提供价格或状态中的一个");
        }

        if (price != null && price.getPrice() == null) {
            throw new BusinessNotAllowException("价格不能为空");
        }

        try {
            String url = String.format("%s/mts-products/%s/price-status",
                    externalApiBaseUrl, productCfgId);

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
                throw new BusinessNotAllowException("更新标品商品信息失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new BusinessNotAllowException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 根据 ID 查询标品商品配置
     */
    public ManufacturerMtsProductCfg findById(String id) {
        return repository.findById(id);
    }

    /**
     * 保存标品商品配置
     */
    public ManufacturerMtsProductCfg save(ManufacturerMtsProductCfg cfg) {
        return repository.add(cfg);
    }

    /**
     * 更新标品商品配置
     */
    public void update(ManufacturerMtsProductCfg cfg) {
        repository.update(cfg);
    }

    /**
     * 删除标品商品配置
     */
    public void delete(ManufacturerMtsProductCfg cfg) {
        repository.delete(cfg);
    }

    /**
     * 根据制造商 ID 查询标品商品配置列表（从数据库获取）
     */
    public List<ManufacturerMtsProductCfg> listByManufacturerId(String manufacturerId, long current, int size) {
        return repository.filterList(current, size, java.util.Map.of("manufacturerId", manufacturerId));
    }

    /**
     * 批量保存标品商品配置
     */
    public java.util.Collection<ManufacturerMtsProductCfg> batchSave(List<ManufacturerMtsProductCfg> items) {
        return repository.batchAdd(items);
    }

    /**
     * 根据 manufacturerId、productId 和 specId 更新标品规格的价格
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productId 产品 ID，不能为空
     * @param specId 规格 ID，不能为空
     * @param price 新的价格信息，不能为空
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void updateSpecPrice(String manufacturerId, String productId, String specId, UnitPrice price) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException("制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(productId)) {
            throw new BusinessNotAllowException("产品 ID 不能为空");
        }
        if (StringUtils.isBlank(specId)) {
            throw new BusinessNotAllowException("规格 ID 不能为空");
        }
        if (price == null || price.getPrice() == null) {
            throw new BusinessNotAllowException("价格不能为空");
        }

        List<ManufacturerMtsProductCfg> cfgList = repository.filterList(1, 1, java.util.Map.of(
            "manufacturerId", manufacturerId,
            "productId", productId
        ));
        ManufacturerMtsProductCfg cfg = cfgList.isEmpty() ? null : cfgList.get(0);
        
        if (cfg == null) {
            throw new BusinessNotAllowException("未找到对应的标品商品配置");
        }

        List<ManufacturerMtsProductSpec> specs = cfg.getMtsProductSpecs();
        if (specs == null || specs.isEmpty()) {
            throw new BusinessNotAllowException("该商品没有配置规格");
        }

        boolean found = false;
        for (ManufacturerMtsProductSpec spec : specs) {
            if (specId.equals(spec.getId())) {
                spec.setPrice(price);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new BusinessNotAllowException("未找到对应的规格");
        }

        repository.update(cfg);
    }

    /**
     * 逻辑删除标品规格（将规格的 status 改为 INVALID）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productId 产品 ID，不能为空
     * @param specId 规格 ID，不能为空
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void deleteSpec(String manufacturerId, String productId, String specId) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException("制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(productId)) {
            throw new BusinessNotAllowException("产品 ID 不能为空");
        }
        if (StringUtils.isBlank(specId)) {
            throw new BusinessNotAllowException("规格 ID 不能为空");
        }

        List<ManufacturerMtsProductCfg> cfgList = repository.filterList(1, 1, java.util.Map.of(
            "manufacturerId", manufacturerId,
            "productId", productId
        ));
        ManufacturerMtsProductCfg cfg = cfgList.isEmpty() ? null : cfgList.get(0);
        
        if (cfg == null) {
            throw new BusinessNotAllowException("未找到对应的标品商品配置");
        }

        List<ManufacturerMtsProductSpec> specs = cfg.getMtsProductSpecs();
        if (specs == null || specs.isEmpty()) {
            throw new BusinessNotAllowException("该商品没有配置规格");
        }

        boolean found = false;
        for (ManufacturerMtsProductSpec spec : specs) {
            if (specId.equals(spec.getId())) {
                spec.setStatus(CfgStatus.INVALID);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new BusinessNotAllowException("未找到对应的规格");
        }

        repository.update(cfg);
    }

    /**
     * 逻辑删除整个标品商品配置（将 status 改为 INVALID）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productId 产品 ID，不能为空
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void deleteProductCfg(String manufacturerId, String productId) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException("制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(productId)) {
            throw new BusinessNotAllowException("产品 ID 不能为空");
        }

        List<ManufacturerMtsProductCfg> cfgList = repository.filterList(1, 1, java.util.Map.of(
            "manufacturerId", manufacturerId,
            "productId", productId
        ));
        ManufacturerMtsProductCfg cfg = cfgList.isEmpty() ? null : cfgList.get(0);
        
        if (cfg == null) {
            throw new BusinessNotAllowException("未找到对应的标品商品配置");
        }

        cfg.setStatus(CfgStatus.INVALID);
        repository.update(cfg);
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
