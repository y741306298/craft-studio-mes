package com.mes.application.dto.req.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserPasswordRequest {
    @NotBlank(message = "用户ID不能为空")
    private String id;

    @NotBlank(message = "密码不能为空")
    private String password;
}
