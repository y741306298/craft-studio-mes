package com.mes.application.command.typesetting.proces.buckle;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * “右边打扣”实体策略：沿右边写入扣点。
 */
@Service
public class RightBuckleProcessStrategy extends AbstractBuckleProcessStrategy {
    public RightBuckleProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(restTemplate, ossTagUploadService);
    }

    @Override
    protected String nodeName() {
        return "右边打扣";
    }

    @Override
    protected String markKeyPrefix() {
        return "right-buckle-point";
    }

    @Override
    protected List<BuckleMarkPoint> buildMarkPoints(double width, double height) {
        return buildEdgeMarkPoints(width, height, List.of(BuckleEdge.RIGHT));
    }

    @Override
    protected List<BuckleMarkPoint> buildMarkPoints(double width, double height, double edgeOffset) {
        return buildEdgeMarkPoints(width, height, List.of(BuckleEdge.RIGHT), edgeOffset);
    }
}
