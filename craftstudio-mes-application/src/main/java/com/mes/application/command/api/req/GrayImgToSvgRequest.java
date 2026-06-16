package com.mes.application.command.api.req;

import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.UploadConfig;
import lombok.Data;

@Data
public class GrayImgToSvgRequest {
    /**
     * 灰度图 URL。
     */
    private String grayImgUrl;

    /**
     * SVG 上传配置。
     */
    private UploadConfig uploadConfig;

    /**
     * 异步回调配置。
     */
    private CallbackConfig callbackConfig;
}
