package com.mes.application.command.typesetting.proces.buckle;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * “四角打扣”实体策略：只在矩形四角内缩位置写入四个扣点。
 */
@Service
public class FourCornerBuckleProcessStrategy extends AbstractBuckleProcessStrategy {
    public FourCornerBuckleProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(restTemplate, ossTagUploadService);
    }

    @Override
    protected String nodeName() {
        return "四角打扣";
    }

    @Override
    protected String markKeyPrefix() {
        return "four-corner-buckle-point";
    }

    @Override
    protected List<BuckleMarkPoint> buildMarkPoints(double width, double height) {
        return buildCornerMarkPoints(width, height);
    }

    @Override
    protected List<BuckleMarkPoint> buildMarkPoints(double width, double height, double edgeOffset) {
        return buildCornerMarkPoints(width, height, edgeOffset);
    }
}
