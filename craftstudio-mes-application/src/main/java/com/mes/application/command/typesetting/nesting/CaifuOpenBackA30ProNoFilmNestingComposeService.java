package com.mes.application.command.typesetting.nesting;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

@Service
public class CaifuOpenBackA30ProNoFilmNestingComposeService extends CaifuOpenBackA30HNoFilmNestingComposeService {
    public CaifuOpenBackA30ProNoFilmNestingComposeService(OssTagUploadService ossTagUploadService) {
        super(ossTagUploadService);
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30PRO_NO_FILM;
    }

    @Override
    protected int markerLineLeftMm() {
        return 30;
    }
}
