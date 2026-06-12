package com.mes.application.dto.req.order;

import com.mes.application.dto.req.base.ApiRequest;
import io.micrometer.common.util.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItemsByOrderIdRequest extends ApiRequest {
    private String manufacturerId;
    private String orderId;

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(orderId);
    }

    @Override
    public String getValidationMessage() {
        if (StringUtils.isBlank(orderId)) {
            return "订单 ID 不能为空";
        }
        return "";
    }
}
