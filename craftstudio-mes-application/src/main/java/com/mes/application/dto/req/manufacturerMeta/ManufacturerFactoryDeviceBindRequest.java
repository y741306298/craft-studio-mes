package com.mes.application.dto.req.manufacturerMeta;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManufacturerFactoryDeviceBindRequest {

    @NotBlank(message = "设备编号不能为空")
    private String id;
}
