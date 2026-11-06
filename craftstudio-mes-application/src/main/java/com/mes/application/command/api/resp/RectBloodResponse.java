package com.mes.application.command.api.resp;

import com.mes.application.command.api.vo.CallbackCustomValue;
import lombok.Data;

/** 矩形缝套出血的任务响应及回调结果。 */
@Data
public class RectBloodResponse {
    private Integer code;
    private String message;
    private Data data;

    @lombok.Data
    public static class Data {
        private String flagUrl;
        private String mirrorFlagUrl;
        private CallbackCustomValue callbackCustomValue;
    }
}
