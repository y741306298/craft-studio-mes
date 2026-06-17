package com.mes.application.dto.req.delivery;

import com.mes.application.dto.req.base.PagedApiRequest;
import io.micrometer.common.util.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AddressRecognitionRecordListRequest extends PagedApiRequest {
    private String manufacturerMetaId;
    private String routeId;
    private String nodeId;
    private String status;
    private Boolean assigned;
    private String name;
    private String detailAddress;

    public String getSearchName() {
        return StringUtils.isNotBlank(name) ? name : detailAddress;
    }
}
