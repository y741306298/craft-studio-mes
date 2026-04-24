package com.mes.interfaces.api.platform.manufacturerSide.manufacturer;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ManufacturerTypeVO {
    private final String code;
    private final String chineseName;
}
