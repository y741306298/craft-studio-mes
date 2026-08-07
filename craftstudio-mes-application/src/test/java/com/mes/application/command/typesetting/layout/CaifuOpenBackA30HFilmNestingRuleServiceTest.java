package com.mes.application.command.typesetting.layout;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaifuOpenBackA30HFilmNestingRuleServiceTest {

    private final CaifuOpenBackA30HFilmNestingRuleService service =
            new CaifuOpenBackA30HFilmNestingRuleService();

    @Test
    void arrangeElementSourcesGroupsPartsByOrderItemIdAndPlatesByTypesettingId() {
        List<ProductionPiece> pieces = new ArrayList<>(List.of(
                piece("piece-1", "order-1"),
                piece("piece-2", "order-2"),
                piece("piece-3", "order-1"),
                piece("piece-4", "order-2")));
        List<TypesettingInfo> typesettings = new ArrayList<>(List.of(
                typesetting("plate-1", "typesetting-1"),
                typesetting("plate-2", "typesetting-2"),
                typesetting("plate-3", "typesetting-1"),
                typesetting("plate-4", "typesetting-1")));

        service.arrangeElementSources(pieces, typesettings);

        assertThat(pieces).extracting(ProductionPiece::getProductionPieceId)
                .containsExactly("piece-1", "piece-3", "piece-2", "piece-4");
        assertThat(typesettings).extracting(TypesettingInfo::getId)
                .containsExactly("plate-1", "plate-3", "plate-4", "plate-2");
    }

    private ProductionPiece piece(String id, String orderItemId) {
        ProductionPiece piece = new ProductionPiece();
        piece.setProductionPieceId(id);
        piece.setOrderItemId(orderItemId);
        return piece;
    }

    private TypesettingInfo typesetting(String id, String typesettingId) {
        TypesettingInfo info = new TypesettingInfo();
        info.setId(id);
        info.setTypesettingId(typesettingId);
        return info;
    }
}
