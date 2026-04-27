package com.mes.application.command.typesetting.layout;

import com.mes.application.command.typesetting.vo.LayoutConfirmResult;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 基础网格排版确认逻辑：
 * 不触发额外印版放置/生成，直接落库确认状态。
 */
@Service
public class GridBasicLayoutConfirmService implements TypesettingLayoutModeConfirmService {

    @Autowired
    private TypesettingService domainTypesettingService;

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.GRID_TYPESETTING_BASIC;
    }

    @Override
    public LayoutConfirmResult confirm(TypesettingInfo typesettingInfo) {
        typesettingInfo.setStatus(TypesettingStatus.PENDING.getCode());
        domainTypesettingService.updateTypesetting(typesettingInfo);

        LayoutConfirmResult result = new LayoutConfirmResult();
        result.setSuccess(true);
        result.setMessage("基础网格排版确认完成，已保存数据");
        return result;
    }
}

