 package com.mes.application.command.api.resp;

import lombok.Data;
import java.util.List;

@Data
public class ImageMaskResponse {
    private String error;
    private String status;
    private String id;
    private String taskId;
    private String task_id;
    private List<Pair> pairs;

    @Data
    public static class Pair {
        private String img;
        private String svg;
        private String previewImg;
        private String thumbnail;
        private Blood blood;
    }

    @Data
    public static class Blood {
        private Integer x;
        private Integer y;
    }
}
