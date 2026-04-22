package com.mes.domain.auth.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerUser extends BaseEntity {
    private String account;
    private String password;
    private String manufacturerMetaId;
    private String name;
    private String phone;
}
