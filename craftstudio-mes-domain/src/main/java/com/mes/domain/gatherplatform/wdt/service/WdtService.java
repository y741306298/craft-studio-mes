package com.mes.domain.gatherplatform.wdt.service;

import com.mes.domain.gatherplatform.wdt.entity.WdtConfig;
import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;
import com.mes.domain.gatherplatform.wdt.repository.WdtConfigRepository;
import com.mes.domain.gatherplatform.wdt.repository.WdtLabelRecordRepository;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 旺店通快递配置及面单记录领域服务。
 */
@Service
public class WdtService {
    @Autowired
    private WdtConfigRepository configRepository;

    @Autowired
    private WdtLabelRecordRepository labelRecordRepository;

    /**
     * 新增快递配置。
     *
     * @param config 待新增的配置
     * @return 已保存的配置
     */
    public WdtConfig add(WdtConfig config) {
        validate(config);
        return configRepository.add(config);
    }

    /**
     * 更新快递配置。
     *
     * @param config 待更新的配置
     */
    public void update(WdtConfig config) {
        validate(config);
        if (StringUtils.isBlank(config.getId())) {
            throw new IllegalArgumentException("配置ID不能为空");
        }
        configRepository.update(config);
    }

    /**
     * 根据 ID 删除快递配置；配置不存在时不执行操作。
     *
     * @param id 配置 ID
     */
    public void delete(String id) {
        WdtConfig config = configRepository.findById(id);
        if (config != null) {
            configRepository.delete(config);
        }
    }

    /**
     * 根据 ID 查询快递配置。
     *
     * @param id 配置 ID
     * @return 配置；不存在时返回 {@code null}
     */
    public WdtConfig findById(String id) {
        return configRepository.findById(id);
    }

    /**
     * 分页查询快递配置。
     *
     * @param current 当前页码
     * @param size 每页数量
     * @return 当前页配置列表
     */
    public List<WdtConfig> list(long current, int size) {
        return configRepository.list(current, size);
    }

    /**
     * 查询快递配置总数。
     *
     * @return 配置总数
     */
    public long total() {
        return configRepository.total();
    }

    /**
     * 查询预下单所需的工厂快递配置。
     *
     * @param manufacturerMetaId 工厂元数据 ID
     * @param presetType MES 订单物流预设类型
     * @return 匹配的配置；不存在时返回 {@code null}
     */
    public WdtConfig findConfig(String manufacturerMetaId, String presetType) {
        return configRepository.findByManufacturerMetaIdAndPresetType(manufacturerMetaId, presetType);
    }

    /**
     * 保存旺店通快递面单记录。
     *
     * @param record 面单记录
     * @return 已保存的面单记录
     */
    public WdtLabelRecord saveLabelRecord(WdtLabelRecord record) {
        return labelRecordRepository.add(record);
    }

    /**
     * 校验快递配置中的必填字段。
     *
     * @param config 待校验的配置
     */
    private void validate(WdtConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("快递配置不能为空");
        }
        if (StringUtils.isBlank(config.getManufacturerMetaId())) {
            throw new IllegalArgumentException("manufacturerMetaId不能为空");
        }
        if (StringUtils.isBlank(config.getWarehouseId())) {
            throw new IllegalArgumentException("warehouseId不能为空");
        }
        if (StringUtils.isBlank(config.getLogisticsId())) {
            throw new IllegalArgumentException("logisticsId不能为空");
        }
        if (StringUtils.isBlank(config.getLogisticsName())) {
            throw new IllegalArgumentException("logisticsName不能为空");
        }
        if (StringUtils.isBlank(config.getPresetType())) {
            throw new IllegalArgumentException("presetType不能为空");
        }
    }
}
