package com.mes.application.command.api.req;

import com.mes.domain.base.UnitPrice;
import lombok.Data;

@Data
public class ConfigMTSProductSpecRequest {

    private String rmfId;
    private String mtsProductSpecId;
    private Integer stock;
    private UnitPrice unitPrice;
    private UnitPrice price;
}
