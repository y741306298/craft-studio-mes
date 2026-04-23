package com.mes.application.command.typesetting.layout;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Supplier;

@Data
public class FormeBuildContext {
    /** 待确认排版实体（包含 element、typesettingCells 等源数据）。 */
    private TypesettingInfo typesettingInfo;
    /** 本次业务主键（优先 typesettingId，兜底 record id）。 */
    private String businessId;
    /** 原始 element 宽度，单位 mm。 */
    private BigDecimal nestedWidth;
    /** 原始 element 高度，单位 mm。 */
    private BigDecimal nestedHeight;
    /** 约定的上下边距高度，单位 mm。 */
    private BigDecimal marginHeight;
    /** 生成元素 B（xxx.plt 名称）的提供器。 */
    private Supplier<String> plateNameSupplier;
    /** 生成元素 BB（第二个 xxx.plt 名称）的提供器。 */
    private Supplier<String> plateNameBBSupplier;
    /** 二维码生成器：入参为内容，返回 data URI。 */
    private Function<String, String> qrDataUriGenerator;
    /** 元素 A 解析器：从 typesettingInfo 提取 typesetting 关联标识。 */
    private Function<TypesettingInfo, String> elementAResolver;
}
