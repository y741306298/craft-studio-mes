package com.mes.application.command.api.resp;

import com.mes.application.command.api.vo.CallbackCustomValue;
import lombok.Data;

/**
 * 彩白彩图像转换结果，同时用于解析算法回调报文。
 */
@Data
public class ColorWhiteColorResponse {
    private Integer code;
    private String message;
    private Data data;

    @lombok.Data
    public static class Data {
        private String downloadUrl;
        private CallbackCustomValue callbackCustomValue;
    }
}
