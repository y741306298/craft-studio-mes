package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppTypesettingServiceCallbackSourceCellTest {

    @Test
    void singleResultUsesCompleteCachedSourcesInsteadOfPartialSvgMatches() {
        AppTypesettingService service = new AppTypesettingService();
        LayoutConfirmRequest request = new LayoutConfirmRequest();
        request.setTypesettingCells(List.of(
                requestCell("source-layout-1"),
                requestCell("source-layout-2")
        ));

        List<TypesettingSourceCell> result = ReflectionTestUtils.invokeMethod(service,
                "resolveSingleResultSourceCells", List.of(sourceCell("source-layout-1")), 1, request,
                Collections.emptyList(), "LAYOUT-NEW");

        assertThat(result)
                .extracting(TypesettingSourceCell::getSourceId)
                .containsExactly("source-layout-1", "source-layout-2");
        assertThat(result)
                .extracting(TypesettingSourceCell::getQuantity)
                .containsExactly(1, 1);
    }

    @Test
    void multipleResultsDoNotGuessSourceDistribution() {
        AppTypesettingService service = new AppTypesettingService();
        LayoutConfirmRequest request = new LayoutConfirmRequest();
        request.setTypesettingCells(List.of(requestCell("source-layout-1")));

        List<TypesettingSourceCell> result = ReflectionTestUtils.invokeMethod(service,
                "resolveSingleResultSourceCells", Collections.emptyList(), 2, request,
                Collections.emptyList(), "LAYOUT-NEW");

        assertThat(result).isEmpty();
    }

    private TypesettingProductionPieceVO requestCell(String sourceId) {
        TypesettingProductionPieceVO cell = new TypesettingProductionPieceVO();
        cell.setSourceType(TypesettingSourceType.TYPESETTING.getCode());
        cell.setSourceId(sourceId);
        cell.setQuantity(1);
        return cell;
    }

    private TypesettingSourceCell sourceCell(String sourceId) {
        TypesettingSourceCell cell = new TypesettingSourceCell();
        cell.setSourceType(TypesettingSourceType.TYPESETTING.getCode());
        cell.setSourceId(sourceId);
        cell.setQuantity(1);
        return cell;
    }
}
