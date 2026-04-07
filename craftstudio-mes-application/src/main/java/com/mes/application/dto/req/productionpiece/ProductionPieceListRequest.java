package com.mes.application.dto.req.productionpiece;

import com.mes.application.dto.req.base.ApiRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductionPieceListRequest extends ApiRequest {

    private String productionPieceId;

    private String manufacturerId;

    private String status;

    private String material;

    private String nodeName;

    private Date startTime;

    private Date endTime;

    @Min(value = 1, message = "当前页码不能小于 1")
    private Integer current = 1;

    @Min(value = 1, message = "每页大小不能小于 1")
    @Max(value = 100, message = "每页大小不能大于 100")
    private Integer size = 10;

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public String getValidationMessage() {
        return "";
    }
}
