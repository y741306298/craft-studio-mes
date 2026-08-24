package com.mes.domain.shared.algorithm.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AlgorithmCoreApiCallRecord extends BaseEntity {

    private String mode;
    private String url;
    private String apiPath;
    private String requestBody;
    private String callbackCustomValue;
    private String type;
    private String sourceId;
}
