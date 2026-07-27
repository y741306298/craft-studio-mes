package com.mes.application.command.wdt.req;

import com.mes.application.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 旺店通快递配置分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WdtConfigListRequest extends PagedApiRequest {
    private String manufacturerMetaId;
    private String presetType;

    public WdtConfigListRequest() {
        setCurrent(1);
        setSize(20);
    }
}
