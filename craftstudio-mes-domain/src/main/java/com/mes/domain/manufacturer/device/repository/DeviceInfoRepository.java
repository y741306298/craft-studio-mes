package com.mes.domain.manufacturer.device.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.manufacturer.device.entity.Device;

import java.util.Collection;
import java.util.List;

public interface DeviceInfoRepository extends BaseRepository<Device> {
    List<Device> findByDeviceInfoIds(Collection<String> deviceInfoIds);
}
