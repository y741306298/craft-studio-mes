package com.mes.application.command.typesetting.proces.buckle;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * “左右打扣”实体策略：沿左边与右边写入扣点。
 */
@Service
public class LeftRightBuckleProcessStrategy extends AbstractBuckleProcessStrategy {
    public LeftRightBuckleProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(restTemplate, ossTagUploadService);
    }

    @Override
    protected String nodeName() {
        return "左右打扣";
    }

    @Override
    protected String markKeyPrefix() {
        return "left-right-buckle-point";
    }

    @Override
    protected List<BuckleMarkPoint> buildMarkPoints(double width, double height) {
        return buildEdgeMarkPoints(width, height, List.of(BuckleEdge.RIGHT, BuckleEdge.LEFT));
    }

    @Override
    protected List<BuckleMarkPoint> buildMarkPoints(double width, double height, double edgeOffset) {
        return buildEdgeMarkPoints(width, height, List.of(BuckleEdge.RIGHT, BuckleEdge.LEFT), edgeOffset);
    }
}
