package com.mes.application.command.typesetting.layout;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

@Service
public class GridCircleQrLayoutBuildService extends CircleQrLayoutBuildService {

    public GridCircleQrLayoutBuildService(OssTagUploadService ossTagUploadService) {
        super(ossTagUploadService);
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.GRID_TYPESETTING_PLT_QR_CIRCLE;
    }

    /**
     * 处理逻辑与 CircleQrLayoutBuildService 完全一致，仅 supportMode 不同。
     */
    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        return super.build(context);
    }
}
