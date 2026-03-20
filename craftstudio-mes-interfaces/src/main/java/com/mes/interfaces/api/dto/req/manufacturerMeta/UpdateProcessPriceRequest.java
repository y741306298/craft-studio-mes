package com.mes.interfaces.api.dto.req.manufacturerMeta;

import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.vo.MaterialProcessPrice;
import com.mes.interfaces.api.dto.req.base.ApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpdateProcessPriceRequest extends ApiRequest {

    private String manufacturerId;

    private String processId;

    private UnitPrice processPrice;

    private Double basePrice;

    private List<MaterialProcessPriceRequest> materialProcessPrices;

    @Override
    public boolean isValid() {
        if (manufacturerId == null || manufacturerId.trim().isEmpty()) {
            return false;
        }
        if (processId == null || processId.trim().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public String getValidationMessage() {
        if (manufacturerId == null || manufacturerId.trim().isEmpty()) {
            return "制造商 ID 不能为空";
        }
        if (processId == null || processId.trim().isEmpty()) {
            return "工艺 ID 不能为空";
        }
        return "";
    }

    @Data
    public static class MaterialProcessPriceRequest {
        private String materialId;
        private String materialName;
        private UnitPrice processPrice;
        private Double basePrice;
    }
}
