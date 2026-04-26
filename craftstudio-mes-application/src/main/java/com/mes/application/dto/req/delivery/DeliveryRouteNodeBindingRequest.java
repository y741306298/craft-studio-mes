package com.mes.application.dto.req.delivery;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryRouteNodeBindingRequest {

    @NotBlank(message = "终端地区编码不能为空")
    private String terminalRegionCode;

    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    @NotBlank(message = "路线节点ID不能为空")
    private String routeNodeId;
}
