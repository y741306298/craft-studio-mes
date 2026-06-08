package com.mes.application.dto.req.order;

import com.mes.application.dto.req.base.PagedApiRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderTransferRecordListRequest extends PagedApiRequest {

    @NotBlank(message = "制造商 ID 不能为空")
    private String manufacturerMetaId;
}
