package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * “留白7cm”实体策略。
 */
@Service
public class Liubai7CmProcessStrategy extends AbstractCentimeterLiubaiProcessStrategy {
    public Liubai7CmProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(7, restTemplate, ossTagUploadService);
    }
}
