package com.mes.application.command.typesetting.strategy;

import com.alibaba.fastjson.JSON;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 双面对裱镜像印版策略：
 * 仅当存在“双面对裱”节点、节点参数类型为 ACC 且 nestedMirrorSvg 存在时触发。
 */
@Service
public class DoubleSideMountingMirrorFormeStrategy implements MirrorFormeStrategy {
    private static final String DOUBLE_SIDE_NODE_NAME = "双面对裱";
    private static final String PARAM_TYPE_ACC = "ACC";

    @Override
    public boolean supports(TypesettingInfo info) {
        return hasDoubleSideMounting(info) && doubleSideNodeAcc(info);
    }

    @Override
    public TypesettingInfo buildMirrorTypesettingInfo(TypesettingInfo origin) {
        if (!supports(origin) || origin == null || origin.getElement() == null) {
            return null;
        }
        if (StringUtils.isBlank(origin.getElement().getNestedMirrorSvg())) {
            return null;
        }
        TypesettingInfo mirror = JSON.parseObject(JSON.toJSONString(origin), TypesettingInfo.class);
        mirror.setId(null);
        mirror.setTypesettingId(origin.getTypesettingId() + "-Mirror");
        mirror.setLayoutMode(TypesettingLayoutMode.DOUBLE_SIDE_MOUNTING_LAYOUT.getCode());
        mirror.getElement().setNestedSvg(origin.getElement().getNestedMirrorSvg());
        return mirror;
    }

    private boolean hasDoubleSideMounting(TypesettingInfo info) {
        if (info == null || info.getProcedureFlow() == null || info.getProcedureFlow().getNodes() == null) {
            return false;
        }
        return info.getProcedureFlow().getNodes().stream().anyMatch(n -> n != null && DOUBLE_SIDE_NODE_NAME.equals(n.getNodeName()));
    }

    private boolean doubleSideNodeAcc(TypesettingInfo info) {
        return info.getProcedureFlow().getNodes().stream()
                .filter(n -> n != null && DOUBLE_SIDE_NODE_NAME.equals(n.getNodeName()))
                .flatMap(n -> n.getParamConfigs() == null ? java.util.stream.Stream.empty() : n.getParamConfigs().stream())
                .map(cfg -> invokeGetter(cfg, "getParam"))
                .map(param -> param instanceof Map ? ((Map<?, ?>) param).get("type") : invokeGetter(param, "getType"))
                .anyMatch(type -> type != null && PARAM_TYPE_ACC.equalsIgnoreCase(type.toString()));
    }

    private Object invokeGetter(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignore) {
            return null;
        }
    }
}
