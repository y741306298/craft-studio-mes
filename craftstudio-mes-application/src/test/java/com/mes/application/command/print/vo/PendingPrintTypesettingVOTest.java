package com.mes.application.command.print.vo;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingPrintTypesettingVOTest {

    @Test
    void fromCopiesUrgentFlag() {
        TypesettingInfo info = new TypesettingInfo();
        info.setIsUrgent(true);

        PendingPrintTypesettingVO vo = PendingPrintTypesettingVO.from(info);

        assertTrue(vo.getIsUrgent());
    }

    @Test
    void fromDefaultsMissingUrgentFlagToFalse() {
        TypesettingInfo info = new TypesettingInfo();

        PendingPrintTypesettingVO vo = PendingPrintTypesettingVO.from(info);

        assertFalse(vo.getIsUrgent());
    }
}
