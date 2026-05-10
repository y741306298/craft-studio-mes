package com.mes.application.dto.resp.auth;

import lombok.Data;

import java.util.Date;

@Data
public class LoginResponse {
    private String token;
    private String manufacturerMetaId;
    private String manufacturerMetaName;
    private String userName;
    private Boolean isAdmin;
    private Date tokenExpireAt;
}
