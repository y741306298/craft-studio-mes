package com.mes.application.command.api;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.api.req.ConfigLogisticsRequest;
import com.mes.application.command.api.req.ConfigMTSProductSpecRequest;
import com.mes.application.command.api.req.ConfigProcessMetaRequest;
import com.mes.application.command.api.req.UpdatePriceStatusRequest;
import com.mes.application.command.api.resp.*;
import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity.ManufacturerMtsProductCfg;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity.ManufacturerProcessPriceCfg;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.logging.Logger;

@Service
public class ProductCoreApiService {

    @Value("${external.api.productCoreUrl:}")
    private String productCoreUrl;

    private final RestTemplate restTemplate;

    public ProductCoreApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    Logger logger = Logger.getLogger(ProductCoreApiService.class.getName());

    

    /**
     * 根据父分类 ID 查询成品商品分类列表
     * @param parentId 父分类 ID，null 表示查询首级分类
     * @return 分类列表
     */
    public List<MtsProductCategoryResponse> findCategoriesByParentId(String parentId) {
        try {
            StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/product/mts/listCategories", productCoreUrl));

            if (parentId != null && !parentId.isEmpty()) {
                urlBuilder.append("?parentId=").append(parentId);
            }

            ParameterizedTypeReference<ApiResponse<List<MtsProductCategoryResponse>>> typeRef =
                new ParameterizedTypeReference<ApiResponse<List<MtsProductCategoryResponse>>>() {};

            ResponseEntity<ApiResponse<List<MtsProductCategoryResponse>>> response = restTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    null,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("获取成品商品分类失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询成品商品列表
     * @param rmfId 工厂 ID，不能为空
     * @param categoryId 产品分类 ID，可为空
     * @param current 当前页码，从 1 开始
     * @param size 每页大小
     * @return 分页结果
     */
    public PagedResult<MtsProductListResponse> findMTSProducts(String rmfId, String categoryId, int current, int size) {
        if (rmfId == null || rmfId.isEmpty()) {
            throw new RuntimeException("工厂 ID 不能为空");
        }
        if (current <= 0) {
            throw new RuntimeException("当前页码必须大于 0");
        }
        if (size <= 0 || size > 100) {
            throw new RuntimeException("每页大小必须在 1-100 之间");
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/product/mts/listMTSProducts?rmfId=%s&current=%d&size=%d",
                    productCoreUrl, rmfId, current, size));
            
            if (categoryId != null && !categoryId.isEmpty()) {
                urlBuilder.append("&categoryId=").append(categoryId);
            }
            
            ParameterizedTypeReference<ApiResponse<PagedResult<MtsProductListResponse>>> typeRef = 
                new ParameterizedTypeReference<ApiResponse<PagedResult<MtsProductListResponse>>>() {};
            
            ResponseEntity<ApiResponse<PagedResult<MtsProductListResponse>>> response = restTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    null,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("获取成品商品列表失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询成品商品规格列表
     * @param rmfId 工厂 ID，不能为空
     * @param productId 成品商品 ID，不能为空
     * @param current 当前页码，从 1 开始
     * @param size 每页大小
     * @return 分页结果
     */
    public PagedResult<MtsProductSpecResponse> findMTSProductSpecs(String rmfId, String productId, int current, int size) {
        if (rmfId == null || rmfId.isEmpty()) {
            throw new RuntimeException("工厂 ID 不能为空");
        }
        if (productId == null || productId.isEmpty()) {
            throw new RuntimeException("成品商品 ID 不能为空");
        }
        if (current <= 0) {
            throw new RuntimeException("当前页码必须大于 0");
        }
        if (size <= 0 || size > 100) {
            throw new RuntimeException("每页大小必须在 1-100 之间");
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/product/mts/listMTSProductSpecs?rmfId=%s&productId=%s&current=%d&size=%d",
                    productCoreUrl, rmfId, productId, current, size));
            
            ParameterizedTypeReference<ApiResponse<PagedResult<MtsProductSpecResponse>>> typeRef = 
                new ParameterizedTypeReference<ApiResponse<PagedResult<MtsProductSpecResponse>>>() {};
            
            ResponseEntity<ApiResponse<PagedResult<MtsProductSpecResponse>>> response = restTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    null,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("获取成品商品规格列表失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 配置成品商品规格
     * @param rmfId 工厂 ID，不能为空
     * @param mtsProductSpecId 成品商品规格 ID，不能为空
     * @param stock 库存数量
     * @param unitPrice 单价
     * @param price 价格
     */
    public void configMTSProductSpec(String rmfId, String mtsProductSpecId, Integer stock, 
                                     UnitPrice unitPrice,
                                     UnitPrice price) {
        if (rmfId == null || rmfId.isEmpty()) {
            throw new RuntimeException("工厂 ID 不能为空");
        }
        if (mtsProductSpecId == null || mtsProductSpecId.isEmpty()) {
            throw new RuntimeException("成品商品规格 ID 不能为空");
        }

        try {
            String url = String.format("%s/api/internal/mes/rmfcfg/configMTSProductSpec", productCoreUrl);

            ConfigMTSProductSpecRequest request = new ConfigMTSProductSpecRequest();
            request.setRmfId(rmfId);
            request.setMtsProductSpecId(mtsProductSpecId);
            request.setStock(stock);
            request.setUnitPrice(unitPrice);
            request.setPrice(price);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ConfigMTSProductSpecRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    (Class<ApiResponse<Void>>) (Class<?>) ApiResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("配置成品商品规格失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 分页查找工艺定义
     * @param rmfId 工厂 ID，不能为空
     * @param current 当前页码，从 1 开始
     * @param size 每页大小
     * @return 分页结果
     */
    public PagedResult<ProcessMetaResponse> listProcessMetas(String rmfId, int current, int size) {
        if (rmfId == null || rmfId.isEmpty()) {
            throw new RuntimeException("工厂 ID 不能为空");
        }
        if (current <= 0) {
            throw new RuntimeException("当前页码必须大于 0");
        }
        if (size <= 0 || size > 100) {
            throw new RuntimeException("每页大小必须在 1-100 之间");
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/listProcessMetas?rmfId=%s&current=%d&size=%d",
                    productCoreUrl, rmfId, current, size));
            
            ParameterizedTypeReference<ApiResponse<PagedResult<ProcessMetaResponse>>> typeRef = 
                new ParameterizedTypeReference<ApiResponse<PagedResult<ProcessMetaResponse>>>() {};
            
            ResponseEntity<ApiResponse<PagedResult<ProcessMetaResponse>>> response = restTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    null,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("获取工艺定义列表失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 按名字模糊搜索工艺定义
     * @param rmfId 工厂 ID，不能为空
     * @param name 名字字段搜索内容，不能为空
     * @param current 当前页码，从 1 开始
     * @param size 每页大小
     * @return 分页结果
     */
    public PagedResult<ProcessMetaResponse> searchProcessMetas(String rmfId, String name, int current, int size) {
        if (rmfId == null || rmfId.isEmpty()) {
            throw new RuntimeException("工厂 ID 不能为空");
        }
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("搜索关键词不能为空");
        }
        if (current <= 0) {
            throw new RuntimeException("当前页码必须大于 0");
        }
        if (size <= 0 || size > 100) {
            throw new RuntimeException("每页大小必须在 1-100 之间");
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/searchProcessMetas?rmfId=%s&name=%s&current=%d&size=%d",
                    productCoreUrl, rmfId, name, current, size));
            
            ParameterizedTypeReference<ApiResponse<PagedResult<ProcessMetaResponse>>> typeRef = 
                new ParameterizedTypeReference<ApiResponse<PagedResult<ProcessMetaResponse>>>() {};
            
            ResponseEntity<ApiResponse<PagedResult<ProcessMetaResponse>>> response = restTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    null,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("搜索工艺定义失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 配置工艺定义
     * @param rmfId 工厂 ID，不能为空
     * @param processMetaId 工艺定义 ID，不能为空
     * @param unitPrice 单价
     */
    public void configProcessMeta(String rmfId, String processMetaId, UnitPrice unitPrice) {
        if (rmfId == null || rmfId.isEmpty()) {
            throw new RuntimeException("工厂 ID 不能为空");
        }
        if (processMetaId == null || processMetaId.isEmpty()) {
            throw new RuntimeException("工艺定义 ID 不能为空");
        }

        try {
            String url = String.format("%s/api/internal/mes/rmfcfg/configProcessMeta", productCoreUrl);

            ConfigProcessMetaRequest request = new ConfigProcessMetaRequest();
            request.setRmfId(rmfId);
            request.setProcessMetaId(processMetaId);
            request.setUnitPrice(unitPrice);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ConfigProcessMetaRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    (Class<ApiResponse<Void>>) (Class<?>) ApiResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("配置工艺定义失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 按父地区获取物流配置列表
     * @param rmfId 工厂 ID，不能为空
     * @param parentRegionCode 父地区码，null 表示获取第一级地区（国家）配置
     * @return 物流配置映射，key 为地区编码，value 为该地区的物流商列表
     */
    public Map<String, List<LogisticsConfigResponse.LogisticsItem>> findLogisticsConfigsByParentRegionCode(String rmfId, String parentRegionCode) {
        if (rmfId == null || rmfId.isEmpty()) {
            throw new RuntimeException("工厂 ID 不能为空");
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(String.format("%s/api/internal/mes/rmfcfg/findLogisticsConfigsByParentRegionCode?rmfId=%s",
                    productCoreUrl, rmfId));
            
            if (parentRegionCode != null && !parentRegionCode.isEmpty()) {
                urlBuilder.append("&parentRegionCode=").append(parentRegionCode);
            }
            
            ParameterizedTypeReference<ApiResponse<Map<String, List<LogisticsConfigResponse.LogisticsItem>>>> typeRef = 
                new ParameterizedTypeReference<ApiResponse<Map<String, List<LogisticsConfigResponse.LogisticsItem>>>>() {};
            
            ResponseEntity<ApiResponse<Map<String, List<LogisticsConfigResponse.LogisticsItem>>>> response = restTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    null,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("获取物流配置列表失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 获取物流配置选项（物流供应商和承运商列表）
     * @param rmfId 工厂 ID，不能为空
     * @return 物流配置选项，包含 providers（物流供应商）和 carriers（承运商）
     */
    public LogisticsConfigOptionsResponse getLogisticsConfigOptions(String rmfId) {
        if (rmfId == null || rmfId.isEmpty()) {
            throw new RuntimeException("工厂 ID 不能为空");
        }

        try {
            String url = String.format("%s/api/internal/mes/logistics/rmfConfigOptions?rmfId=%s", productCoreUrl, rmfId);
            
            ParameterizedTypeReference<ApiResponse<LogisticsConfigOptionsResponse>> typeRef = 
                new ParameterizedTypeReference<ApiResponse<LogisticsConfigOptionsResponse>>() {};
            
            ResponseEntity<ApiResponse<LogisticsConfigOptionsResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("获取物流配置选项失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 配置物流
     * @param rmfId 工厂 ID，不能为空
     * @param carrierId 物流方式（承运商）ID，不能为空
     * @param providerId 物流供应商 ID，不能为空
     * @param regionCode 地区码，不能为空
     * @param firstWeightPrice 首重价格
     * @param extraWeightPrice 续重价格
     */
    public void configLogistics(String rmfId, String carrierId, String providerId, 
                                String regionCode, Double firstWeightPrice, Double extraWeightPrice) {
        if (rmfId == null || rmfId.isEmpty()) {
            throw new RuntimeException("工厂 ID 不能为空");
        }
        if (carrierId == null || carrierId.isEmpty()) {
            throw new RuntimeException("物流方式 ID 不能为空");
        }
        if (providerId == null || providerId.isEmpty()) {
            throw new RuntimeException("物流供应商 ID 不能为空");
        }
        if (regionCode == null || regionCode.isEmpty()) {
            throw new RuntimeException("地区码不能为空");
        }

        try {
            String url = String.format("%s/api/internal/mes/rmfcfg/configLogistics", productCoreUrl);

            ConfigLogisticsRequest request = new ConfigLogisticsRequest();
            request.setRmfId(rmfId);
            request.setCarrierId(carrierId);
            request.setProviderId(providerId);
            request.setRegionCode(regionCode);
            
            ConfigLogisticsRequest.PriceInfo price = new ConfigLogisticsRequest.PriceInfo();
            price.setFirstWeightPrice(firstWeightPrice);
            price.setExtraWeightPrice(extraWeightPrice);
            request.setPrice(price);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ConfigLogisticsRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    (Class<ApiResponse<String>>) (Class<?>) ApiResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("配置物流失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 根据制造商 ID 和产品名称分页查询标品商品配置（从外部系统获取）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param productName 产品名称，可为空
     * @param current 当前页码，从 1 开始，必须大于 0
     * @param size 每页大小，范围 1-100
     * @return 标品商品配置列表
     */
    public List<ManufacturerMtsProductCfg> findMtsProductCfgsByManufacturerId(String manufacturerId, String productName, int current, int size) {
        if (current <= 0) {
            throw new RuntimeException("当前页码必须大于 0");
        }
        if (size <= 0 || size > 100) {
            throw new RuntimeException("每页大小必须在 1-100 之间");
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(String.format("%s/mts-products?manufacturerId=%s&current=%d&size=%d",
                    productCoreUrl, manufacturerId, current, size));
            
            if (productName != null && !productName.isEmpty()) {
                urlBuilder.append("&productName=").append(productName);
            }
            
            ParameterizedTypeReference<ApiResponse<List<ManufacturerMtsProductCfg>>> typeRef = 
                new ParameterizedTypeReference<ApiResponse<List<ManufacturerMtsProductCfg>>>() {};
            
            ResponseEntity<ApiResponse<List<ManufacturerMtsProductCfg>>> response = restTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    null,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("获取标品商品配置失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 根据制造商 ID 查询工艺价格配置列表（从外部系统获取）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param current 当前页码，从 1 开始，必须大于 0
     * @param size 每页大小，范围 1-100
     * @return 工艺价格配置列表
     */
    public List<ManufacturerProcessPriceCfg> findProcessPriceCfgsByManufacturerId(String manufacturerId, int current, int size) {
        if (size <= 0 || size > 100) {
            throw new RuntimeException("每页大小必须在 1-100 之间");
        }

        try {
            String url = String.format("%s/process-prices?manufacturerId=%s&current=%d&size=%d",
                    productCoreUrl, manufacturerId, current, size);
            
            ParameterizedTypeReference<ApiResponse<List<ManufacturerProcessPriceCfg>>> typeRef = 
                new ParameterizedTypeReference<ApiResponse<List<ManufacturerProcessPriceCfg>>>() {};
            
            ResponseEntity<ApiResponse<List<ManufacturerProcessPriceCfg>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("获取工艺价格配置失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 更新单个标品商品的价格和状态信息（调用外部系统接口）
     * 
     * @param productCfgId 标品商品配置 ID，不能为空
     * @param price 新的价格信息，可为 null（为 null 时不更新价格）
     * @param status 新的状态信息，可为 null（为 null 时不更新状态）
     */
    public void updateProductPriceAndStatus(String productCfgId, UnitPrice price, String status) {
        if (price == null && status == null) {
            throw new RuntimeException("至少需要提供价格或状态中的一个");
        }

        if (price != null && price.getPrice() == null) {
            throw new RuntimeException("价格不能为空");
        }

        try {
            String url = String.format("%s/mts-products/%s/price-status",
                    productCoreUrl, productCfgId);

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
                throw new RuntimeException("更新标品商品信息失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 更新单个工艺价格配置的价格和状态信息（调用外部系统接口）
     * 
     * @param processPriceId 工艺价格配置 ID，不能为空
     * @param price 新的价格信息，可为 null（为 null 时不更新价格）
     * @param status 新的状态信息，可为 null（为 null 时不更新状态）
     */
    public void updateProcessPriceAndStatus(String processPriceId, UnitPrice price, String status) {
        if (price == null && status == null) {
            throw new RuntimeException("至少需要提供价格或状态中的一个");
        }

        if (price != null && price.getPrice() == null) {
            throw new RuntimeException("价格不能为空");
        }

        try {
            String url = String.format("%s/process-prices/%s/price-status",
                    productCoreUrl, processPriceId);

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
                throw new RuntimeException("更新工艺价格配置失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 注册工厂
     * @param name 工厂名称，不能为空
     * @param terminalRegionCode 终端地区编码，不能为空
     * @param detailAddress 详细地址，不能为空
     * @param consigneeName 收货人姓名，不能为空
     * @param consigneePhone 收货人电话，不能为空
     * @return 工厂ID（rmfId）
     */
    public String registerManufacturer(String name, String terminalRegionCode, String detailAddress,
                                       String consigneeName, String consigneePhone) {
        try {
            String url = String.format("%s/api/internal/mes/rmf/register", productCoreUrl);

            Map<String, Object> address = new HashMap<>();
            address.put("terminalRegionCode", terminalRegionCode);
            address.put("detailAddress", detailAddress);

            Map<String, Object> consignee = new HashMap<>();
            consignee.put("name", consigneeName);
            consignee.put("phone", consigneePhone);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", name);
            requestBody.put("address", address);
            requestBody.put("consignee", consignee);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    (Class<ApiResponse<String>>) (Class<?>) ApiResponse.class
            );
            logger.info("registerManufacturer response ======"+ JSON.toJSONString( response));
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("注册工厂失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
    }

    /**
     * 修改工厂接单状态
     * @param rmfId 工厂ID，不能为空
     * @param status 接单状态，ACCEPTING:接单中 PAUSED：暂停接单
     */
    public void changeOrderAcceptStatus(String rmfId, String status) {
        if (rmfId == null || rmfId.isEmpty()) {
            throw new RuntimeException("工厂ID不能为空");
        }
        if (status == null || status.isEmpty()) {
            throw new RuntimeException("接单状态不能为空");
        }
        if (!"ACCEPTING".equals(status) && !"PAUSED".equals(status)) {
            throw new RuntimeException("接单状态必须为 ACCEPTING 或 PAUSED");
        }

        try {
            String url = String.format("%s/api/internal/mes/rmf/changeOrderAcceptStatus", productCoreUrl);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("rmfId", rmfId);
            requestBody.put("status", status);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    (Class<ApiResponse<String>>) (Class<?>) ApiResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("修改工厂接单状态失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用外部系统失败：" + e.getMessage());
        }
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


    // 内部类用于分页结果
    public static class PagedResult<T> {
        private Long current;
        private Long size;
        private Long total;
        private List<T> items;

        public Long getCurrent() { return current; }
        public void setCurrent(Long current) { this.current = current; }
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        public Long getTotal() { return total; }
        public void setTotal(Long total) { this.total = total; }
        public List<T> getItems() { return items; }
        public void setItems(List<T> items) { this.items = items; }
    }
}
