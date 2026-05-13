package com.mes.application.command.typesetting.strategy;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;

/**
 * 镜像印版触发策略：
 * 用于判断并构建双面场景下的镜像排版数据。
 */
public interface MirrorFormeStrategy {
    /**
     * 是否匹配当前排版数据。
     */
    boolean supports(TypesettingInfo info);

    /**
     * 构建镜像排版数据；返回 null 表示不生成镜像印版。
     */
    TypesettingInfo buildMirrorTypesettingInfo(TypesettingInfo origin);
}

