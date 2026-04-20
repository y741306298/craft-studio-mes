package com.mes.application.command.Address;

import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.piliofpala.craftstudio.shared.domain.geo.world.repository.WorldRepository;
import com.piliofpala.craftstudio.shared.domain.geo.world.vo.Region;
import com.piliofpala.craftstudio.shared.domain.geo.world.vo.World;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressRegionOptionsService {

    @Autowired
    private WorldRepository worldRepository;


    public ApiResponse<RegionOptionsResult> getRegionoptions(RegionOptionsRequest req) {
        World world = worldRepository.loadWorld();
        if (world.getCountries() == null || world.getCountries().isEmpty()) {
            return ApiResponse.success(new RegionOptionsResult());
        }
        if (StringUtils.isBlank(req.getParentRegionCode())) req.setParentRegionCode("CN");
        Region parentRegion = world.findRegionByCode(req.getParentRegionCode());
        if (parentRegion == null) {
            throw BusinessNotAllowException.badParamsException("无效的地区码");
        }
        List<Region> initRegions = parentRegion.getChildren();
        if (initRegions == null || initRegions.isEmpty()) {
            return ApiResponse.success(new RegionOptionsResult());
        }
        RegionOptionsResult regionOptionsResult = new RegionOptionsResult();
        regionOptionsResult.addRegionoptions(initRegions);
        return ApiResponse.success(regionOptionsResult);
    }
}
