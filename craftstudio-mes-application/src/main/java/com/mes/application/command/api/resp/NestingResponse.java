package com.mes.application.command.api.resp;

import lombok.Data;

import java.math.BigDecimal;
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
        private BigDecimal utilization;
        private GridLines gridLines;
    }

    @Data
    public static class GridLines {
        private List<Double> xs;
        private List<Double> ys;
    }
}
