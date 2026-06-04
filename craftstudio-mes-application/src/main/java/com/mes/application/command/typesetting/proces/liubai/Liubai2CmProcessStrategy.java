package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * “留白2cm”实体策略。
 */
@Service
public class Liubai2CmProcessStrategy extends AbstractCentimeterLiubaiProcessStrategy {
    public Liubai2CmProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(2, restTemplate, ossTagUploadService);
    }
}
