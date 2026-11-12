package com.mes.application.dto.req.storageTank;

import lombok.Data;

@Data
public class StorageTankListRequest {
    private int current = 1;
    private int size = 20;
    private String storageTankName;
    private String storageTankCode;
    private String status;
}
