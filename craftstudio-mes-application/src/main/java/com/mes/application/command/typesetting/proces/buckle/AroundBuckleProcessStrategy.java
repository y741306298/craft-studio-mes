package com.mes.application.command.typesetting.proces.buckle;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * “四周打扣”实体策略：沿上、右、下、左四条边写入扣点，相邻扣点间距不超过 300mm。
 */
@Service
public class AroundBuckleProcessStrategy extends AbstractBuckleProcessStrategy {
    public AroundBuckleProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(restTemplate, ossTagUploadService);
    }

    @Override
    protected String nodeName() {
        return "四周打扣";
    }

    @Override
    protected String markKeyPrefix() {
        return "around-buckle-point";
    }

    @Override
    protected List<BuckleMarkPoint> buildMarkPoints(double width, double height) {
        return buildEdgeMarkPoints(width, height, List.of(BuckleEdge.TOP, BuckleEdge.RIGHT, BuckleEdge.BOTTOM, BuckleEdge.LEFT));
    }
}
