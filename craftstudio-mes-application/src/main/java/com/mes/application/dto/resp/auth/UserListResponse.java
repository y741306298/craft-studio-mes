package com.mes.application.dto.resp.auth;

import com.mes.domain.auth.entity.ManufacturerUser;
import lombok.Data;

@Data
public class UserListResponse {
    private String id;
    private String account;
    private String manufacturerMetaId;
    private String name;
    private String phone;
    private Boolean isAdmin;

    public static UserListResponse from(ManufacturerUser user) {
        UserListResponse response = new UserListResponse();
        response.setId(user.getId());
        response.setAccount(user.getAccount());
        response.setManufacturerMetaId(user.getManufacturerMetaId());
        response.setName(user.getName());
        response.setPhone(user.getPhone());
        response.setIsAdmin(user.getIsAdmin());
        return response;
    }
}
