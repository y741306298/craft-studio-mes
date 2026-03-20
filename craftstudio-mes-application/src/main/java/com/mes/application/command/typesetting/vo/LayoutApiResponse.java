package com.mes.application.command.typesetting.vo;

import lombok.Data;

@Data
public class LayoutApiResponse {
    private boolean success;
    private String layoutId;
    private String layoutUrl;
    private String code;
    private String msg;


}
