package com.mes.application.dto.req.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelOrderRequest {
    
    @NotBlank(message = "制造商ID不能为空")
    private String manufacturerMetaId;
    
    @NotBlank(message = "订单号不能为空")
    private String orderId;
}
