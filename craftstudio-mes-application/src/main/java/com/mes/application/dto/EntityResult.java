package com.mes.application.dto;

import lombok.Data;

import java.util.Date;

@Data
public class EntityResult {
    private String id;
    private Date createTime;
    private Date updateTime;
}
