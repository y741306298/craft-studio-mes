package com.mes.domain.manufacturer.procedure.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProcedureInputSign extends BaseEntity {
    private List<String> requestParams;
    private List<String> responseParams;
}
