package com.mes.application.command.api.resp;

import lombok.Data;
import java.util.List;

@Data
public class NestingResponse {
    private String error;
    private String status;
    private String id;
    private List<Result> results;

    @Data
    public static class Result {
        private String nestedSvg;
        private Double utilization;
    }
}
