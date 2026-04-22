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
        /**
         * 兼容历史返回：部分算法版本会直接回传 width/height。
         */
        private BigDecimal width;
        private BigDecimal height;
        private GridLines gridLines;
        /**
         * 当前主返回格式：容器实际尺寸（即 nestedSvg 的宽高）。
         */
        private ContainerSize containerSize;
    }

    @Data
    public static class ContainerSize {
        private BigDecimal width;
        private BigDecimal height;
    }

    @Data
    public static class GridLines {
        private List<Double> xs;
        private List<Double> ys;
    }
}
