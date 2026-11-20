package com.mes.application.command.typesetting;

import com.mes.application.command.graphics.ImgUtilClient;
import com.mes.application.command.graphics.RemoteGraphics;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MirrorNestingSvgTest {
    private static final String SOURCE_URL = "https://example.com/mirror.svg";
    private static final String STORED_URL = "https://example.com/cleaned.svg";
    private static final String SVG_OPEN = "<svg xmlns='http://www.w3.org/2000/svg'>";
    private static final String ORIGINAL = "<g id='sublimation-original-piece' style='fill:var(--ink)'><rect/></g>";
    private static final String SPLICE_MARK = "<g id='SUPER-WIDTH-SPLICE-piece'><rect/></g>";
    private static final String SUBLIMATION_MARK = "<g id='sublimation-mark-piece'><rect/></g>";

    private final AppTypesettingService service = new AppTypesettingService();
    private final RestTemplate downloads = mock(RestTemplate.class);
    private final ImgUtilClient imgUtil = mock(ImgUtilClient.class);
    private final TypesettingInfo info = new TypesettingInfo();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "restTemplate", downloads);
        RemoteGraphics.enter(imgUtil, "workflow");
        info.setTypesettingId("layout");
        info.setManufacturerMetaId("factory");
    }

    @AfterEach
    void clearScope() {
        RemoteGraphics.exit();
    }

    @Test
    void sendsFullSvgAndSublimationFlagToImgUtil() {
        String source = SVG_OPEN + ORIGINAL + SPLICE_MARK + SUBLIMATION_MARK + "</svg>";
        download(source);
        info.setProcessingFlow("热升华");
        when(imgUtil.cleanNestedMirrorSvg("workflow", SOURCE_URL, source, true)).thenReturn(STORED_URL);

        assertEquals(STORED_URL, clean());
        verify(imgUtil).cleanNestedMirrorSvg("workflow", SOURCE_URL, source, true);
    }

    @Test
    void remoteFailureUsesOriginalUrlForOrdinaryLayoutAndFailsSublimation() {
        String source = SVG_OPEN + ORIGINAL + SPLICE_MARK + "</svg>";
        download(source);
        var cause = new IllegalStateException("graphics failed");
        when(imgUtil.cleanNestedMirrorSvg(eq("workflow"), eq(SOURCE_URL), eq(source), anyBoolean())).thenThrow(cause);

        assertEquals(SOURCE_URL, clean());
        info.setProcessingFlow("热升华");
        assertSame(cause, assertThrows(IllegalStateException.class, this::clean).getCause());
    }

    @Test
    void downloadFailureUsesOriginalUrlForOrdinaryLayoutAndFailsSublimation() {
        var cause = new RestClientException("download failed");
        when(downloads.getForObject(URI.create(SOURCE_URL), byte[].class)).thenThrow(cause);

        assertEquals(SOURCE_URL, clean());
        info.setProcessingFlow("热升华");
        assertSame(cause, assertThrows(IllegalStateException.class, this::clean).getCause());
        verifyNoInteractions(imgUtil);
    }

    private void download(String svg) {
        when(downloads.getForObject(URI.create(SOURCE_URL), byte[].class))
                .thenReturn(svg.getBytes(StandardCharsets.UTF_8));
    }

    private String clean() {
        return ReflectionTestUtils.invokeMethod(service, "removeSpliceMarksFromNestedMirrorSvg", SOURCE_URL, info);
    }
}
