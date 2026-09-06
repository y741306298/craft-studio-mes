package com.mes.application.dto.req.typesetting;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Date;

@Data
public class PendingPrintMaterialListRequest {

    @NotBlank(message = "manufacturerMetaId不能为空")
    private String manufacturerMetaId;

    /**
     * 设备配置 ID。
     */
    private String id;

    private Date startTime;

    private Date endTime;
}
