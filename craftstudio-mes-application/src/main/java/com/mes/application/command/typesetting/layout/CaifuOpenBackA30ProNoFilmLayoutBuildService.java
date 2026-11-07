package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A30pro 不覆膜：复用 A30H 的 D / 非出血黑线，B 改为双侧 126mm 标记。 */
@Service
public class CaifuOpenBackA30ProNoFilmLayoutBuildService extends CaifuOpenBackA30HNoFilmLayoutBuildService {
    private static final int EXPAND_RIGHT_MM = 3;
    private static final int ELEMENT_B_OFFSET_Y_MM = 126;
    private static final int ELEMENT_B_LEFT_X_MM = 3;

    public CaifuOpenBackA30ProNoFilmLayoutBuildService(OssTagUploadService ossTagUploadService,
                                                      TypesettingService typesettingService) {
        super(ossTagUploadService, typesettingService);
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30PRO_NO_FILM;
    }

    @Override
    protected int expandRightMm() {
        return EXPAND_RIGHT_MM;
    }

    @Override
    protected int openBackTagHeightMm() {
        return 0;
    }

    @Override
    protected void addElementBMarks(FormeBuildContext context,
                                    List<FormeGenerationRequest.Mark> marks,
                                    String elementB,
                                    int expandedHeight,
                                    int contentOffsetY,
                                    List<MarkerBand> orderedBands) {
        int rightX = EXPAND_LEFT_MM + context.getNestedWidth().intValue() + EXPAND_RIGHT_MM;
        Set<Integer> placedYs = new LinkedHashSet<>();
        for (MarkerBand band : orderedBands) {
            if (band == null) {
                continue;
            }
            // 与 A30H 共用已解析的辅助条中线、顶部和底部参考，不读取 gridLines。
            double markY = band.getCenterY() - ELEMENT_B_OFFSET_Y_MM;
            int roundedY = (int) Math.round(markY);
            if (markY < 0 || markY + ELEMENT_B_HEIGHT_MM > expandedHeight
                    || roundedY + ELEMENT_B_HEIGHT_MM > expandedHeight || !placedYs.add(roundedY)) {
                continue;
            }
            marks.add(createMark(elementB, ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM, ELEMENT_B_LEFT_X_MM, roundedY));
            marks.add(createMark(elementB, ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM, rightX, roundedY));
        }
    }
}
