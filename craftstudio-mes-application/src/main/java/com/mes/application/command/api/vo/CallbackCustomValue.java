package com.mes.application.command.api.vo;

import lombok.Data;

@Data
public class CallbackCustomValue {

    private String id;

    /**
     * 原始订单项 ID。id 字段会编码预处理请求 ID，该字段保留纯订单项 ID 方便算法服务透传。
     */
    private String orderItemId;

    /**
     * 本次预处理请求 ID，用于回调时丢弃重做前的旧请求结果。
     */
    private String preprocessRequestId;

    private String presetType;

}
