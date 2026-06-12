package com.mes.application.dto.req.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class OrderTransferRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderId;

    @NotBlank(message = "转出工厂不能为空")
    private String manufacturerMetaId;

    /**
     * 转入工厂账号，对应 manufacturerUser.account。
     */
    @NotBlank(message = "转入工厂不能为空")
    private String targetId;

    @Valid
    @NotEmpty(message = "转单订单项不能为空")
    private List<OrderTransferItemDto> orderItemDtos;

    @Data
    public static class OrderTransferItemDto {

        @NotBlank(message = "订单项 ID 不能为空")
        private String orderItemId;

        @NotNull(message = "转单数量不能为空")
        @Positive(message = "转单数量必须大于 0")
        private Integer quantity;
    }
}
