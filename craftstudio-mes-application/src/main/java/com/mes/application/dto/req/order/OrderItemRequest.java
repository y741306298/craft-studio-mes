package com.mes.application.dto.req.order;

import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import lombok.Data;

@Data
public class OrderItemRequest {
    private Integer id;
    private MTOProductSpecDTO mtoProductSpec;
    private Integer count;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private SpecifyRmfInfo specifyRmfInfo;

    @Data
    public static class LogisticsCarrierInfo {
        private String carrierId;
        private String carrierName;
    }

    @Data
    public static class SpecifyRmfInfo {
        private String rmfId;
        private String rmfName;
    }
}
