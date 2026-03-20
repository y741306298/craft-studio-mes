package com.mes.interfaces.api.dto.req.manufacturerMeta;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerProductionLineMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerProcedureMeta;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ProductionLineRequest {
    
    @NotBlank(message = "生产线ID不能为空")
    @Size(max = 50, message = "生产线ID长度不能超过50个字符")
    private String productionLineId;
    
    @NotBlank(message = "生产线名称不能为空")
    @Size(max = 100, message = "生产线名称长度不能超过100个字符")
    private String productionLineName;
    
    @Valid
    private List<ManufacturerProcedure> procedures = new ArrayList<ManufacturerProcedure>();

    /**
     * 转换为领域实体
     * @return ManufacturerProductionLineMeta领域实体
     */
    public ManufacturerProductionLineMeta toDomainEntity() {
        ManufacturerProductionLineMeta productionLineMeta = new ManufacturerProductionLineMeta();
        productionLineMeta.setProductionLineId(this.productionLineId);
        productionLineMeta.setProductionLineName(this.productionLineName);
        
        if (this.procedures != null && !this.procedures.isEmpty()) {
            List<ManufacturerProcedureMeta> procedureMetas = this.procedures.stream()
                .map(ManufacturerProcedure::toDomainEntity)
                .collect(Collectors.toList());
            productionLineMeta.setManufacturerProceduresMetas(procedureMetas);
        } else {
            productionLineMeta.setManufacturerProceduresMetas(new ArrayList<>());
        }
        
        return productionLineMeta;
    }

    public boolean isValid() {
        if (productionLineId == null || productionLineId.trim().isEmpty()) {
            return false;
        }
        if (productionLineName == null || productionLineName.trim().isEmpty()) {
            return false;
        }
        if (productionLineId.length() > 50) {
            return false;
        }
        if (productionLineName.length() > 100) {
            return false;
        }
        
        if (procedures != null) {
            for (ManufacturerProcedure procedure : procedures) {
                if (!procedure.isValid()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    public String getValidationMessage() {
        if (productionLineId == null || productionLineId.trim().isEmpty()) {
            return "生产线ID不能为空";
        }
        if (productionLineName == null || productionLineName.trim().isEmpty()) {
            return "生产线名称不能为空";
        }
        if (productionLineId.length() > 50) {
            return "生产线ID长度不能超过50个字符";
        }
        if (productionLineName.length() > 100) {
            return "生产线名称长度不能超过100个字符";
        }
        
        if (procedures != null) {
            for (int i = 0; i < procedures.size(); i++) {
                String procedureMessage = procedures.get(i).getValidationMessage();
                if (!procedureMessage.isEmpty()) {
                    return "第" + (i + 1) + "个工序: " + procedureMessage;
                }
            }
        }
        
        return "";
    }
}
