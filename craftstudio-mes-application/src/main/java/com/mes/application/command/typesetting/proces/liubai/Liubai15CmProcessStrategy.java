package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * “留白15cm”实体策略。
 */
@Service
public class Liubai15CmProcessStrategy extends AbstractCentimeterLiubaiProcessStrategy {
    public Liubai15CmProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(15, restTemplate, ossTagUploadService);
    }
}
