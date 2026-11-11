package com.mes.application.command.orderPreprocessing.sublimation;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SublimationRulesTest {
    @Test
    void sameBackNeedsOnlyImageNode() {
        SublimationPlan plan = resolve(step("反面相同画面", 3));
        assertTrue(plan.doubleSided());
        assertEquals(SublimationPlan.BackImageMode.SAME, plan.backImageMode());
        assertEquals(SublimationGeometry.Edge.TOP, plan.sleeveEdge());
    }

    @Test
    void backImageReplacementPreservesSleeveColorFromLatestMain() {
        SublimationPlan plan = resolve(step("反面相同画面", 3));
        assertEquals("#ffffff", plan.withBackImageUrl("front.tif").scrollColorHex());
    }

    @Test
    void backImageWorksWithoutSleeveAndWithFourEdgeSewing() {
        SublimationPlan plan = SublimationRules.resolve(List.of(step("热升华", 1),
                step("单针锁边", 2), step("反面相同画面", 3)), false);
        assertTrue(plan.doubleSided());
        assertNull(plan.sleeveEdge());
        assertEquals(0, plan.sleeveWidthMm());
        assertEquals(20, plan.sewWidthMm());
        assertEquals(plan.margins(), plan.withBackImageUrl("front.tif").margins());
    }

    @Test
    void differentBackPrefersRawFileWithoutSewingMode() {
        SublimationPlan plan = resolve(step("反面不同画面", 3,
                "{'file':{'rawFile':'back.tif','filePreview':{'raw':'preview.tif'}}}"));
        assertTrue(plan.doubleSided());
        assertEquals(SublimationPlan.BackImageMode.DIFFERENT, plan.backImageMode());
        assertEquals("back.tif", plan.backImageUrl());
    }

    @Test
    void differentBackFallsBackToPreviewRaw() {
        SublimationPlan plan = resolve(step("反面不同画面", 3,
                "{'file':{'rawFile':' ','filePreview':{'raw':'preview.tif'}}}"));
        assertEquals("preview.tif", plan.backImageUrl());
    }

    @Test
    void sewingModesDoNotRequireOrCreateBackImage() {
        assertFalse(resolve().doubleSided());
        assertFalse(resolve(step("双面缝", 3)).doubleSided());
        assertFalse(resolve(step("双面夹黑缝", 3)).doubleSided());
        assertEquals(SublimationPlan.BackImageMode.SAME,
                resolve(step("双面缝", 3), step("双面夹黑缝", 4), step("反面相同画面", 5)).backImageMode());
    }

    @Test
    void rejectsConflictingBackNodesEvenWithoutSewingMode() {
        assertThrows(IllegalArgumentException.class,
                () -> resolve(step("反面相同画面", 3), step("反面不同画面", 4, "{'file':{'rawFile':'back.tif'}}")));
    }

    @Test
    void rejectsMissingBackFileEvenWithoutSewingMode() {
        assertThrows(IllegalArgumentException.class, () -> resolve(step("反面不同画面", 3)));
    }

    @Test
    void onlyExactNamesAfterSublimationCountInSortedFlow() {
        assertFalse(resolve(step("反面相同画面", 0)).doubleSided());
        assertFalse(resolve(step("反面相同画面（备注）", 3)).doubleSided());
        assertTrue(resolve(step("反面相同画面", 3)).doubleSided());
    }

    @Test
    void backNodesParticipateInStrategySelectionAlongsideLegacyRectangle() {
        List<SublimationRules.Step> old = List.of(step("热升华", 1), step("矩形旗缝套筒", 2));
        assertFalse(SublimationRules.applies(old));
        List<SublimationRules.Step> withBack = new ArrayList<>(old);
        withBack.add(step("反面相同画面", 3));
        assertTrue(SublimationRules.applies(withBack));
    }

    private static SublimationPlan resolve(SublimationRules.Step... extra) {
        List<SublimationRules.Step> flow = new ArrayList<>();
        flow.addAll(Arrays.asList(extra)); // 刻意打乱输入顺序，验证 nodeOrder 排序。
        flow.add(step("缝布带管套", 2, "{'type':'RECT_EDGE_SELECTION','edges':['TOP']}"));
        flow.add(step("热升华（单面布）", 1));
        return SublimationRules.resolve(flow, false);
    }

    private static SublimationRules.Step step(String name, int order, String... params) {
        return new SublimationRules.Step(name, order, Arrays.stream(params).map(JSONObject::parseObject).toList());
    }
}
