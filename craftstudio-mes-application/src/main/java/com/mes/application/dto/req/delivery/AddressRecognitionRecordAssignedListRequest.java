package com.mes.application.dto.req.delivery;

import com.mes.application.dto.req.base.PagedApiRequest;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AddressRecognitionRecordAssignedListRequest extends PagedApiRequest {

    @NotBlank(message = "路线 ID 不能为空")
    private String routeId;

    @NotBlank(message = "节点 ID 不能为空")
    private String nodeId;

    private String name;
    private String detailAddress;

    public String getSearchName() {
        return StringUtils.isNotBlank(name) ? name : detailAddress;
    }
}
