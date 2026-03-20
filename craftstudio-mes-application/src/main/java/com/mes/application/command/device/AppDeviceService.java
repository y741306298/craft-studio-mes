package com.mes.application.command.device;

import com.mes.domain.manufacturer.device.entity.Device;
import com.mes.domain.manufacturer.device.repository.DeviceInfoRepository;
import com.mes.domain.manufacturer.device.service.DeviceService;
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

    public PagedResult<Device> findDevices(String deviceName, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        List<Device> items;
        long total;

        if (StringUtils.isBlank(deviceName)) {
            items = deviceInfoRepository.list(query.getCurrent(), query.getSize());
            total = deviceInfoRepository.total();
        } else {
            items = domainDeviceService.findDevicesByName(deviceName, (int) query.getCurrent(), query.getSize());
            total = domainDeviceService.getTotalCount(deviceName);
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

    public void deleteDevice(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainDeviceService.deleteDevice(id);
    }

    public Device findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        return domainDeviceService.findById(id);
    }
}
