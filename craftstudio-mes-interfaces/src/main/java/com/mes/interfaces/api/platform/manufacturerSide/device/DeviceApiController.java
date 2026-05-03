package com.mes.interfaces.api.platform.manufacturerSide.device;

import com.mes.application.command.device.AppDeviceService;
import com.mes.application.dto.req.device.DeviceListRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.device.DeviceListResponse;
import com.mes.domain.manufacturer.device.entity.Device;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manufacturerSide/device")
public class DeviceApiController {

    @Autowired
    private AppDeviceService appDeviceService;

    /**
     * 分页查询设备列表
     * @param request 分页请求参数
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<DeviceListResponse> listDevices(
            @Valid @RequestBody DeviceListRequest request) {

        PagedQuery query = request.toPagedQuery();
        String deviceName = request.getDeviceName();
        String deviceType = request.getDeviceType();

        PagedResult<Device> result = appDeviceService.findDevices(deviceName,deviceType, query);

        List<DeviceListResponse> responses = result.items().stream()
                .map(DeviceListResponse::from)
                .collect(Collectors.toList());

        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }


}
