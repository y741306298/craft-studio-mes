package com.mes.application.dto.req.device;

import com.mes.application.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceListRequest extends PagedApiRequest {
    private String deviceType;
    private String deviceName;
}
