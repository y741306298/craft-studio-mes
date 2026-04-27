package com.mes.application.command.typesetting.layout;

import com.mes.application.command.typesetting.vo.LayoutConfirmResult;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;

/**
 * 排版确认扩展点：
 * 用于承载某些排版模式在 confirmLayout 阶段的定制化确认逻辑。
 */
public interface TypesettingLayoutModeConfirmService {
    /**
     * 当前实现支持的排版模式。
     */
    TypesettingLayoutMode supportMode();

    /**
     * 执行该模式下的确认排版逻辑。
     */
    LayoutConfirmResult confirm(TypesettingInfo typesettingInfo);
}

