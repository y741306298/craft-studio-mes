package com.mes.interfaces.api.dto.req.manufacturerMeta;

import com.mes.domain.base.UnitPrice;
import com.mes.interfaces.api.dto.req.base.ApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpdateSpecPriceRequest extends ApiRequest {

    private String manufacturerId;

    private String productId;

    private String specId;

    private UnitPrice price;

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public String getValidationMessage() {
        return "";
    }
}