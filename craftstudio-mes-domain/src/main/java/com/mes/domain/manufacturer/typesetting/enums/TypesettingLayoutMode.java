package com.mes.domain.manufacturer.typesetting.enums;


import io.micrometer.common.util.StringUtils;

/**
 * 排版方式枚举：
 * 决定文件输出要求（json/plt/svg）以及二维码、订单号、临时码与定位点策略。
 */
public enum TypesettingLayoutMode {
    /**
     * 异形切割（plt二维码）-圆形定位点：
     * 需要 json/plt/svg，码位策略为 plt_qr，临时码格式 xxx。
     */
    SHAPED_CUTTING_PLT_QR_CIRCLE(
            "shaped_cutting_plt_qr_circle",
            "异形切割（plt二维码）-圆形定位点",
            "shaped_typesetting",
            true, true, true,
            "plt_qr",
            "xxx",
            "circle"
    ),
    /**
     * 异形切割（plt二维码）-方形定位点：
     * 需要 json/plt/svg，码位策略为 plt_qr，临时码格式 xxx。
     */
    SHAPED_CUTTING_PLT_QR_SQUARE(
            "shaped_cutting_plt_qr_square",
            "异形切割（plt二维码）-方形定位点",
            "shaped_typesetting",
            true, true, true,
            "plt_qr",
            "xxx",
            "square"
    ),
    /**
     * xy切割(切割辅助线-裁赋）：
     * 需要 json/svg，不需要 plt，码位策略为 side_aux_line。
     */
    XY_CUTTING_AUX_LINE_CAIFU(
            "xy_cutting_aux_line_caifu",
            "xy切割(切割辅助线-裁赋）",
            "grid_typesetting",
            true, false, true,
            "side_aux_line",
            null,
            "none"
    ),
    /**
     * xy切割(切割辅助线-九段）：
     * 需要 json/svg，不需要 plt，码位策略为 side_aux_line。
     */
    XY_CUTTING_AUX_LINE_NINE_SEGMENT(
            "xy_cutting_aux_line_nine_segment",
            "xy切割(切割辅助线-九段）",
            "grid_typesetting",
            true, false, true,
            "side_aux_line",
            null,
            "none"
    ),
    /**
     * xy切割(切割辅助线-六渡大板模式）：
     * 需要 json/svg，不需要 plt，码位策略为 side_aux_line。
     */
    XY_CUTTING_AUX_LINE_LIUDU_LARGE_BOARD(
            "xy_cutting_aux_line_liudu_large_board",
            "xy切割(切割辅助线-六渡大板模式）",
            "grid_typesetting",
            true, false, true,
            "side_aux_line",
            null,
            "none"
    ),
    /**
     * xy切割(切割辅助线-六渡小图模式）：
     * 需要 json/svg，不需要 plt，码位策略为 side_aux_line。
     */
    XY_CUTTING_AUX_LINE_LIUDU_SMALL_GRAPH(
            "xy_cutting_aux_line_liudu_small_graph",
            "xy切割(切割辅助线-六渡小图模式）",
            "grid_typesetting",
            true, false, true,
            "side_aux_line",
            null,
            "none"
    ),
    /**
     * xy切割(切割辅助线-全自动打扣）：
     * 需要 json/svg，不需要 plt，码位策略为 side_aux_line。
     */
    XY_CUTTING_AUX_LINE_FULL_AUTO_BUCKLE(
            "xy_cutting_aux_line_full_auto_buckle",
            "xy切割(切割辅助线-全自动打扣）",
            "grid_typesetting",
            true, false, true,
            "side_aux_line",
            null,
            "none"
    );

    private final String code;
    private final String description;
    private final String layoutCategory;
    private final boolean requireJsonFile;
    private final boolean requirePltFile;
    private final boolean requireSvgFile;
    private final String codeGenerateType;
    private final String tempCodeFormat;
    private final String anchorPointShape;

    TypesettingLayoutMode(String code, String description, String layoutCategory,
                          boolean requireJsonFile, boolean requirePltFile, boolean requireSvgFile,
                          String codeGenerateType, String tempCodeFormat, String anchorPointShape) {
        this.code = code;
        this.description = description;
        this.layoutCategory = layoutCategory;
        this.requireJsonFile = requireJsonFile;
        this.requirePltFile = requirePltFile;
        this.requireSvgFile = requireSvgFile;
        this.codeGenerateType = codeGenerateType;
        this.tempCodeFormat = tempCodeFormat;
        this.anchorPointShape = anchorPointShape;
    }

    public static TypesettingLayoutMode fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return SHAPED_CUTTING_PLT_QR_CIRCLE;
        }
        for (TypesettingLayoutMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        if ("xy_cutting_aux_line_liudu".equalsIgnoreCase(code)) {
            return XY_CUTTING_AUX_LINE_LIUDU_LARGE_BOARD;
        }
        throw new IllegalArgumentException("未知排版方式：" + code);
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getLayoutCategory() {
        return layoutCategory;
    }

    public boolean isRequireJsonFile() {
        return requireJsonFile;
    }

    public boolean isRequirePltFile() {
        return requirePltFile;
    }

    public boolean isRequireSvgFile() {
        return requireSvgFile;
    }

    public String getCodeGenerateType() {
        return codeGenerateType;
    }

    public String getTempCodeFormat() {
        return tempCodeFormat;
    }

    public String getAnchorPointShape() {
        return anchorPointShape;
    }
}
