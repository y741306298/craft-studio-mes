package com.mes.application.command.typesetting.proces.splice;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SuperWidthSpliceProcessServiceTest {

    private static final String EXCLUDED_MANUFACTURER_META_ID = "6a26a93758a9abfcdc66d93c";

    @Test
    void skipsMarksForExcludedManufacturerAndMaterials() {
        assertThat(SuperWidthSpliceProcessService.shouldSkipMarks(
                orderItem(EXCLUDED_MANUFACTURER_META_ID, "超透PVC膜"))).isTrue();
        assertThat(SuperWidthSpliceProcessService.shouldSkipMarks(
                orderItem(EXCLUDED_MANUFACTURER_META_ID, "静电膜(透明uv)"))).isTrue();
        assertThat(SuperWidthSpliceProcessService.shouldSkipMarks(
                orderItem(EXCLUDED_MANUFACTURER_META_ID, "磨砂膜(uv)"))).isTrue();
        assertThat(SuperWidthSpliceProcessService.shouldSkipMarks(
                orderItem(EXCLUDED_MANUFACTURER_META_ID, "透明膜(uv)"))).isTrue();
    }

    @Test
    void keepsMarksWhenOnlyManufacturerOrMaterialMatches() {
        assertThat(SuperWidthSpliceProcessService.shouldSkipMarks(
                orderItem("another-manufacturer", "超透PVC膜"))).isFalse();
        assertThat(SuperWidthSpliceProcessService.shouldSkipMarks(
                orderItem(EXCLUDED_MANUFACTURER_META_ID, "其他材料"))).isFalse();
    }

    @Test
    void keepsMarksWhenMaterialInformationIsMissing() {
        OrderItem withoutMaterial = new OrderItem();
        withoutMaterial.setManufacturerId(EXCLUDED_MANUFACTURER_META_ID);

        assertThat(SuperWidthSpliceProcessService.shouldSkipMarks(null)).isFalse();
        assertThat(SuperWidthSpliceProcessService.shouldSkipMarks(withoutMaterial)).isFalse();
    }

    private OrderItem orderItem(String manufacturerMetaId, String materialName) {
        MaterialConfig material = mock(MaterialConfig.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(material.getMaterialSnapshot().getName()).thenReturn(materialName);
        OrderItem orderItem = new OrderItem();
        orderItem.setManufacturerId(manufacturerMetaId);
        orderItem.setMaterial(material);
        return orderItem;
    }
}
