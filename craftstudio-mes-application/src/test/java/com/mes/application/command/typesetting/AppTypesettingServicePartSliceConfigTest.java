package com.mes.application.command.typesetting;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AppTypesettingServicePartSliceConfigTest {
    private final AppTypesettingService service = new AppTypesettingService();
    private final ProductionPieceService pieces = mock(ProductionPieceService.class);

    @Test
    void convertsCmToMmOnceUsingDatabaseIdWithoutMetadata() {
        ReflectionTestUtils.setField(service, "productionPieceService", pieces);
        ProductionPiece piece = piece("小块裁切");
        when(pieces.findByIds(Set.of("part-id"))).thenReturn(Map.of("part-id", piece));
        TypesettingInfo info = new TypesettingInfo();
        info.setTypesettingCells(List.of(cell(TypesettingSourceType.PART), cell(TypesettingSourceType.PART)));

        List<FormeGenerationRequest.PartSliceConfig> result = build(info);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPartId()).isEqualTo("part-id");
        assertThat(result.get(0).getParam().getXs())
                .usingComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .containsExactly(new java.math.BigDecimal("125"), new java.math.BigDecimal("250"),
                        new java.math.BigDecimal("375"));
        assertThat(result.get(0).getParam().getYs())
                .usingComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .containsExactly(new java.math.BigDecimal("25"));
        assertThat(JSON.toJSONString(piece.getProcedureFlow())).contains("12.5", "37.5");
        assertThat(JSON.toJSONString(build(info))).isEqualTo(JSON.toJSONString(result));
        assertThat(JSON.toJSONString(result)).doesNotContain("SLICE", "processParamMetaId", "business-id");
        verify(pieces).findByIds(Set.of("part-id"));
    }

    @Test
    void ignoresCoordinatesFromOtherProcessesAndMissingPieces() {
        ReflectionTestUtils.setField(service, "productionPieceService", pieces);
        TypesettingInfo info = new TypesettingInfo();
        info.setTypesettingCells(List.of(cell(TypesettingSourceType.PART)));
        when(pieces.findByIds(Set.of("part-id"))).thenReturn(Map.of("part-id", piece("拼接")));
        assertThat(build(info)).isEmpty();
        when(pieces.findByIds(Set.of("part-id"))).thenReturn(Map.of());
        assertThat(build(info)).isEmpty();
    }

    @Test
    void ignoresTypesettingSourcesAndEmptyCells() {
        TypesettingInfo info = new TypesettingInfo();
        assertThat(build(info)).isEmpty();
        info.setTypesettingCells(List.of(cell(TypesettingSourceType.TYPESETTING)));
        assertThat(build(info)).isEmpty();
        verifyNoInteractions(pieces);
    }

    private List<FormeGenerationRequest.PartSliceConfig> build(TypesettingInfo info) {
        return ReflectionTestUtils.invokeMethod(service, "buildPartSliceConfigs", info);
    }

    private TypesettingSourceCell cell(TypesettingSourceType type) {
        TypesettingSourceCell cell = new TypesettingSourceCell();
        cell.setSourceType(type.getCode());
        cell.setSourceId("part-id");
        return cell;
    }

    private ProductionPiece piece(String nodeName) {
        ProductionPiece piece = new ProductionPiece();
        piece.setId("part-id");
        piece.setProductionPieceId("business-id");
        piece.setProcedureFlow(JSON.parseObject("""
                {"nodes":[{"nodeName":"%s","paramConfigs":[{"param":{
                  "type":"SLICE","xs":[12.5,25,37.5],"ys":[2.5],"processParamMetaId":"meta-id"
                }}]}]}
                """.formatted(nodeName), ProcedureFlow.class));
        return piece;
    }
}
