package com.mes.application.command.api.resp;

import com.mes.application.command.api.vo.CallbackCustomValue;
import lombok.Data;

/** 矩形旗出血结果，同时用于解析算法回调报文。 */
@Data
public class RectBloodResponse {
    private Integer code;
    private String message;
    private Data data;

    @lombok.Data
    public static class Data {
        private String flagUrl;
        /** 未请求镜像时可能为空。 */
        private String mirrorFlagUrl;
        private CallbackCustomValue callbackCustomValue;
    }
}
