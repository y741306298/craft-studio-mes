package com.mes.application.command.api.req;

import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.UploadConfig;
import lombok.Data;

/** 矩形旗出血请求。 */
@Data
public class RectBloodRequest {
    private String imageUrl;
    private BloodConfig bloodConfig;
    private UploadConfig uploadConfig;
    /** 异步调用时必填。 */
    private CallbackConfig callbackConfig;

    @Data
    public static class BloodConfig {
        private ScrollEdge scrollEdge;
        /** null 或不传时由算法自动采样边缘主色。 */
        private String scrollColorHex;
        private Integer scrollWidth;
        private Integer sewWidth;
        private Boolean mirrorRequired;
    }

    public enum ScrollEdge {
        LEFT, TOP, BOTTOM, RIGHT
    }
}
