package com.mes.application.command.typesetting.proces.buckle;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * “下边打扣”实体策略：沿下边写入扣点。
 */
@Service
public class BottomBuckleProcessStrategy extends AbstractBuckleProcessStrategy {
    public BottomBuckleProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(restTemplate, ossTagUploadService);
    }

    @Override
    protected String nodeName() {
        return "下边打扣";
    }

    @Override
    protected String markKeyPrefix() {
        return "bottom-buckle-point";
    }

    @Override
    protected List<BuckleMarkPoint> buildMarkPoints(double width, double height) {
        return buildEdgeMarkPoints(width, height, List.of(BuckleEdge.BOTTOM));
    }
}
