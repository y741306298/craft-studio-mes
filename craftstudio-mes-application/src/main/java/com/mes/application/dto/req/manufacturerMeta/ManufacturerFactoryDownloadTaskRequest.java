package com.mes.application.dto.req.manufacturerMeta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManufacturerFactoryDownloadTaskRequest {

    @Valid
    @NotNull(message = "machine不能为空")
    private Machine machine;

    @Data
    public static class Machine {
        @NotBlank(message = "设备id不能为空")
        private String id;

        @NotNull(message = "绑定版本不能为空")
        private Integer version;
    }
}
