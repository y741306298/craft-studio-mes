package com.mes.application.dto.req.packagingBox;

import com.mes.application.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class PackagingBoxListRequest extends PagedApiRequest {
    private String boxName;
    private String code;
    private BigDecimal cardboardHeight;
    private BigDecimal cardboardWidth;
}
