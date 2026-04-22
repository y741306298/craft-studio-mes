package com.mes.application.command.api.req;

import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.UploadConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FormeGenerationRequest {
    private FormeInfo forme;
    private Outputs outputs;
    private UploadConfig uploadConfig;
    private CallbackConfig callbackConfig;

    @Data
    public static class FormeInfo {
        private String svgUrl;
        private Margin margin;
        private List<Mark> marks;
        private List<AnchorPoint> anchorPoints;
    }

    @Data
    public static class Margin {
        private Integer left;
        private Integer top;
        private Integer right;
        private Integer bottom;
    }

    @Data
    public static class Mark {
        private String img;
        private Size size;
        private Position position;
    }

    @Data
    public static class AnchorPoint {
        private String img;
        private String svg;
        private Size size;
        private Position position;
    }

    @Data
    public static class Size {
        private BigDecimal width;
        private BigDecimal height;
    }

    @Data
    public static class Position {
        private Integer x;
        private Integer y;
    }

    @Data
    public static class Outputs {
        private OutputConfig json;
        private OutputConfig plt;
        private OutputConfig formeSvg;
    }

    @Data
    public static class OutputConfig {
        private Object objectName;
        private String direction;
        private EnvConfig env;
    }

    /**
     * PLT 文件对象名配置（支持正常和旋转180度两种）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PltObjectName {
        private String normal;
        private String reverse;
    }

    @Data
    public static class EnvConfig {
        private DtpConfig dtp;
        private String basePath;
    }

    @Data
    public static class DtpConfig {
        private String newpage;
        private String showmode;
        private String autoSaveFile;
        private String tpfSavePath;
    }

}
