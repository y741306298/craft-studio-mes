package com.mes.interfaces.api.platform.manufacturerSide.manufacturer;

import lombok.Data;

@Data
public class ManufacturerFactoryDeviceBindResp {

    private String name;
    private String sn;
    private String code;
    private Integer version;
}
