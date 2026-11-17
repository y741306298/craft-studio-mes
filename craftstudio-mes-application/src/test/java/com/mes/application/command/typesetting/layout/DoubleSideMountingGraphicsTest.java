package com.mes.application.command.typesetting.layout;

import com.mes.application.command.graphics.*;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DoubleSideMountingGraphicsTest {
    @ParameterizedTest @ValueSource(booleans={true,false})
    void fixedArrowRemainsSeparateFromBothLabelImages(boolean onlyParts) {
        var resolver=mock(QrLayoutOrderIdResolver.class);
        var service=new DoubleSideMountingLayoutBuildService(mock(OssTagUploadService.class),resolver);
        var info=new TypesettingInfo();info.setManufacturerMetaId("factory");info.setTypesettingId("LAYOUT-Mirror");
        var cell=new TypesettingSourceCell();cell.setSourceType(onlyParts?TypesettingSourceType.PART.getCode():TypesettingSourceType.TYPESETTING.getCode());
        info.setTypesettingCells(List.of(cell));
        var context=new FormeBuildContext();context.setTypesettingInfo(info);context.setBusinessId("business");
        context.setNestedWidth(BigDecimal.valueOf(300));context.setNestedHeight(BigDecimal.valueOf(100));context.setMarginHeight(BigDecimal.valueOf(30));
        context.setElementAResolver(value->"A-001");context.setPlateNameSupplier(()->"001.plt");context.setPlateNameBBSupplier(()->"002.plt");
        var client=mock(ImgUtilClient.class);
        when(client.render(anyString(),eq("DOUBLE_TAG"),anyMap())).thenReturn(new RemoteImage("front","https://bucket/front.png",3543,354),new RemoteImage("back","https://bucket/back.png",3543,354));
        FormeLayoutBuildResult result;
        RemoteGraphics.enter(client,"workflow");
        try { result=service.build(context); } finally { RemoteGraphics.exit(); }
        var requests=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(client,times(2)).render(eq("workflow"),eq("DOUBLE_TAG"),requests.capture());
        var values=requests.getAllValues();
        assertFalse(values.get(0).containsKey("qrDataUri"));assertFalse(values.get(1).containsKey("qrDataUri"));
        assertEquals(false,values.get(0).get("rotate180"));assertEquals(true,values.get(1).get("rotate180"));
        assertEquals("LAYOUT",values.get(0).get("typesettingId"));
        assertEquals(onlyParts?3:2,result.getMarks().size());
        if(onlyParts) {
            assertTrue(result.getMarks().get(2).getImg().endsWith("/basetag/basetag-leftarrow.png"));
            assertEquals(result.getMarks().get(2).getImg(),info.getMarks().get("elementG"));
        }else assertFalse(info.getMarks().containsKey("elementG"));
        assertTrue(result.getAnchorPoints().isEmpty());verifyNoMoreInteractions(client);
    }
}
