package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * “留白10cm”实体策略。
 */
@Service
public class Liubai10CmProcessStrategy extends AbstractCentimeterLiubaiProcessStrategy {
    public Liubai10CmProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(10, restTemplate, ossTagUploadService);
    }
}
