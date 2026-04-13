package com.mes.application.dto.req.manufacturerMeta;

import com.mes.application.dto.req.base.ApiRequest;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerWorkshopMeta;
import com.mes.domain.manufacturer.manufacturerMeta.enums.ManufacturerType;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Consignee;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerMetaRequest extends ApiRequest {
    private String id;

    private String manufacturerMetaId;

    private String manufacturerTempId;

    private Consignee consignee;
    private Address address;
    
    @NotBlank(message = "制造商类型不能为空")
    @Size(max = 100, message = "制造商类型长度不能超过 100 个字符")
    private String manufacturerMetaType;
    
    @NotBlank(message = "制造商名称不能为空")
    @Size(max = 200, message = "制造商名称长度不能超过 200 个字符")
    private String name;
    
    @Size(max = 720, message = "描述长度不能超过 720 个字符")
    private String description;
    
    private String status;

    @Valid
    private List<WorkshopRequest> workshops = new ArrayList<>();
    
    @Valid
    private List<ManufacturerDeviceRequest> devices = new ArrayList<>();

    /**
     * 转换为领域实体
     * @return ManufacturerMeta 领域实体
     */
    public ManufacturerMeta toDomainEntity() {
        ManufacturerMeta manufacturerMeta = new ManufacturerMeta();
        
        // 基础字段映射
        manufacturerMeta.setManufacturerMetaId(this.manufacturerMetaId);
        manufacturerMeta.setManufacturerTempId(this.manufacturerTempId);
        manufacturerMeta.setManufacturerMetaType(ManufacturerType.getByCode(this.manufacturerMetaType));
        manufacturerMeta.setName(this.name);
        manufacturerMeta.setDescription(this.description);
        manufacturerMeta.setConsignee(this.consignee);
        manufacturerMeta.setAddress(this.address);
        
        // 设置状态，默认为 NORMAL
        if (this.status != null && !this.status.trim().isEmpty()) {
            manufacturerMeta.setStatus(com.mes.domain.manufacturer.enums.CfgStatus.getByCode(this.status));
        } else {
            manufacturerMeta.setStatus(com.mes.domain.manufacturer.enums.CfgStatus.NORMAL);
        }
        
        // 转换车间信息
        if (this.workshops != null && !this.workshops.isEmpty()) {
            List<ManufacturerWorkshopMeta> workshopMetas = this.workshops.stream()
                .map(WorkshopRequest::toDomainEntity)
                .collect(Collectors.toList());
            manufacturerMeta.setManufacturerWorkshopMetas(workshopMetas);
        } else {
            manufacturerMeta.setManufacturerWorkshopMetas(new ArrayList<>());
        }
        
        return manufacturerMeta;
    }

    @Override
    public boolean isValid() {
        // 基本非空验证
        if (manufacturerMetaType == null || manufacturerMetaType.trim().isEmpty()) {
            return false;
        }
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // 长度验证
        if (manufacturerMetaType.length() > 100) {
            return false;
        }
        if (name.length() > 200) {
            return false;
        }
        if (description != null && description.length() > 720) {
            return false;
        }
        
        // 嵌套对象验证
        if (workshops != null) {
            for (WorkshopRequest workshop : workshops) {
                if (!workshop.isValid()) {
                    return false;
                }
            }
        }
        
        if (devices != null) {
            for (ManufacturerDeviceRequest device : devices) {
                if (!device.isValid()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    @Override
    public String getValidationMessage() {
        if (manufacturerMetaType == null || manufacturerMetaType.trim().isEmpty()) {
            return "制造商类型不能为空";
        }
        if (name == null || name.trim().isEmpty()) {
            return "制造商名称不能为空";
        }
        if (manufacturerMetaType.length() > 100) {
            return "制造商类型长度不能超过 100 个字符";
        }
        if (name.length() > 200) {
            return "制造商名称长度不能超过 200 个字符";
        }
        if (description != null && description.length() > 720) {
            return "描述长度不能超过 720 个字符";
        }
        
        // 嵌套对象验证消息
        if (workshops != null) {
            for (int i = 0; i < workshops.size(); i++) {
                String workshopMessage = workshops.get(i).getValidationMessage();
                if (!workshopMessage.isEmpty()) {
                    return "第" + (i + 1) + "个车间：" + workshopMessage;
                }
            }
        }
        
        if (devices != null) {
            for (int i = 0; i < devices.size(); i++) {
                String deviceMessage = devices.get(i).getValidationMessage();
                if (!deviceMessage.isEmpty()) {
                    return "第" + (i + 1) + "个设备：" + deviceMessage;
                }
            }
        }
        
        return "";
    }
}
