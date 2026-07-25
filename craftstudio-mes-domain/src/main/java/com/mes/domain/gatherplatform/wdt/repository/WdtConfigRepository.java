package com.mes.domain.gatherplatform.wdt.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.gatherplatform.wdt.entity.WdtConfig;

/**
 * 旺店通快递配置仓储。
 */
public interface WdtConfigRepository extends BaseRepository<WdtConfig> {
    /**
     * 根据工厂和物流预设类型查询一条快递配置。
     *
     * @param manufacturerMetaId 工厂元数据 ID
     * @param presetType MES 订单物流预设类型
     * @return 匹配的配置；不存在时返回 {@code null}
     */
    WdtConfig findByManufacturerMetaIdAndPresetType(String manufacturerMetaId, String presetType);
}
