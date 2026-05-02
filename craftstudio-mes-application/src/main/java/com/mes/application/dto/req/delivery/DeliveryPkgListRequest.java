package com.mes.application.dto.req.delivery;

import com.mes.application.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeliveryPkgListRequest extends PagedApiRequest {

    private String manufacturerMetaId;
    private String recipientName;
    private String trackingNumber;
    private String status;
}
