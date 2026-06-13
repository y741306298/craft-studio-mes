package com.mes.application.dto.req.delivery;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRecognitionRecordBindRequest {

    @NotBlank(message = "地址识别记录 ID 不能为空")
    private String recordId;

    @NotBlank(message = "路线 ID 不能为空")
    private String routeId;

    @NotBlank(message = "节点 ID 不能为空")
    private String nodeId;

    private Integer order;
}
