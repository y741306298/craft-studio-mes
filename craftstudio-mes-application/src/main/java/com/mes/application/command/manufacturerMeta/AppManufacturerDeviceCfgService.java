package com.mes.application.command.manufacturerMeta;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.mes.domain.manufacturer.manufacturerMeta.service.ManufacturerDeviceCfgService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingPrintTaskStatus;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingPrintTaskRepository;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingDownloadTaskData;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppManufacturerDeviceCfgService {

    @Autowired
    private ManufacturerDeviceCfgService domainDeviceCfgService;

    @Autowired
    private ManufacturerDeviceCfgRepository manufacturerDeviceCfgRepository;

    @Autowired
    private TypesettingPrintTaskRepository typesettingPrintTaskRepository;

    public PagedResult<ManufacturerDeviceCfg> findDeviceCfgsByManufacturerId(String manufacturerMetaId, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        List<ManufacturerDeviceCfg> items;
        long total;

        if (StringUtils.isBlank(manufacturerMetaId)) {
            items = manufacturerDeviceCfgRepository.list(query.getCurrent(), query.getSize());
            total = manufacturerDeviceCfgRepository.total();
        } else {
            items = domainDeviceCfgService.findDeviceCfgsByManufacturerId(manufacturerMetaId, (int) query.getCurrent(), query.getSize());
            total = domainDeviceCfgService.getTotalCountByManufacturerId(manufacturerMetaId);
        }

        return new PagedResult<ManufacturerDeviceCfg>(items, total, query.getSize(), query.getCurrent());
    }

    public void addDeviceCfg(ManufacturerDeviceCfg command) {
        if (command == null) {
            throw new IllegalArgumentException("设备配置不能为空");
        }
        if (StringUtils.isBlank(command.getManufacturerMetaId())) {
            throw new IllegalArgumentException("制造商 ID 不能为空");
        }
        
        domainDeviceCfgService.addDeviceCfg(command);
    }
    
    public void updateDeviceCfg(ManufacturerDeviceCfg command) {
        if (command == null) {
            throw new IllegalArgumentException("设备配置不能为空");
        }
        if (StringUtils.isBlank(command.getId())) {
            throw new IllegalArgumentException("设备 ID 不能为空");
        }
        domainDeviceCfgService.updateDeviceCfg(command);
    }
    
    public void deleteDeviceCfg(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainDeviceCfgService.deleteDeviceCfg(id);
    }
    
    public ManufacturerDeviceCfg findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        return domainDeviceCfgService.findById(id);
    }

    public List<ManufacturerDeviceCfg> listDeviceCfgsByManufacturerId(String manufacturerMetaId) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("制造商 ID 不能为空");
        }

        List<ManufacturerDeviceCfg> result = new ArrayList<ManufacturerDeviceCfg>();
        int current = 1;
        int size = 99;
        while (true) {
            List<ManufacturerDeviceCfg> pageItems =
                    domainDeviceCfgService.findDeviceCfgsByManufacturerId(manufacturerMetaId, current, size);
            if (pageItems == null || pageItems.isEmpty()) {
                break;
            }
            result.addAll(pageItems);
            if (pageItems.size() < size) {
                break;
            }
            current++;
        }
        return result;
    }

    public ManufacturerDeviceCfg bindDeviceById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("设备id不能为空");
        }

        ManufacturerDeviceCfg cfg = domainDeviceCfgService.findById(id);
        if (cfg == null) {
            return null;
        }
        if (cfg.isBound()) {
            throw new IllegalStateException("绑定失败，机器已绑定");
        }
        if (cfg.getBoundVersion() == null) {
            cfg.setBoundVersion(1);
        }
        cfg.setBound(true);
        domainDeviceCfgService.updateDeviceCfg(cfg);
        return cfg;
    }

    public List<TypesettingDownloadTaskData> listDownloadTasksByDeviceCfg(String id, Integer version) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("设备id不能为空");
        }
        if (version == null) {
            throw new IllegalArgumentException("绑定版本不能为空");
        }

        ManufacturerDeviceCfg cfg = domainDeviceCfgService.findById(id);
        if (cfg == null) {
            return null;
        }

        if (!cfg.isBound() || cfg.getBoundVersion() == null || !cfg.getBoundVersion().equals(version)) {
            throw new IllegalStateException("该设备已经解绑，需要重新绑定");
        }

        Map<String, Object> cfgFilters = new HashMap<String, Object>();
        cfgFilters.put("manufacturerMetaId", cfg.getManufacturerMetaId());
        cfgFilters.put("deviceCode", cfg.getDeviceCode());
        List<ManufacturerDeviceCfg> matchedCfgList = manufacturerDeviceCfgRepository.filterList(1, 1, cfgFilters);
        if (matchedCfgList == null || matchedCfgList.isEmpty()) {
            return new ArrayList<TypesettingDownloadTaskData>();
        }

        Map<String, Object> taskFilters = new HashMap<String, Object>();
        taskFilters.put("deviceInfoId", matchedCfgList.get(0).getDeviceInfoId());
        taskFilters.put("status", TypesettingPrintTaskStatus.PENDING.getCode());
        List<TypesettingDownloadTaskData> result = new ArrayList<TypesettingDownloadTaskData>();

        int current = 1;
        int size = 200;
        while (true) {
            List<TypesettingPrintTask> pageItems = typesettingPrintTaskRepository.filterList(current, size, taskFilters);
            if (pageItems == null || pageItems.isEmpty()) {
                break;
            }
            for (TypesettingPrintTask task : pageItems) {
                if (task == null) {
                    continue;
                }
                if (task.getData() != null) {
                    result.add(task.getData());
                }
                task.setStatus(TypesettingPrintTaskStatus.CLAIMED.getCode());
                typesettingPrintTaskRepository.update(task);
            }
            if (pageItems.size() < size) {
                break;
            }
            current++;
        }
        return result;
    }
}
