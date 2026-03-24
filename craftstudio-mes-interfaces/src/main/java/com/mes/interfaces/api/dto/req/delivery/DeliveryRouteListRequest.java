package com.mes.interfaces.api.dto.req.delivery;

import com.mes.interfaces.api.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeliveryRouteListRequest extends PagedApiRequest {
    private String routeName;
}
