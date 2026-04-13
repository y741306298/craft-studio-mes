package com.mes.application.command.device;

import com.mes.domain.manufacturer.device.entity.Device;
import com.mes.domain.manufacturer.device.repository.DeviceInfoRepository;
import com.mes.domain.manufacturer.device.service.DeviceService;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppDeviceService {

    @Autowired
    private DeviceService domainDeviceService;

    @Autowired
    private DeviceInfoRepository deviceInfoRepository;

    @Autowired
    private ManufacturerDeviceCfgRepository manufacturerDeviceCfgRepository;

    public PagedResult<Device> findDevices(String deviceName, String deviceType, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        List<Device> items;
        long total;

        if (StringUtils.isBlank(deviceName) && StringUtils.isBlank(deviceType)) {
            items = deviceInfoRepository.list(query.getCurrent(), query.getSize());
            total = deviceInfoRepository.total();
        } else {
            items = domainDeviceService.findDevicesByCondition(deviceName, deviceType, (int) query.getCurrent(), query.getSize());
            total = domainDeviceService.getTotalCount(deviceName, deviceType);
        }

        return new PagedResult<Device>(items, total, query.getSize(), query.getCurrent());
    }

    public Device addDevice(Device command) {
        if (command == null) {
            throw new IllegalArgumentException("设备不能为空");
        }
        return domainDeviceService.addDevice(command);
    }

    public void updateDevice(Device command) {
        if (command == null) {
            throw new IllegalArgumentException("设备不能为空");
        }
        if (StringUtils.isBlank(command.getId())) {
            throw new IllegalArgumentException("设备 ID 不能为空");
        }
        domainDeviceService.updateDevice(command);
    }

    public String deleteDevice(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        Device byId = domainDeviceService.findById(id);
        String deviceInfoId = byId.getDeviceInfoId();
        List<com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg> cfgs = 
            manufacturerDeviceCfgRepository.filterList(1, 100, 
                java.util.Collections.singletonMap("deviceId", deviceInfoId));
        
        if (cfgs != null && !cfgs.isEmpty()) {
            boolean hasNormalCfg = cfgs.stream()
                .anyMatch(cfg -> cfg.getStatus() == com.mes.domain.manufacturer.enums.CfgStatus.NORMAL);
            
            if (hasNormalCfg) {
                return "该设备已被制造商设备配置使用，无法删除";
            }
            
            for (com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg cfg : cfgs) {
                manufacturerDeviceCfgRepository.delete(cfg);
            }
        }
        
        domainDeviceService.deleteDevice(id);
        return null;
    }

    public Device findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        return domainDeviceService.findById(id);
    }

    public Device findByDeviceInfoId(String deviceInfoId) {
        if (StringUtils.isBlank(deviceInfoId)) {
            throw new IllegalArgumentException("设备业务ID不能为空");
        }
        
        List<Device> devices = deviceInfoRepository.filterList(1, 1, 
            java.util.Collections.singletonMap("deviceInfoId", deviceInfoId));
        
        if (devices == null || devices.isEmpty()) {
            return null;
        }
        
        return devices.get(0);
    }
}
