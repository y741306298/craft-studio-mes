package com.mes.application.command.api.resp;

import lombok.Data;

@Data
public class GrayImgToSvgResponse {
    private Integer code;
    private String msg;
    private Data data;

    @lombok.Data
    public static class Data {
        private String svgObjectName;
        private String id;
    }
}
