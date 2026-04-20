package com.mes.domain.manufacturer.typesetting.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.vo.ProductionPieceCell;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingCell;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TypesettingInfo extends BaseEntity {
    //排版文件
    private String typesettingId;
    /**
     * 排版算法返回结果（单条）：
     * - nestedSvg
     * - utilization
     * - width
     * - height
     */
    private TypesettingElement element;
    private List<String> materialConfigs;
    private String status;
    private Integer totalQuantity;
    private Integer quantity;
    private Integer completedQuantity;
    private List<TypesettingCell> typesettingCells;
    private List<ProductionPieceCell> pieceCells;
    private ProcedureFlow procedureFlow;
    private String remark;
    /**
     * 参与排版用的轮廓SVG（来源由上游工序补充）
     */
    private String maskSvg;
    /**
     * 排版方式，可编辑：
     * shaped_cutting_plt_qr_circle / shaped_cutting_plt_qr_square /
     * xy_cutting_aux_line_caifu / xy_cutting_aux_line_nine_segment / xy_cutting_aux_line_liudu
     */
    private String layoutMode;
    /**
     * 排版大类：shaped_typesetting（异形排版）/ grid_typesetting（网格排版）
     */
    private String layoutCategory;
    private Boolean requireJsonFile;
    private Boolean requirePltFile;
    private Boolean requireSvgFile;
    /**
     * 二维码、订单号、临时码生成方式（例如：plt_qr / side_aux_line）
     */
    private String codeGenerateType;
    /**
     * 临时码格式描述（例如：xxx）
     */
    private String tempCodeFormat;
    /**
     * 定位点形状（circle / square / none）
     */
    private String anchorPointShape;

    public void applyLayoutModeConfig() {
        TypesettingLayoutMode mode = TypesettingLayoutMode.fromCode(this.layoutMode);
        this.layoutCategory = mode.getLayoutCategory();
        this.requireJsonFile = mode.isRequireJsonFile();
        this.requirePltFile = mode.isRequirePltFile();
        this.requireSvgFile = mode.isRequireSvgFile();
        this.codeGenerateType = mode.getCodeGenerateType();
        this.tempCodeFormat = mode.getTempCodeFormat();
        this.anchorPointShape = mode.getAnchorPointShape();
    }

}
