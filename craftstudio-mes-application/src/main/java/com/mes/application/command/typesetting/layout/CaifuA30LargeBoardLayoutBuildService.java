package com.mes.application.command.typesetting.layout;

import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

@Service
public class CaifuA30LargeBoardLayoutBuildService extends CaifuLayoutBuildService {
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A30_LARGE_BOARD;
    }
}
