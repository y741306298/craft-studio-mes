package com.mes.interfaces.api.dto.resp.manufacturerMeta;

import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity.ManufacturerProcessPriceCfg;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.vo.MaterialProcessPrice;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ManufacturerProcessCfgListResponse {

    // 基础信息
    private String id;
    private String processId;
    private String manufacturerId;
    private String processName;
    private String processCode;
    private String processType;
    private String processDescription;
    private String capacity;
    private String unit;

    // 价格信息
    private UnitPriceResponse processPrice;
    private Double basePrice;
    private List<MaterialProcessPriceResponse> materialProcessPrices;

    // 时间信息
    private Date createTime;
    private Date updateTime;

    // 状态信息
    private String status;
    private String statusName;

    /**
     * 从 ManufacturerProcessPriceCfg 实体转换为响应 DTO
     * @param cfg 工艺价格配置实体
     * @return ManufacturerProcessCfgListResponse
     */
    public static ManufacturerProcessCfgListResponse from(ManufacturerProcessPriceCfg cfg) {
        if (cfg == null) {
            return null;
        }

        ManufacturerProcessCfgListResponse response = new ManufacturerProcessCfgListResponse();

        // 基础字段映射
        response.setId(cfg.getId());
        response.setProcessId(cfg.getProcessId());
        response.setManufacturerId(cfg.getManufacturerId());
        response.setProcessName(cfg.getProcessName());
        response.setProcessCode(cfg.getProcessCode());
        response.setProcessType(cfg.getProcessType());
        response.setProcessDescription(cfg.getProcessDescription());
        response.setCapacity(cfg.getCapacity());
        response.setUnit(cfg.getUnit());
        response.setCreateTime(cfg.getCreateTime());
        response.setUpdateTime(cfg.getUpdateTime());
        response.setStatus(cfg.getStatus() != null ? cfg.getStatus().getCode() : null);
        response.setStatusName(cfg.getStatus() != null ? cfg.getStatus().getDescription() : null);

        // 价格信息映射
        if (cfg.getProcessPrice() != null) {
            response.setProcessPrice(UnitPriceResponse.from(cfg.getProcessPrice()));
        }
        response.setBasePrice(cfg.getBasePrice());

        // 材料工艺价格映射
        if (cfg.getMaterialProcessPrices() != null && !cfg.getMaterialProcessPrices().isEmpty()) {
            List<MaterialProcessPriceResponse> materialPrices = cfg.getMaterialProcessPrices().stream()
                    .map(MaterialProcessPriceResponse::from)
                    .collect(Collectors.toList());
            response.setMaterialProcessPrices(materialPrices);
        }

        return response;
    }

    @Data
    public static class UnitPriceResponse {
        private Double price;
        private String unit;

        public static UnitPriceResponse from(UnitPrice unitPrice) {
            if (unitPrice == null) {
                return null;
            }
            UnitPriceResponse response = new UnitPriceResponse();
            response.setPrice(unitPrice.getPrice());
            response.setUnit(unitPrice.getUnit());
            return response;
        }
    }

    @Data
    public static class MaterialProcessPriceResponse {
        private String materialId;
        private String materialName;
        private UnitPriceResponse processPrice;
        private Double basePrice;

        public static MaterialProcessPriceResponse from(MaterialProcessPrice materialProcessPrice) {
            if (materialProcessPrice == null) {
                return null;
            }
            MaterialProcessPriceResponse response = new MaterialProcessPriceResponse();
            response.setMaterialId(materialProcessPrice.getMaterialId());
            response.setMaterialName(materialProcessPrice.getMaterialName());
            if (materialProcessPrice.getProcessPrice() != null) {
                response.setProcessPrice(UnitPriceResponse.from(materialProcessPrice.getProcessPrice()));
            }
            response.setBasePrice(materialProcessPrice.getBasePrice());
            return response;
        }
    }
}
