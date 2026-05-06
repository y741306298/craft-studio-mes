package com.mes.application.command.print.vo;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import io.micrometer.common.util.StringUtils;
import lombok.Data;

@Data
public class PendingPrintTypesettingVO extends TypesettingInfo {

    private String jsonfile;

    public static PendingPrintTypesettingVO from(TypesettingInfo info) {
        PendingPrintTypesettingVO vo = new PendingPrintTypesettingVO();
        if (info == null) {
            return vo;
        }
        vo.setId(info.getId());
        vo.setCreateTime(info.getCreateTime());
        vo.setUpdateTime(info.getUpdateTime());
        vo.setTypesettingId(info.getTypesettingId());
        vo.setManufacturerMetaId(info.getManufacturerMetaId());
        vo.setStatus(info.getStatus());
        vo.setMaterialConfig(info.getMaterialConfig());
        vo.setMaterialConfigs(info.getMaterialConfigs());
        vo.setQuantity(info.getQuantity());
        vo.setLeaveQuantity(info.getLeaveQuantity());
        vo.setElement(info.getElement());
        vo.setProcedureFlow(info.getProcedureFlow());
        vo.setProcessingFlow(info.getProcessingFlow());
        vo.setTypesettingCells(info.getTypesettingCells());
        vo.setRemark(info.getRemark());
        vo.setDeviceCode(info.getDeviceCode());
        vo.setDeviceName(info.getDeviceName());
        vo.setMaskSvg(info.getMaskSvg());
        vo.setLayoutMode(info.getLayoutMode());
        vo.setLayoutCategory(info.getLayoutCategory());
        vo.setRequireJsonFile(info.getRequireJsonFile());
        vo.setRequirePltFile(info.getRequirePltFile());
        vo.setRequireSvgFile(info.getRequireSvgFile());
        vo.setCodeGenerateType(info.getCodeGenerateType());
        vo.setTempCodeFormat(info.getTempCodeFormat());
        vo.setAnchorPointShape(info.getAnchorPointShape());
        vo.setTemplateCode(info.getTemplateCode());
        vo.setMarks(info.getMarks());

        String elementJson = info.getElement() == null ? null : info.getElement().getJson();
        vo.setJsonfile(extractJsonFileName(elementJson));
        return vo;
    }

    private static String extractJsonFileName(String jsonPath) {
        if (StringUtils.isBlank(jsonPath)) {
            return null;
        }
        int index = Math.max(jsonPath.lastIndexOf('/'), jsonPath.lastIndexOf('\\'));
        return index >= 0 && index < jsonPath.length() - 1 ? jsonPath.substring(index + 1) : jsonPath;
    }
}
