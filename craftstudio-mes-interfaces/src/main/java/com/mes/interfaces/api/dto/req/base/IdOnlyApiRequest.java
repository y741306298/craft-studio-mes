package com.mes.interfaces.api.dto.req.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class IdOnlyApiRequest extends ApiRequest {
    private String id;
    @Override
    public boolean isValid() {
        return id!=null;
    }

    @Override
    public String getValidationMessage() {
        if(id==null) return "id 不能为空";
        return "";
    }
}
