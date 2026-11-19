package com.mes.application.command.typesetting.layout;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

@Service
public class ShapedCrossQr5mmLayoutBuildService extends CrossQrLayoutBuildService {

    public ShapedCrossQr5mmLayoutBuildService(OssTagUploadService ossTagUploadService,
                                              QrLayoutOrderIdResolver qrLayoutOrderIdResolver) {
        super(ossTagUploadService, qrLayoutOrderIdResolver);
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CROSS_5MM;
    }
}
