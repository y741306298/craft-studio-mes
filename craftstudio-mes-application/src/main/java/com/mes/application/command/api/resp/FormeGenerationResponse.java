package com.mes.application.command.api.resp;

import lombok.Data;

@Data
public class FormeGenerationResponse {
    private String error;
    private String status;
    private String id;
    private Result result;

    @Data
    public static class Result {
        private String json;
        private String plt;
        private String formeSvg;
    }
}
