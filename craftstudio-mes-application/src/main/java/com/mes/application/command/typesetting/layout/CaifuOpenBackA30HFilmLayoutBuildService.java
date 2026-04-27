package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class CaifuOpenBackA30HFilmLayoutBuildService extends CaifuLayoutBuildService {
    private static final int EXPAND_TOP_MM = 3;
    private static final int EXPAND_RIGHT_MM = 11;

    private static final int ELEMENT_A_WIDTH_MM = 3;
    private static final int ELEMENT_B_WIDTH_MM = 8;
    private static final int ELEMENT_B_HEIGHT_MM = 3;
    private static final int ELEMENT_A_OFFSET_Y_MM = 295;

    public CaifuOpenBackA30HFilmLayoutBuildService(OssTagUploadService ossTagUploadService) {
        super(ossTagUploadService);
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_FILM;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        int originalWidth = context.getNestedWidth().intValue();
        int originalHeight = context.getNestedHeight().intValue();
        int expandedHeight = originalHeight + EXPAND_TOP_MM;

        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(0);
        margin.setTop(EXPAND_TOP_MM);
        margin.setRight(EXPAND_RIGHT_MM);
        margin.setBottom(0);
        result.setMargin(margin);

        String tagUploadSubDir = buildTagUploadSubDir(context);
        String elementA = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(ELEMENT_A_WIDTH_MM, expandedHeight),
                tagUploadSubDir
        );
        String elementB = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM),
                tagUploadSubDir
        );

        LinkedHashSet<Double> ys = new LinkedHashSet<>();
        ys.add(0D);
        TypesettingElement.GridLines gridLines = context.getTypesettingInfo() != null
                && context.getTypesettingInfo().getElement() != null
                ? context.getTypesettingInfo().getElement().getGridLines()
                : null;
        if (gridLines != null && gridLines.getYs() != null) {
            ys.addAll(gridLines.getYs());
        }

        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();
        for (Double y : ys) {
            if (y == null) {
                continue;
            }
            int elementAY = (int) Math.round(y + ELEMENT_A_OFFSET_Y_MM);
            if (elementAY > expandedHeight) {
                continue;
            }
            marks.add(createMark(elementA, ELEMENT_A_WIDTH_MM, expandedHeight, originalWidth, elementAY));
        }
        marks.add(createMark(elementB, ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM, originalWidth + ELEMENT_B_WIDTH_MM, 0));

        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> markFiles = new LinkedHashMap<>();
            markFiles.put("elementA", elementA);
            markFiles.put("elementB", elementB);
            context.getTypesettingInfo().setMarks(markFiles);
        }

        result.setMarks(marks);
        result.setAnchorPoints(Collections.emptyList());
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }
}
