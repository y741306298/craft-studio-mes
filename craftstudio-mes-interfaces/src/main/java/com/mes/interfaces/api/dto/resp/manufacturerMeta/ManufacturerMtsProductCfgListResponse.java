package com.mes.interfaces.api.dto.resp.manufacturerMeta;

import com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity.ManufacturerMtsProductCfg;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ManufacturerMtsProductCfgListResponse {

    // 基础信息
    private String id;
    private String manufacturerId;
    private String productId;
    private String productName;
    private String productPreviewUrl;

    // 时间信息
    private Date createTime;
    private Date updateTime;

    // 统计信息
    private Integer specCount;              // 规格数量
    private List<ManufacturerMtsProductSpecSummary> specs;  // 规格摘要信息
    private String status;
    private String statusName;

    /**
     * 从 ManufacturerMtsProductCfg 实体转换为响应 DTO
     * @param cfg 标品商品配置实体
     * @return ManufacturerMtsProductCfgListResponse
     */
    public static ManufacturerMtsProductCfgListResponse from(ManufacturerMtsProductCfg cfg) {
        if (cfg == null) {
            return null;
        }

        ManufacturerMtsProductCfgListResponse response = new ManufacturerMtsProductCfgListResponse();

        // 基础字段映射
        response.setId(cfg.getId());
        response.setManufacturerId(cfg.getManufacturerId());
        response.setProductId(cfg.getProductId());
        response.setProductName(cfg.getProductName());
        response.setProductPreviewUrl(cfg.getProductPreviewUrl());
        response.setCreateTime(cfg.getCreateTime());
        response.setUpdateTime(cfg.getUpdateTime());
        response.setStatus(cfg.getStatus() != null ? cfg.getStatus().getCode() : null);
        response.setStatusName(cfg.getStatus() != null ? cfg.getStatus().getDescription() : null);

        // 规格信息
        if (cfg.getMtsProductSpecs() != null) {
            response.setSpecCount(cfg.getMtsProductSpecs().size());

            // 转换规格摘要信息
            List<ManufacturerMtsProductSpecSummary> specSummaries =
                    cfg.getMtsProductSpecs().stream()
                            .map(ManufacturerMtsProductSpecSummary::from)
                            .collect(Collectors.toList());

            response.setSpecs(specSummaries);
        } else {
            response.setSpecCount(0);
            response.setSpecs(List.of());
        }

        return response;
    }
}
