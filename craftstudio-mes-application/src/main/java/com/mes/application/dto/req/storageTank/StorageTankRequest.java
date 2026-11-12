package com.mes.application.dto.req.storageTank;

import lombok.Data;

/** 储位仅通过可选总数量配置，明细由服务器生成；容量与环境暂不开放配置。 */
@Data
public class StorageTankRequest {
    private String id;
    private String storageTankName;
    private String storageTankCode;
    private String storageTankType;
    private String location;
    private String status;
    private Boolean movable;
    private String description;
    private String managerId;
    private String managerName;
    /** 新增为 null 时生成零个储位，编辑为 null 时保留；0 表示目标为空柜。 */
    private Integer totalSlots;
}
