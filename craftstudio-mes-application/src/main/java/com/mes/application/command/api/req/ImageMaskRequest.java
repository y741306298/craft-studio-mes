package com.mes.application.command.api.req;

import lombok.Data;

@Data
public class ImageMaskRequest {
    private RawImage rawImage;
    private String maskSvgUrl;
    private UploadConfig uploadConfig;
    private CallbackConfig callbackConfig;

    @Data
    public static class RawImage {
        private String url;
        private ImageProperties imageProperties;
    }

    @Data
    public static class ImageProperties {
        private String colorSpace;
        private Integer dpiX;
        private Integer dpiY;
        private Integer width;
        private Integer height;
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
