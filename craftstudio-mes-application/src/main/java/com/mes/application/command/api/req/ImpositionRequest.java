package com.mes.application.command.api.req;

import lombok.Data;
import java.util.List;

@Data
public class ImpositionRequest {
    private ImpositionManifest impositionManifest;
    private UploadConfig uploadConfig;
    private CallbackConfig callbackConfig;

    @Data
    public static class ImpositionManifest {
        private String basePath;
        private ModelConfig modelConfig;
        private Container container;
        private List<Segment> segments;
    }

    @Data
    public static class ModelConfig {
        private DtpConfig dtp;
        private List<Integer> margin;
    }

    @Data
    public static class DtpConfig {
        private String newpage;
        private String showmode;
        private String autoSaveFile;
        private String tpfSavePath;
    }

    @Data
    public static class Container {
        private Integer width;
        private Integer height;
    }

    @Data
    public static class Segment {
        private String svgUrl;
    }

    @Data
    public static class UploadConfig {
        private OssConfig ossConfig;
        private String uploadPath;
    }

    @Data
    public static class OssConfig {
        private StsToken stsToken;
        private String bucket;
        private String region;
    }

    @Data
    public static class StsToken {
        private String accessKeyId;
        private String accessKeySecret;
        private String expiration;
        private String securityToken;
    }

    @Data
    public static class CallbackConfig {
        private String callbackUrl;
        private Object callbackCustomValue;
    }
}
