package com.mes.interfaces.api.dto.req.manufacturerMeta;

import com.mes.interfaces.api.dto.req.base.PagedApiRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ManufacturerMtsProductCfgListRequest extends PagedApiRequest {
    @NotBlank(message = "manufacturerId不能为空")
    private String manufacturerId;
    private String productName;
}
