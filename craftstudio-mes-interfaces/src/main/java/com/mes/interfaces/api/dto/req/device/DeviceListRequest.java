package com.mes.interfaces.api.dto.req.device;

import com.mes.interfaces.api.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceListRequest extends PagedApiRequest {
    private String deviceName;
}
