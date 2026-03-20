package com.mes.domain.manufacturer.procedure.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProcedureAssetSign extends BaseEntity {

    private List<String> assets;

}
