package com.mes.application.command.api.req;

import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.UploadConfig;
import lombok.Data;

/**
 * 彩白彩图像转换请求。
 */
@Data
public class ColorWhiteColorRequest {
    /** 原始图像 URL，支持算法服务可访问的 TIFF 文件地址。 */
    private String imageUrl;

    /** 转换结果的 OSS 上传配置。 */
    private UploadConfig uploadConfig;

    /** 回调配置，异步调用时必填。 */
    private CallbackConfig callbackConfig;
}
