package com.mes.interfaces.api.dto.resp.manufacturerMeta;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerWorkshopMeta;
import com.mes.domain.manufacturer.manufacturerMeta.enums.ManufacturerType;
import com.mes.interfaces.api.dto.resp.manufacturerMeta.DeviceCfgSummary;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ManufacturerMetaDetailResponse {

    // 基础信息
    private String id;
    private String manufacturerMetaId;
    private String manufacturerMetaType;
    private String manufacturerMetaTypeName;
    private String name;
    private String description;

    // 时间信息
    private Date createTime;
    private Date updateTime;

    // 关联信息概要
    private Integer workshopCount;        // 车间数量
    private List<WorkshopSummary> workshops; // 车间摘要信息

    // 设备配置列表
    private List<DeviceCfgSummary> deviceCfgs;

    @Data
    public static class WorkshopSummary {
        private String workshopId;
        private String workshopName;
        private String status;
        private Integer productionLineCount;  // 生产线数量
    }

    /**
     * 从 ManufacturerMeta 实体转换为响应 DTO
     * @param manufacturerMeta 制造商元数据实体
     * @param deviceCfgs 设备配置列表
     * @return ManufacturerMetaDetailResponse
     */
    public static ManufacturerMetaDetailResponse from(ManufacturerMeta manufacturerMeta, List<DeviceCfgSummary> deviceCfgs) {
        if (manufacturerMeta == null) {
            return null;
        }

        ManufacturerMetaDetailResponse response = new ManufacturerMetaDetailResponse();

        // 基础字段映射
        response.setId(manufacturerMeta.getId());
        response.setManufacturerMetaId(manufacturerMeta.getManufacturerMetaId());
        response.setManufacturerMetaType(manufacturerMeta.getManufacturerMetaType() != null ? manufacturerMeta.getManufacturerMetaType().getCode() : null);
        response.setManufacturerMetaTypeName(manufacturerMeta.getManufacturerMetaType() != null ? manufacturerMeta.getManufacturerMetaType().getDescription() : null);
        response.setName(manufacturerMeta.getName());
        response.setDescription(manufacturerMeta.getDescription());
        response.setCreateTime(manufacturerMeta.getCreateTime());
        response.setUpdateTime(manufacturerMeta.getUpdateTime());

        // 统计信息
        if (manufacturerMeta.getManufacturerWorkshopMetas() != null) {
            response.setWorkshopCount(manufacturerMeta.getManufacturerWorkshopMetas().size());

            // 转换车间摘要信息
            List<WorkshopSummary> workshopSummaries =
                    manufacturerMeta.getManufacturerWorkshopMetas().stream()
                            .map(ManufacturerMetaDetailResponse::convertToWorkshopSummary)
                            .collect(Collectors.toList());

            response.setWorkshops(workshopSummaries);
        } else {
            response.setWorkshopCount(0);
            response.setWorkshops(List.of());
        }

        // 设置设备配置列表
        response.setDeviceCfgs(deviceCfgs != null ? deviceCfgs : List.of());

        return response;
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

        // 统计生产线数量
        if (workshop.getManufacturerProductionLineMetas() != null) {
            summary.setProductionLineCount(workshop.getManufacturerProductionLineMetas().size());
        } else {
            summary.setProductionLineCount(0);
        }

        return summary;
    }
}
