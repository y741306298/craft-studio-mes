package com.mes.application.command.typesetting.strategy;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 双面对裱镜像印版策略：
 * 仅当排版元素全部为零件、存在“双面对裱”或“覆双面”节点，且 nestedMirrorSvg 存在时触发。
 */
@Service
public class DoubleSideMountingMirrorFormeStrategy implements MirrorFormeStrategy {
    private static final String DOUBLE_SIDE_NODE_NAME = "双面对裱";
    private static final String COVER_DOUBLE_SIDE_NODE_NAME = "覆双面";

    @Override
    public boolean supports(TypesettingInfo info) {
        return allCellsAreProductionPieces(info) && hasDoubleSideMounting(info);
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

    private boolean allCellsAreProductionPieces(TypesettingInfo info) {
        if (info == null || info.getTypesettingCells() == null || info.getTypesettingCells().isEmpty()) {
            return false;
        }
        return info.getTypesettingCells().stream()
                .allMatch(cell -> cell != null && TypesettingSourceType.PART.getCode().equals(cell.getSourceType()));
    }

    private boolean hasDoubleSideMounting(TypesettingInfo info) {
        if (info == null || info.getProcedureFlow() == null || info.getProcedureFlow().getNodes() == null) {
            return false;
        }
        return info.getProcedureFlow().getNodes().stream()
                .anyMatch(n -> n != null
                        && (DOUBLE_SIDE_NODE_NAME.equals(n.getNodeName())
                        || COVER_DOUBLE_SIDE_NODE_NAME.equals(n.getNodeName())));
    }
}
