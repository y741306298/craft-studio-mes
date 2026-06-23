package com.mes.application.dto.req.materialLayoutSpec;

import com.mes.application.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialLayoutSpecListRequest extends PagedApiRequest {
    private String materialId;
    private String materialName;
}
