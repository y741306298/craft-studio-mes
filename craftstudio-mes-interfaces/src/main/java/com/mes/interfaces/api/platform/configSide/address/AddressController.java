package com.mes.interfaces.api.platform.configSide.address;

import com.mes.application.command.Address.AddressRegionOptionsService;
import com.mes.application.command.Address.RegionOptionsRequest;
import com.mes.application.command.Address.RegionOptionsResult;
import com.mes.domain.base.repository.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configSide/address")
public class AddressController {

    @Autowired
    private AddressRegionOptionsService addressRegionOptionsService;

    /**
     * 获取地区选项列表
     * @param parentRegionCode 父地区码，null 表示获取第一级地区（国家）
     * @return 地区选项结果
     */
        @GetMapping("/regionOptions")
    public ApiResponse<RegionOptionsResult> getRegionOptions(
            @RequestParam(required = false) String parentRegionCode) {
        RegionOptionsRequest request = new RegionOptionsRequest();
        request.setParentRegionCode(parentRegionCode);
        return addressRegionOptionsService.getRegionoptions(request);
    }
}
