package com.mes.domain.manufacturer.typesetting.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TypesettingSequencePool extends BaseEntity {
    private String manufacturerMetaId;
    private String usageType;
    private List<Integer> sequenceArray;
}
