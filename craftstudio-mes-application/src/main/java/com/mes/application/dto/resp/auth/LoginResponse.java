package com.mes.application.dto.resp.auth;

import lombok.Data;

import java.util.Date;

@Data
public class LoginResponse {
    private String token;
    private String manufacturerMetaId;
    private Date tokenExpireAt;
}
