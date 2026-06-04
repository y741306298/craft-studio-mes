package com.mes.application.command.typesetting.proces.buckle;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FourCornerBuckleProcessServiceTest {

    @Test
    void buildMarksSvgUsesPathBucklePointsInsteadOfImageElements() throws Exception {
        FourCornerBuckleProcessService service = new FourCornerBuckleProcessService(null, null);
        Method method = FourCornerBuckleProcessService.class.getDeclaredMethod("buildMarksSvg", String.class, double.class, double.class);
        method.setAccessible(true);

        String marksSvg = (String) method.invoke(service, "piece-1", 100D, 200D);

        assertEquals(4, countOccurrences(marksSvg, "<path "));
        assertFalse(marksSvg.contains("<image"));
        assertTrue(marksSvg.contains("id=\"four-corner-buckle-point-lt-piece-1\""));
        assertTrue(marksSvg.contains("transform=\"translate(21 21)\""));
        assertTrue(marksSvg.contains("transform=\"translate(71 171)\""));
    }

    private int countOccurrences(String value, String target) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }
}
