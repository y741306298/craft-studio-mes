 package com.mes.application.command.api.resp;

import lombok.Data;
import java.util.List;

@Data
public class ImageMaskResponse {
    private String error;
    private String status;
    private String id;
    private List<Pair> pairs;

    @Data
    public static class Pair {
        private String img;
        private String svg;
    }
}
