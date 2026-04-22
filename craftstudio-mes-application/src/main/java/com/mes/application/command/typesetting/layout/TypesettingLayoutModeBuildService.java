package com.mes.application.command.typesetting.layout;

import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;

/**
 * 排版模式构建器接口。
 *
 * <p>职责：将“某一种 TypesettingLayoutMode”对应的参数拼装逻辑下沉到独立 service。
 * 每个 mode 对应一个实现类，最终输出 Forme 生成请求所需的：
 * margin / marks / anchorPoints / outputs / uploadPath。
 */
public interface TypesettingLayoutModeBuildService {
    /**
     * 当前实现类支持的排版模式。
     */
    TypesettingLayoutMode supportMode();

    /**
     * 基于上下文构建模式化结果。
     */
    FormeLayoutBuildResult build(FormeBuildContext context);
}
