package com.mes.application.command.typesetting.layout;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

@Service
public class CaifuOpenBackA30HFilmLayoutBuildService extends CaifuLayoutBuildService {
    public CaifuOpenBackA30HFilmLayoutBuildService(OssTagUploadService ossTagUploadService) {
        super(ossTagUploadService);
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_FILM;
    }
}
