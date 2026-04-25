package com.mes.application.command.manufacturerMeta;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.mes.domain.manufacturer.manufacturerMeta.service.ManufacturerDeviceCfgService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppManufacturerDeviceCfgService {

    @Autowired
    private ManufacturerDeviceCfgService domainDeviceCfgService;

    @Autowired
    private ManufacturerDeviceCfgRepository manufacturerDeviceCfgRepository;

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
        int size = 200;
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
}
