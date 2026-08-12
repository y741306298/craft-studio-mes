package com.mes.application.command.typesetting.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaifuOpenBackA30HNoFilmLayoutBuildServiceTest {

    @Test
    void shouldPlaceBottomBloodMarksEightMillimetersAboveCurrentCellBottom() {
        double cellTopY = 120D;
        double cellHeight = 300D;
        double lineHeight = 5D;

        double bottomLineY = CaifuOpenBackA30HNoFilmLayoutBuildService
                .calculateBottomLineY(cellTopY, cellHeight, lineHeight);

        assertEquals(407D, bottomLineY);
        assertEquals(8D, cellTopY + cellHeight - bottomLineY - lineHeight);
    }
}
