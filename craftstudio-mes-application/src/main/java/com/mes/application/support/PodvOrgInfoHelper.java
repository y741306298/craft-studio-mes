package com.mes.application.support;

import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import io.micrometer.common.util.StringUtils;

/** podv 平台下单企业信息转换工具。 */
public final class PodvOrgInfoHelper {

    private static final String PODV_PLATFORM_CODE = "podv";
    private static final String PODV_ORG_NAME = "pod1688";

    private PodvOrgInfoHelper() {
    }

    public static OrgInfo normalize(String platformCode, OrgInfo source) {
        if (!PODV_PLATFORM_CODE.equals(platformCode) || source == null) {
            return source;
        }

        OrgInfo normalized = new OrgInfo();
        normalized.setOriName(StringUtils.isNotBlank(source.getOriName())
                ? source.getOriName()
                : source.getName());
        normalized.setName(PODV_ORG_NAME);
        return normalized;
    }
}
