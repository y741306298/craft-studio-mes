package com.mes.application.dto.req.manufacturerMeta;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerProductionLineMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerWorkshopMeta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class WorkshopRequest {
    
    private String workshopId;
    
    @NotBlank(message = "车间名称不能为空")
    @Size(max = 100, message = "车间名称长度不能超过100个字符")
    private String workshopName;
    
    @Size(max = 20, message = "状态长度不能超过20个字符")
    private String status;
    
    @Valid
    private List<ProductionLineRequest> productionLines = new ArrayList<>();

    /**
     * 转换为领域实体
     * @return ManufacturerWorkshopMeta领域实体
     */
    public ManufacturerWorkshopMeta toDomainEntity() {
        ManufacturerWorkshopMeta workshopMeta = new ManufacturerWorkshopMeta();
        workshopMeta.setWorkshopId(this.workshopId);
        workshopMeta.setWorkshopName(this.workshopName);
        workshopMeta.setStatus(this.status);
        
        if (this.productionLines != null && !this.productionLines.isEmpty()) {
            List<ManufacturerProductionLineMeta> productionLineMetas = this.productionLines.stream()
                .map(ProductionLineRequest::toDomainEntity)
                .collect(Collectors.toList());
            workshopMeta.setManufacturerProductionLineMetas(productionLineMetas);
        } else {
            workshopMeta.setManufacturerProductionLineMetas(new ArrayList<>());
        }
        
        return workshopMeta;
    }

    public boolean isValid() {
        if (workshopName == null || workshopName.trim().isEmpty()) {
            return false;
        }
        if (workshopName.length() > 100) {
            return false;
        }
        if (status != null && status.length() > 20) {
            return false;
        }
        
        if (productionLines != null) {
            for (ProductionLineRequest line : productionLines) {
                if (!line.isValid()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    public String getValidationMessage() {
        if (workshopName == null || workshopName.trim().isEmpty()) {
            return "车间名称不能为空";
        }
        if (workshopName.length() > 100) {
            return "车间名称长度不能超过100个字符";
        }
        if (status != null && status.length() > 20) {
            return "状态长度不能超过20个字符";
        }
        
        if (productionLines != null) {
            for (int i = 0; i < productionLines.size(); i++) {
                String lineMessage = productionLines.get(i).getValidationMessage();
                if (!lineMessage.isEmpty()) {
                    return "第" + (i + 1) + "条生产线: " + lineMessage;
                }
            }
        }
        
        return "";
    }
}
