package com.mes.domain.order.orderInfo.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class LogisticsCarrierInfo {
    @JsonAlias("id")
    private String carrierId;
    @JsonAlias("name")
    private String carrierName;
    private String presetType;
}
