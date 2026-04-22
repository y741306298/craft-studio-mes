package com.mes.application.dto.req.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddUserRequest {
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "manufacturerMetaId不能为空")
    private String manufacturerMetaId;

    private String name;
    private String phone;
}
