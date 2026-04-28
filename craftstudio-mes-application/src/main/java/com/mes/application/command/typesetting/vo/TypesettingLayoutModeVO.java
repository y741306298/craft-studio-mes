package com.mes.application.command.typesetting.vo;

import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import lombok.Data;

/**
 * 排版方式完整对象
 */
@Data
public class TypesettingLayoutModeVO {
    private String code;
    private String description;
    private String layoutCategory;
    private boolean requireJsonFile;
    private boolean requirePltFile;
    private boolean requireSvgFile;
    private String codeGenerateType;
    private String tempCodeFormat;
    private String anchorPointShape;
    private int nestingSpacingMm;

    public static TypesettingLayoutModeVO from(TypesettingLayoutMode mode) {
        TypesettingLayoutModeVO vo = new TypesettingLayoutModeVO();
        vo.setCode(mode.getCode());
        vo.setDescription(mode.getDescription());
        vo.setLayoutCategory(mode.getLayoutCategory());
        vo.setRequireJsonFile(mode.isRequireJsonFile());
        vo.setRequirePltFile(mode.isRequirePltFile());
        vo.setRequireSvgFile(mode.isRequireSvgFile());
        vo.setCodeGenerateType(mode.getCodeGenerateType());
        vo.setTempCodeFormat(mode.getTempCodeFormat());
        vo.setAnchorPointShape(mode.getAnchorPointShape());
        vo.setNestingSpacingMm(mode.getNestingSpacingMm());
        return vo;
    }
}
