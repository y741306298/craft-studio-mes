package com.mes.application.dto.req.manufacturerMeta;

import com.mes.application.dto.req.base.ApiRequest;
import com.mes.domain.base.UnitPrice;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpdateProcessPriceRequest extends ApiRequest {

    private String rmfId;

    private String processMetaId;

    private UnitPrice unitPrice;

    private Double basePrice;

    private List<MaterialProcessPriceRequest> materialProcessPrices;

    @Override
    public boolean isValid() {
        if (rmfId == null || rmfId.trim().isEmpty()) {
            return false;
        }
        if (processMetaId == null || processMetaId.trim().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public String getValidationMessage() {
        if (rmfId == null || rmfId.trim().isEmpty()) {
            return "制造商 ID 不能为空";
        }
        if (processMetaId == null || processMetaId.trim().isEmpty()) {
            return "工艺 ID 不能为空";
        }
        return "";
    }

    @Data
    public static class MaterialProcessPriceRequest {
        private String materialId;
        private String materialName;
        private UnitPrice unitPrice;
        private Double basePrice;
    }
}
