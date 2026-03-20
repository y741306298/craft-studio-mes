package com.mes.domain.base;

import lombok.Data;

import java.util.Date;

@Data
public abstract class BaseEntity {
    private String id;
    private Date createTime;
    private Date updateTime;
}
