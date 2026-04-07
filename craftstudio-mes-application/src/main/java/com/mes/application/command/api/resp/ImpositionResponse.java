package com.mes.application.command.api.resp;

import lombok.Data;

@Data
public class ImpositionResponse {
    private String error;
    private String status;
    private Object id;
    private Result result;

    @Data
    public static class Result {
        private String svg;
        private String json;
    }
}
