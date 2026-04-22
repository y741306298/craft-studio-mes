package com.mes.application.command.api.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class FormeGenerationResponse {
    private String error;
    private String status;
    private String id;
    private Result result;

    @Data
    public static class Result {
        private String json;
        private PltObjectName plt;
        private String formeSvg;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PltObjectName {
        private String normal;
        private String reverse;
    }
}
