package com.mes.application.command.api.resp;

import com.mes.domain.base.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
public class MtsProductCategoryResponse extends BaseEntity {

    private String parentId;
    private String name;
    private Integer level;
    private Boolean terminal;
}
