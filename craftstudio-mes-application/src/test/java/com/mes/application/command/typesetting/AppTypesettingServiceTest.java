package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppTypesettingServiceTest {

    @Test
    void typesettingPiecesPutUrgentItemsFirstThenSortByCreateTime() {
        TypesettingProductionPieceVO normalOlder = piece(false, 1000L);
        TypesettingProductionPieceVO urgentNewer = piece(true, 4000L);
        TypesettingProductionPieceVO urgentOlder = piece(true, 3000L);
        TypesettingProductionPieceVO normalNewer = piece(null, 2000L);
        List<TypesettingProductionPieceVO> items = new ArrayList<>(
                List.of(normalOlder, urgentNewer, urgentOlder, normalNewer));

        ReflectionTestUtils.invokeMethod(new AppTypesettingService(),
                "sortTypesettingProductionPiecesByCreateTime", items);

        assertThat(items).containsExactly(urgentOlder, urgentNewer, normalOlder, normalNewer);
    }

    private TypesettingProductionPieceVO piece(Boolean isUrgent, long createTime) {
        TypesettingProductionPieceVO piece = new TypesettingProductionPieceVO();
        piece.setIsUrgent(isUrgent);
        piece.setCreateTime(new Date(createTime));
        return piece;
    }
}
