package com.mes.domain.shared.algorithm.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 算法核心接口调用类型。
 */
public enum AlgorithmCoreApiCallType {
    GENERATE_MASK_FILES_ASYNC("generateMaskFilesAsync", "异步图片遮罩抠图", "orderItemId"),
    GENERATE_MASK_FILES_SYNC("generateMaskFilesSync", "同步图片遮罩抠图", "orderItemId"),
    CONVERT_GRAY_IMG_TO_SVG_ASYNC("convertGrayImgToSvgAsync", "异步灰度图转 SVG", "orderItemId"),
    CONVERT_GRAY_IMG_TO_SVG("convertGrayImgToSvg", "同步灰度图转 SVG", "orderItemId"),
    GENERATE_NESTED_FILES_ASYNC("generateNestedFilesAsync", "异步通用排版", "typesettingId"),
    GENERATE_NESTED_FILES_SYNC("generateNestedFilesSync", "同步通用排版", "typesettingId"),
    GENERATE_GRID_NESTED_FILES_ASYNC("generateGridNestedFilesAsync", "异步网格排版", "typesettingId"),
    GENERATE_RECT_NESTED_FILES_ASYNC("generateRectNestedFilesAsync", "异步矩形排版", "typesettingId"),
    GENERATE_VERTICAL_NESTED_FILES_ASYNC("generateVerticalNestedFilesAsync", "异步竖排排版", "typesettingId"),
    GENERATE_FORME_ASYNC("generateFormeAsync", "异步生成印版", "typesettingId"),
    GENERATE_FORME("generateForme", "同步生成印版", "typesettingId");

    private final String value;
    private final String description;
    private final String sourceIdField;

    AlgorithmCoreApiCallType(String value, String description, String sourceIdField) {
        this.value = value;
        this.description = description;
        this.sourceIdField = sourceIdField;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceIdField() {
        return sourceIdField;
    }

    public static Optional<AlgorithmCoreApiCallType> fromValue(String value) {
        return Arrays.stream(values()).filter(type -> type.value.equals(value)).findFirst();
    }
}
