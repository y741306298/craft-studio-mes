package com.mes.application.command.api.vo;

import lombok.Data;

@Data
public class CallbackConfig {
    private String callbackUrl;
    private Object callbackCustomValue;
}