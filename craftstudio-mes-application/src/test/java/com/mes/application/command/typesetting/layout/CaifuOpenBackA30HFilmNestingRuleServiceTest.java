package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.NestingRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CaifuOpenBackA30HFilmNestingRuleServiceTest {
    private final CaifuOpenBackA30HFilmNestingRuleService service =
            new CaifuOpenBackA30HFilmNestingRuleService();

    @Test
    void shouldAddRightMarginWhenRequestHasPlateButNoBloodElement() {
        NestingRequest.Element element = new NestingRequest.Element();

        service.applyElementStyle(element, false, false, true);

        assertEquals("right", element.getHGravity());
        assertEquals(30, element.getHMargin());
        assertEquals(0, element.getVMargin());
    }

    @Test
    void shouldKeepExistingBloodLayoutMargin() {
        NestingRequest.Element element = new NestingRequest.Element();

        service.applyElementStyle(element, false, true, true);

        assertEquals("right", element.getHGravity());
        assertEquals(0, element.getHMargin());
        assertEquals(0, element.getVMargin());
    }

    @Test
    void shouldNotAddStyleWhenRequestHasNeitherPlateNorBloodElement() {
        NestingRequest.Element element = new NestingRequest.Element();

        service.applyElementStyle(element, false, false, false);

        assertNull(element.getHGravity());
        assertNull(element.getHMargin());
        assertNull(element.getVMargin());
    }
}
