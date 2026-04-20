package com.mes.application.command.Address;

import com.piliofpala.craftstudio.shared.domain.geo.world.vo.Region;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RegionOptionsResult {
    private List<RegionOption> options = new ArrayList<>();

    public void addRegionoptions(List<Region> regions) {
        for (Region region : regions) {
            RegionOption option = new RegionOption();
            option.setCode(region.getCode());
            option.setName(region.getName());
            option.setLevel(region.getLevel());
            this.options.add(option);
        }
    }

    @Data
    public static class RegionOption {
        private String code;
        private String name;
        private Integer level;
    }
}
