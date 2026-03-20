package com.mes.domain.manufacturer.procedure.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProcedureAssetHandSign extends BaseEntity {

    private List<String> downloadCfgs;

    private List<String> uploadCfgs;

}
