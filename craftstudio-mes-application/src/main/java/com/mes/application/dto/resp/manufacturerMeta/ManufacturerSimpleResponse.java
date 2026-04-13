package com.mes.application.dto.resp.manufacturerMeta;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ManufacturerSimpleResponse {
    private String manufacturerType;
    private String id;
    private String manufacturerMetaId;
    private String name;
}
