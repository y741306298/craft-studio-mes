package com.mes.application.dto.req.typesetting;

import com.mes.application.dto.req.base.PagedApiRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class PendingPrintTypesettingListRequest extends PagedApiRequest {

    @NotBlank(message = "manufacturerMetaId不能为空")
    private String manufacturerMetaId;

    private String id;

    /**
     * 排版文件 ID，支持模糊搜索。
     */
    private String typesettingId;

    private Date startTime;

    private Date endTime;

    private String status;
}
