package com.mes.application.dto.req.manufacturerMeta;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManufacturerFactoryDeviceUnbindRequest {

    @NotBlank(message = "设备ID不能为空")
    private String id;
}
