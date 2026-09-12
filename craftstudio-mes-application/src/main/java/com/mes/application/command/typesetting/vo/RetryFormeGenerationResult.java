package com.mes.application.command.typesetting.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RetryFormeGenerationResult {
    private int total;
    private int submitted;
    private int skipped;
    private int failed;
    private List<Failure> failures = new ArrayList<>();

    @Data
    public static class Failure {
        private final String id;
        private final String message;
    }
}
