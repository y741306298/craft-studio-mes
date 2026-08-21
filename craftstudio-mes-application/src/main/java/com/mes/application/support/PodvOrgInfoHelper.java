package com.mes.application.support;

import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** podv 平台下单企业信息转换工具。 */
@Component
public class PodvOrgInfoHelper {

    private static final String PODV_PLATFORM_CODE = "podv";
    private static final String PODV_ORG_NAME = "华物POD供应链";
    private final boolean usePodSupplyChainName;

    public PodvOrgInfoHelper(
            @Value("${mes.podv-org-info.use-pod-supply-chain-name:false}") boolean usePodSupplyChainName) {
        this.usePodSupplyChainName = usePodSupplyChainName;
    }

    public OrgInfo normalize(String platformCode, OrgInfo source) {
        if (!usePodSupplyChainName || !PODV_PLATFORM_CODE.equals(platformCode) || source == null) {
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
