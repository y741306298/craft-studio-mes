package com.mes.application.dto.req.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddressRecognitionRecordBatchBindRequest {

    @NotEmpty(message = "地址识别记录 ID 不能为空")
    private List<String> recordIds;

    @NotBlank(message = "路线 ID 不能为空")
    private String routeId;

    @NotBlank(message = "节点 ID 不能为空")
    private String nodeId;
}
