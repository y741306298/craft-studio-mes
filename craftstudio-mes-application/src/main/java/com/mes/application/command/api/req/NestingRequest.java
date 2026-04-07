package com.mes.application.command.api.req;

import lombok.Data;
import java.util.List;

@Data
public class NestingRequest {
    private NestManifest nestManifest;
    private UploadConfig uploadConfig;
    private CallbackConfig callbackConfig;

    @Data
    public static class NestManifest {
        private Integer spacing;
        private List<Container> containers;
        private List<Element> elements;
    }

    @Data
    public static class Container {
        private Integer width;
        private Integer height;
    }

    @Data
    public static class Element {
        private String id;
        private String img;
        private String svg;
        private Integer counts;
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
