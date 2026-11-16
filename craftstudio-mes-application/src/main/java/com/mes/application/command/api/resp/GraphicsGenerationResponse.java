package com.mes.application.command.api.resp;

import com.mes.application.command.api.vo.GraphicsCallbackCustomValue;
import lombok.Data;

/** Terminal result delivered by imgUtil, not the HTTP 202 acceptance response. */
@Data
public class GraphicsGenerationResponse {
    private String id;
    private String status;
    private Result result;
    private String error;
    private String errorCode;
    private GraphicsCallbackCustomValue callbackCustomValue;
    @Data public static class Result {
        private String taskId;
        private Artifact artifact;
    }
    @Data public static class Artifact {
        private String filename;
        private String mediaType;
        private long bytes;
        private String sha256;
        private double width;
        private double height;
        private String unit;
        private String downloadPath;
        private String objectName;
    }
}
