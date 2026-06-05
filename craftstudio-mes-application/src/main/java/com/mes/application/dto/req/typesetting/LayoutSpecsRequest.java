package com.mes.application.dto.req.typesetting;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LayoutSpecsRequest {

    @NotEmpty(message = "材料ID列表不能为空")
    private List<String> materialIds;
}
