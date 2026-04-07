package com.mes.application.dto.req.manufacturerMeta;

import com.mes.application.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ManufacturerMetaListRequest extends PagedApiRequest {
    private String name;
    private String manufacturerType;
}
