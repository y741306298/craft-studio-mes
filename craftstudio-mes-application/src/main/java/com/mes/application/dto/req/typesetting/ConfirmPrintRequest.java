package com.mes.application.dto.req.typesetting;

import com.mes.application.dto.req.base.ApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConfirmPrintRequest extends ApiRequest {

    /**
     * 开始打印接口沿用该字段。
     */
    private List<String> productionPieceIds;

    private String id;

    /**
     * 可选：覆盖排版方式。
     */
    private String layoutMode;

    private String deviceInfoId;

    @Override
    public boolean isValid() {
        return id != null && !id.isBlank() && deviceInfoId != null && !deviceInfoId.isBlank();
    }

    @Override
    public String getValidationMessage() {
        if (id == null || id.isBlank()) {
            return "排版ID不能为空";
        }
        if (deviceInfoId == null || deviceInfoId.isBlank()) {
            return "设备编号不能为空";
        }
        return null;
    }
}
