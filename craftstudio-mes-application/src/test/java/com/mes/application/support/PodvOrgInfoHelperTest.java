package com.mes.application.support;

import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class PodvOrgInfoHelperTest {

    @Test
    void shouldKeepOriginalOrgInfoWhenSwitchIsOff() {
        OrgInfo source = orgInfo("原企业", null);

        OrgInfo result = new PodvOrgInfoHelper(false).normalize("podv", source);

        assertSame(source, result);
        assertEquals("原企业", result.getName());
    }

    @Test
    void shouldUsePodSupplyChainNameWhenSwitchIsOnForPodv() {
        OrgInfo source = orgInfo("原企业", null);

        OrgInfo result = new PodvOrgInfoHelper(true).normalize("podv", source);

        assertNotSame(source, result);
        assertEquals("华物POD供应链", result.getName());
        assertEquals("原企业", result.getOriName());
    }

    @Test
    void shouldKeepOriginalOrgInfoForOtherPlatformsWhenSwitchIsOn() {
        OrgInfo source = orgInfo("原企业", null);

        OrgInfo result = new PodvOrgInfoHelper(true).normalize("other", source);

        assertSame(source, result);
    }

    @Test
    void shouldPreserveExistingOriginalNameWhenReplacingName() {
        OrgInfo source = orgInfo("当前企业", "最初企业");

        OrgInfo result = new PodvOrgInfoHelper(true).normalize("podv", source);

        assertEquals("华物POD供应链", result.getName());
        assertEquals("最初企业", result.getOriName());
    }

    private OrgInfo orgInfo(String name, String oriName) {
        OrgInfo orgInfo = new OrgInfo();
        orgInfo.setName(name);
        orgInfo.setOriName(oriName);
        return orgInfo;
    }
}
