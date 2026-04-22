package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import lombok.Data;

import java.util.List;

@Data
public class FormeLayoutBuildResult {
    private FormeGenerationRequest.Margin margin;
    private List<FormeGenerationRequest.Mark> marks;
    private List<FormeGenerationRequest.AnchorPoint> anchorPoints;
    private FormeGenerationRequest.Outputs outputs;
    private String uploadPath;
}
