package com.mes.application.dto.req.manufacturerMeta;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManufacturerFactoryDeviceUnbindRequest {

    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;
}
