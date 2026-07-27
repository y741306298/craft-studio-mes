package com.mes.application.command.typesetting.strategy;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.util.ProcedureFlowNodeMatcher;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 双面对裱镜像印版策略：
 * 仅当排版元素全部为零件、存在“双面对裱”或“覆双面”节点，且 nestedMirrorSvg 存在时触发。
 */
@Service
public class DoubleSideMountingMirrorFormeStrategy implements MirrorFormeStrategy {
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
        restoreMissingProcedureFlowNodes(origin, mirror);
        mirror.setId(null);
        mirror.setTypesettingId(buildMirrorTypesettingId(origin));
        mirror.setLayoutMode(TypesettingLayoutMode.DOUBLE_SIDE_MOUNTING_LAYOUT.getCode());
        mirror.getElement().setNestedSvg(origin.getElement().getNestedMirrorSvg());
        preserveOriginalMaterial(mirror, origin.getMaterialConfig());
        MaterialConfig mirrorMaterialConfig = buildMirrorMaterialConfig(origin.getProcedureFlow(), origin.getMaterialConfig());
        if (mirrorMaterialConfig != null) {
            mirror.setMaterialConfig(mirrorMaterialConfig);
            Object materialId = getFieldValue(mirrorMaterialConfig, "materialId");
            if (materialId != null && StringUtils.isNotBlank(String.valueOf(materialId))) {
                mirror.setMaterialConfigs(Collections.singletonList(String.valueOf(materialId)));
            }
        }
        return mirror;
    }


    private void restoreMissingProcedureFlowNodes(TypesettingInfo origin, TypesettingInfo mirror) {
        if (origin == null || origin.getProcedureFlow() == null || origin.getProcedureFlow().getNodes() == null
                || mirror == null || mirror.getProcedureFlow() == null || mirror.getProcedureFlow().getNodes() == null) {
            return;
        }
        List<ProcedureFlowNode> originNodes = origin.getProcedureFlow().getNodes();
        List<ProcedureFlowNode> mirrorNodes = mirror.getProcedureFlow().getNodes();
        int nodeCount = Math.min(originNodes.size(), mirrorNodes.size());
        for (int i = 0; i < nodeCount; i++) {
            ProcedureFlowNode mirrorNode = mirrorNodes.get(i);
            if (mirrorNode != null && StringUtils.isNotBlank(mirrorNode.getNodeName())) {
                continue;
            }
            ProcedureFlowNode originNode = originNodes.get(i);
            if (originNode != null) {
                mirrorNodes.set(i, copyProcedureFlowNode(originNode));
            }
        }
        mirror.getProcedureFlow().setTotalNodes(mirrorNodes.size());
    }

    private ProcedureFlowNode copyProcedureFlowNode(ProcedureFlowNode source) {
        ProcedureFlowNode target = new ProcedureFlowNode();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private String buildMirrorTypesettingId(TypesettingInfo origin) {
        String baseId = StringUtils.isNotBlank(origin.getTypesettingId()) ? origin.getTypesettingId() : origin.getId();
        return baseId + "-Mirror";
    }

    private MaterialConfig buildMirrorMaterialConfig(ProcedureFlow procedureFlow, MaterialConfig originMaterialConfig) {
        ProcedureFlowNode node = findDoubleSideMountingNode(procedureFlow);
        if (node == null || node.getParamConfigs() == null || node.getParamConfigs().isEmpty()) {
            return null;
        }
        Object param = getFieldValue(node.getParamConfigs().get(0), "param");
        if (param == null) {
            return null;
        }
        Object type = getFieldValue(param, "type");
        Object accessorySnapshot = getFieldValue(param, "accessorySnapshot");
        Object accessoryName = getFieldValue(accessorySnapshot, "name");
        if (type == null && accessoryName == null) {
            return null;
        }

        // 镜像印版仍归属正面材料：保留原 materialId，只用配件名称覆盖展示名称。
        // 配件工艺继续写入 materialType，供后续工艺处理使用。
        MaterialConfig materialConfig = originMaterialConfig == null
                ? new MaterialConfig()
                : JSON.parseObject(JSON.toJSONString(originMaterialConfig), MaterialConfig.class);
        preserveOriginalMaterial(materialConfig, originMaterialConfig);
        setFieldValue(materialConfig, "materialType", type);
        if (accessoryName != null) {
            setMaterialSnapshotName(materialConfig, accessoryName);
        }
        Object usageSize3D = getFieldValue(originMaterialConfig, "usageSize3D");
        if (usageSize3D != null) {
            setFieldValue(materialConfig, "usageSize3D", usageSize3D);
        }
        return materialConfig;
    }

    private void preserveOriginalMaterial(MaterialConfig mirrorMaterialConfig, MaterialConfig originMaterialConfig) {
        if (mirrorMaterialConfig == null || originMaterialConfig == null) {
            return;
        }
        setFieldValue(mirrorMaterialConfig, "oriName", getFieldValue(getFieldValue(originMaterialConfig, "materialSnapshot"), "name"));
        setFieldValue(mirrorMaterialConfig, "oriMaterialId", getFieldValue(originMaterialConfig, "materialId"));
        setFieldValue(mirrorMaterialConfig, "oriMaterialType", getFieldValue(originMaterialConfig, "materialType"));
    }

    private void preserveOriginalMaterial(TypesettingInfo mirror, MaterialConfig originMaterialConfig) {
        if (mirror == null || originMaterialConfig == null) {
            return;
        }
        mirror.setOriName(toStringOrNull(getFieldValue(getFieldValue(originMaterialConfig, "materialSnapshot"), "name")));
        mirror.setOriMaterialId(toStringOrNull(getFieldValue(originMaterialConfig, "materialId")));
        mirror.setOriMaterialType(toStringOrNull(getFieldValue(originMaterialConfig, "materialType")));
    }

    private String toStringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private ProcedureFlowNode findDoubleSideMountingNode(ProcedureFlow procedureFlow) {
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            return null;
        }
        return procedureFlow.getNodes().stream()
                .filter(node -> node != null && StringUtils.isNotBlank(node.getNodeName()))
                .filter(node -> ProcedureFlowNodeMatcher.isDoubleSideMountingNodeName(node.getNodeName()))
                .findFirst()
                .orElse(null);
    }

    private void setMaterialSnapshotName(MaterialConfig materialConfig, Object name) {
        for (Class<?> nestedClass : MaterialConfig.class.getDeclaredClasses()) {
            if (!"MaterialSnapshot".equals(nestedClass.getSimpleName())) {
                continue;
            }
            try {
                Object snapshot = nestedClass.getDeclaredConstructor().newInstance();
                setFieldValue(snapshot, "name", name);
                Method setter = MaterialConfig.class.getMethod("setMaterialSnapshot", nestedClass);
                setter.invoke(materialConfig, snapshot);
            } catch (Exception ignored) {
                // ignore invalid snapshot shape
            }
            return;
        }
    }

    private Object getFieldValue(Object target, String fieldName) {
        if (target == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        if (target instanceof Map<?, ?> map) {
            return map.get(fieldName);
        }
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            return target.getClass().getMethod(getterName).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        if (target == null || StringUtils.isBlank(fieldName) || value == null) {
            return;
        }
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        for (Method method : target.getClass().getMethods()) {
            if (!setterName.equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            Object convertedValue = convertValue(value, method.getParameterTypes()[0]);
            if (convertedValue == null) {
                continue;
            }
            try {
                method.invoke(target, convertedValue);
                return;
            } catch (Exception ignored) {
                // try the next compatible setter
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (String.class.equals(targetType)) {
            return String.valueOf(value);
        }
        if (targetType.isEnum()) {
            try {
                return Enum.valueOf((Class<Enum>) targetType, String.valueOf(value));
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean allCellsAreProductionPieces(TypesettingInfo info) {
        if (info == null || info.getTypesettingCells() == null || info.getTypesettingCells().isEmpty()) {
            return false;
        }
        return info.getTypesettingCells().stream()
                .allMatch(cell -> cell != null && TypesettingSourceType.PART.getCode().equals(cell.getSourceType()));
    }

    private boolean hasDoubleSideMounting(TypesettingInfo info) {
        return info != null && ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(info.getProcedureFlow());
    }
}
