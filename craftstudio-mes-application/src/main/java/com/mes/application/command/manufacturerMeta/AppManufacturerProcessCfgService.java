package com.mes.application.command.manufacturerMeta;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity.ManufacturerProcessPriceCfg;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.service.ManufacturerProcessPriceCfgService;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.vo.MaterialProcessPrice;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppManufacturerProcessCfgService {

    private final ManufacturerProcessPriceCfgService processPriceCfgService;

    public AppManufacturerProcessCfgService(ManufacturerProcessPriceCfgService processPriceCfgService) {
        this.processPriceCfgService = processPriceCfgService;
    }

    /**
     * 分页获取某工厂的工艺信息并转化为 ManufacturerProcessPriceCfg
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param current 当前页码，从 1 开始，必须大于 0
     * @param size 每页大小，范围 1-100
     * @return 工艺价格配置列表
     * @throws BusinessNotAllowException 当参数不合法时抛出此异常
     */
    public List<ManufacturerProcessPriceCfg> getProcessPriceCfgByManufacturerId(String manufacturerId, int current, int size) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        return processPriceCfgService.findByManufacturerId(manufacturerId, current, size);
    }

    /**
     * 根据 manufacturerId 和 processId 更新工艺价格配置
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param processId 工艺 ID，不能为空
     * @param processPrice 新的工艺价格，可为 null（为 null 时不更新）
     * @param basePrice 新的基准价格，可为 null（为 null 时不更新）
     * @param materialProcessPrices 材料工艺价格列表，可为 null（为 null 时不更新）
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void updateProcessPriceConfig(String manufacturerId, String processId, 
                                         UnitPrice processPrice, Double basePrice,
                                         List<MaterialProcessPrice> materialProcessPrices) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(processId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工艺 ID 不能为空");
        }

        ManufacturerProcessPriceCfg cfg = processPriceCfgService.findByManufacturerId(manufacturerId, 1, 100)
                .stream()
                .filter(item -> processId.equals(item.getProcessId()))
                .findFirst()
                .orElseThrow(() -> new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "未找到对应的工艺价格配置"));

        boolean needUpdate = false;

        if (processPrice != null) {
            if (processPrice.getPrice() == null) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工艺价格不能为空");
            }
            cfg.setProcessPrice(processPrice);
            needUpdate = true;
        }

        if (basePrice != null) {
            cfg.setBasePrice(basePrice);
            needUpdate = true;
        }

        if (materialProcessPrices != null) {
            for (MaterialProcessPrice mpp : materialProcessPrices) {
                if (mpp.getProcessPrice() != null && mpp.getProcessPrice().getPrice() == null) {
                    throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "材料工艺价格不能为空");
                }
            }
            cfg.setMaterialProcessPrices(materialProcessPrices);
            needUpdate = true;
        }

        if (needUpdate) {
            processPriceCfgService.update(cfg);
        }
    }

    /**
     * 逻辑删除工艺价格配置（将 status 改为 INVALID）
     * 
     * @param manufacturerId 制造商 ID，不能为空
     * @param processId 工艺 ID，不能为空
     * @throws BusinessNotAllowException 当参数不合法或记录不存在时抛出此异常
     */
    public void deleteProcessPriceConfig(String manufacturerId, String processId) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }
        if (StringUtils.isBlank(processId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工艺 ID 不能为空");
        }

        ManufacturerProcessPriceCfg cfg = processPriceCfgService.findByManufacturerId(manufacturerId, 1, 100)
                .stream()
                .filter(item -> processId.equals(item.getProcessId()))
                .findFirst()
                .orElseThrow(() -> new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "未找到对应的工艺价格配置"));

        cfg.setStatus(CfgStatus.INVALID);
        processPriceCfgService.update(cfg);
    }
}
