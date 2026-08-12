package com.mes.application.command.typesetting.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaifuOpenBackA30HNoFilmLayoutBuildServiceTest {

    @Test
    void shouldPlaceBottomBloodMarksEightMillimetersAboveContentBottom() {
        double expandedHeight = 503D;
        double lineHeight = 5D;

        double bottomLineY = CaifuOpenBackA30HNoFilmLayoutBuildService
                .calculateBottomLineY(expandedHeight, lineHeight);

        assertEquals(490D, bottomLineY);
        assertEquals(8D, expandedHeight - bottomLineY - lineHeight);
    }
}
