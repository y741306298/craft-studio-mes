package com.mes.application.dto.req.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {
    /**
     * 用户ID（必填）
     */
    private String id;

    /**
     * 用户名称（可选）
     */
    private String name;

    /**
     * 用户手机号（可选）
     */
    private String phone;

    /**
     * 是否管理员（可选）
     */
    private Boolean isAdmin;
}
