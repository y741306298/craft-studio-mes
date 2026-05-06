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
        vo.setTypesettingId(info.getTypesettingId());
        vo.setManufacturerMetaId(info.getManufacturerMetaId());
        vo.setManufactureOrderId(info.getManufactureOrderId());
        vo.setStatus(info.getStatus());
        vo.setStyleName(info.getStyleName());
        vo.setMaterialConfig(info.getMaterialConfig());
        vo.setQuantity(info.getQuantity());
        vo.setElement(info.getElement());
        vo.setImageUrl(info.getImageUrl());
        vo.setSvgUrl(info.getSvgUrl());
        vo.setProcedureFlow(info.getProcedureFlow());
        vo.setTemplateCode(info.getTemplateCode());
        vo.setTypesettingCells(info.getTypesettingCells());
        vo.setLeaveQuantity(info.getLeaveQuantity());
        vo.setLayoutMode(info.getLayoutMode());
        vo.setRemark(info.getRemark());
        vo.setProcessingFlow(info.getProcessingFlow());
        vo.setMarks(info.getMarks());
        vo.setCreatedAt(info.getCreatedAt());
        vo.setUpdatedAt(info.getUpdatedAt());
        vo.setCreatedBy(info.getCreatedBy());
        vo.setUpdatedBy(info.getUpdatedBy());

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
