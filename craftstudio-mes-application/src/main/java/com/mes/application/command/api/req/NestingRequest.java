package com.mes.application.command.api.req;

import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.UploadConfig;
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
        /**
         * 是否需要生成 plt 结果
         */
        private Boolean requirePlt;
        /**
         * 是否需要生成镜像结果
         */
        private Boolean mirrorAppend;
        /**
         * 镜像结果是否需要 plt
         */
        private Boolean mirrorRequirePlt;
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
        /**
         * 兼容算法侧字段命名
         */
        private String imgFile;
        private Boolean forme;
        private String svg;
        private Integer counts;
        /**
         * 竖向边距（竖向重力方向固定为 top）
         */
        private Integer vMargin;
        /**
         * 横向重力方向：left / right
         */
        private String hGravity;
        /**
         * 横向边距
         */
        private Integer hMargin;
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


}
