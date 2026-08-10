package com.mes.infra.dal.manufacurer.device;

import com.mes.domain.manufacturer.device.entity.Device;
import com.mes.domain.manufacturer.device.repository.DeviceInfoRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.device.po.DevicePo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class DeviceRepositoryImp extends BaseRepositoryImp<Device, DevicePo> implements DeviceInfoRepository {

    @Override
    public Class<DevicePo> poClass() {
        return DevicePo.class;
    }

    @Override
    public List<Device> findByDeviceInfoIds(Collection<String> deviceInfoIds) {
        if (deviceInfoIds == null || deviceInfoIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<DevicePo> devices = mongoTemplate.find(
                new SoftDeleteQuery(Criteria.where("deviceInfoId").in(deviceInfoIds)), poClass());
        return devices.stream().map(DevicePo::toDO).toList();
    }
}
