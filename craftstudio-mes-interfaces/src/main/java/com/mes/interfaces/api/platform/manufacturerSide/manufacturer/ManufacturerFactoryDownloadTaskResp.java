package com.mes.interfaces.api.platform.manufacturerSide.manufacturer;

import lombok.Data;

import java.util.List;

@Data
public class ManufacturerFactoryDownloadTaskResp {
    private String id;
    private List<String> imamges;
    private List<String> plts;
    private List<String> jsons;
}
