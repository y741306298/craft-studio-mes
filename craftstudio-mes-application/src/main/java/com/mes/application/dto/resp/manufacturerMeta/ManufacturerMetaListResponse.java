package com.mes.application.dto.resp.manufacturerMeta;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerProductionLineMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerWorkshopMeta;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ManufacturerMetaListResponse {

    // 基础信息
    private String id;
    private String manufacturerMetaId;
    private String manufacturerMetaType;
    private String manufacturerMetaTypeName;
    private String name;
    private String description;
    private String status;
    
    // 时间信息
    private Date createTime;
    private Date updateTime;
    
    // 关联信息概要
    private Integer workshopCount;        // 车间数量
    private Integer deviceCount;          // 设备数量
    private List<WorkshopSummary> workshops; // 车间摘要信息
    private List<DeviceCfgSummary> deviceCfgs; // 设备配置列表
    
    @Data
    public static class WorkshopSummary {
        private String workshopId;
        private String workshopName;
        private String status;
        private Integer productionLineCount;  // 生产线数量
        private List<String> productionLines;
    }

    /**
     * 从 ManufacturerMeta 实体转换为响应 DTO（仅包含基本信息）
     * @param manufacturerMeta 制造商元数据实体
     * @return ManufacturerMetaListResponse
     */
    public static ManufacturerMetaListResponse from(ManufacturerMeta manufacturerMeta) {
        if (manufacturerMeta == null) {
            return null;
        }
        
        ManufacturerMetaListResponse response = new ManufacturerMetaListResponse();
        
        // 基础字段映射
        response.setId(manufacturerMeta.getId());
        response.setManufacturerMetaId(manufacturerMeta.getManufacturerMetaId());
        response.setManufacturerMetaType(manufacturerMeta.getManufacturerMetaType() != null ? manufacturerMeta.getManufacturerMetaType().getCode() : null);
        response.setManufacturerMetaTypeName(manufacturerMeta.getManufacturerMetaType() != null ? manufacturerMeta.getManufacturerMetaType().getDescription() : null);
        response.setName(manufacturerMeta.getName());
        response.setDescription(manufacturerMeta.getDescription());
        response.setCreateTime(manufacturerMeta.getCreateTime());
        response.setUpdateTime(manufacturerMeta.getUpdateTime());
        response.setStatus(manufacturerMeta.getStatus().getCode());
        
        // 统计信息
        if (manufacturerMeta.getManufacturerWorkshopMetas() != null) {
            response.setWorkshopCount(manufacturerMeta.getManufacturerWorkshopMetas().size());
            
            // 转换车间摘要信息
            List<WorkshopSummary> workshopSummaries = 
                manufacturerMeta.getManufacturerWorkshopMetas().stream()
                    .map(ManufacturerMetaListResponse::convertToWorkshopSummary)
                    .collect(Collectors.toList());
                    
            response.setWorkshops(workshopSummaries);
        } else {
            response.setWorkshopCount(0);
            response.setWorkshops(List.of());
        }
        
        // 设备数量和列表默认为空（需要从其他地方获取）
        response.setDeviceCount(0);
        response.setDeviceCfgs(List.of());
        
        return response;
    }

    /**
     * 设置设备配置信息（由外部调用）
     * @param deviceCfgs 设备配置列表
     */
    public void setDeviceCfgsFromExternal(List<DeviceCfgSummary> deviceCfgs) {
        this.deviceCfgs = deviceCfgs != null ? deviceCfgs : List.of();
        this.deviceCount = this.deviceCfgs.size();
    }

    /**
     * 转换车间为摘要信息
     * @param workshop 车间实体
     * @return 车间摘要
     */
    private static WorkshopSummary convertToWorkshopSummary(ManufacturerWorkshopMeta workshop) {
        WorkshopSummary summary = new WorkshopSummary();
        summary.setWorkshopId(workshop.getWorkshopId());
        summary.setWorkshopName(workshop.getWorkshopName());
        summary.setStatus(workshop.getStatus());
        
        // 统计生产线数量并返回产线名称列表
        if (workshop.getManufacturerProductionLineMetas() != null) {
            summary.setProductionLineCount(workshop.getManufacturerProductionLineMetas().size());
            
            // 提取所有产线的名称
            List<String> productionLines = workshop.getManufacturerProductionLineMetas().stream()
                    .map(ManufacturerProductionLineMeta::getProductionLineName)
                    .collect(Collectors.toList());
            summary.setProductionLines(productionLines);
        } else {
            summary.setProductionLineCount(0);
            summary.setProductionLines(List.of());
        }
        
        return summary;
    }
}
