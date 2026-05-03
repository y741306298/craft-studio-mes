package com.mes.application.dto.req.auth;

import com.mes.application.dto.req.base.PagedApiRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserListRequest extends PagedApiRequest {
    @NotBlank(message = "manufacturerMetaId不能为空")
    private String manufacturerMetaId;

    private String phone;
}
