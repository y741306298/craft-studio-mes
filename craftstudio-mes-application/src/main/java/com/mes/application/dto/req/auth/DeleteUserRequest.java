package com.mes.application.dto.req.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteUserRequest {
    @NotBlank(message = "用户ID不能为空")
    private String id;
}
