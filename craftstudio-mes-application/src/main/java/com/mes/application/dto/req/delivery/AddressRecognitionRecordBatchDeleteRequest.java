package com.mes.application.dto.req.delivery;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddressRecognitionRecordBatchDeleteRequest {

    @NotEmpty(message = "地址识别记录 ID 不能为空")
    private List<String> recordIds;
}
