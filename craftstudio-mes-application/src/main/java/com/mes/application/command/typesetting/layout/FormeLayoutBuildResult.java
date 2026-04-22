package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import lombok.Data;

import java.util.List;

@Data
public class FormeLayoutBuildResult {
    /** 版面四边距。 */
    private FormeGenerationRequest.Margin margin;
    /** 标记元素（二维码条、辅助线等）。 */
    private List<FormeGenerationRequest.Mark> marks;
    /** 定位点元素。 */
    private List<FormeGenerationRequest.AnchorPoint> anchorPoints;
    /** 输出文件配置（json/plt/svg）。 */
    private FormeGenerationRequest.Outputs outputs;
    /** 上传目录（相对 OSS 路径）。 */
    private String uploadPath;
}
