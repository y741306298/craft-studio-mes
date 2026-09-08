package com.mes.application.command.typesetting.layout;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QrLayoutNoFilmLabelTest {

    @Test
    void includesNoFilmInElementFExtraInformationForAllQrLayouts() {
        TypesettingInfo info = noFilmTypesettingInfo();
        List<Object> layoutServices = List.of(
                new CircleQrLayoutBuildService(null, null),
                new CrossQrLayoutBuildService(null, null),
                new GridCircleQrLayoutBuildService(null, null),
                new GridCrossQrLayoutBuildService(null, null),
                new RectCircleQrLayoutBuildService(null, null),
                new RectCrossQrLayoutBuildService(null, null));

        for (Object layoutService : layoutServices) {
            List<String> extInfos = ReflectionTestUtils.invokeMethod(
                    layoutService, "buildElementAExtInfos", info);
            assertThat(extInfos)
                    .as(layoutService.getClass().getSimpleName())
                    .contains("不覆膜");
        }
    }

    private TypesettingInfo noFilmTypesettingInfo() {
        ProcedureFlowNode noFilm = new ProcedureFlowNode();
        noFilm.setNodeName("不覆膜");
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(noFilm));
        TypesettingInfo info = new TypesettingInfo();
        info.setProcedureFlow(flow);
        return info;
    }
}
