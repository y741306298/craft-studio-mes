package com.mes.application.dto.req.typesetting;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import lombok.Data;

import java.util.List;

@Data
public class BatchConfirmLayoutRequest {
    private List<TypesettingInfo> typesettingInfos;
}
