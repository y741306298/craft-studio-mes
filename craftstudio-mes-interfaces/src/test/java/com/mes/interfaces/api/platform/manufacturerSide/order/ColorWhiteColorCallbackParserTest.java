package com.mes.interfaces.api.platform.manufacturerSide.order;

import org.junit.jupiter.api.Test;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ColorWhiteColorCallbackParserTest {
    private final String data = "{\"downloadUrl\":\"https://images.example/cwc.tif?key=a%2Bb&v=1\","
            + "\"callbackCustomValue\":{\"id\":\"2111823835378413569\",\"preprocessRequestId\":\"OPR123\"}}";

    @Test
    void parsesJsonAndPreservesUrlAndRequestIdentity() {
        var response = ColorWhiteColorCallbackParser.parse("{\"code\":200,\"message\":\"success\",\"data\":" + data + "}");
        assertEquals(200, response.getCode());
        assertEquals("OPR123", response.getData().getCallbackCustomValue().getPreprocessRequestId());
    }

    @Test
    void parsesFormWithNestedDataAndDecodesOnlyOnce() {
        var response = ColorWhiteColorCallbackParser.parse("code=200&message=success&data="
                + URLEncoder.encode(data, StandardCharsets.UTF_8));
        assertEquals(200, response.getCode());
        assertEquals("2111823835378413569", response.getData().getCallbackCustomValue().getId());
        assertEquals("https://images.example/cwc.tif?key=a%2Bb&v=1", response.getData().getDownloadUrl());
    }

    @Test
    void parsesSingleQuotedNestedDataFromPythonFormSubmission() {
        String data = "{'downloadUrl':'https://images.example/cwc.tif',"
                + "'callbackCustomValue':{'id':'item-1','preprocessRequestId':'OPR123'}}";
        var response = ColorWhiteColorCallbackParser.parse("code=200&message=success&data="
                + URLEncoder.encode(data, StandardCharsets.UTF_8));
        assertEquals("item-1", response.getData().getCallbackCustomValue().getId());
    }

    @Test
    void rejectsEmptyMissingAndRepeatedFields() {
        assertThrows(IllegalArgumentException.class, () -> ColorWhiteColorCallbackParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> ColorWhiteColorCallbackParser.parse("code=200"));
        assertThrows(IllegalArgumentException.class, () -> ColorWhiteColorCallbackParser.parse("code=200&code=500"));
    }
}
