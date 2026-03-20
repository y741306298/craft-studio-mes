package com.mes.infra.dal.manufacurer.device;

import com.mes.domain.manufacturer.device.entity.Device;
import com.mes.domain.manufacturer.device.repository.DeviceInfoRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.device.po.DevicePo;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceRepositoryImp extends BaseRepositoryImp<Device, DevicePo> implements DeviceInfoRepository {

    @Override
    public Class<DevicePo> poClass() {
        return DevicePo.class;
    }
}
