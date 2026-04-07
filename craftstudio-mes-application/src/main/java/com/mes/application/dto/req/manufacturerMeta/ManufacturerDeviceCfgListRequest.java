package com.mes.application.dto.req.manufacturerMeta;

import com.mes.application.dto.req.base.PagedApiRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ManufacturerDeviceCfgListRequest extends PagedApiRequest {
    @NotBlank(message = "manufacturerMetaId不能为空")
    private String manufacturerMetaId;
}
