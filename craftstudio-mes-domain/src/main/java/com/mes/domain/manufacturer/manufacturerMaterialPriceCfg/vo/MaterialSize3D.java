package com.mes.domain.manufacturer.manufacturerMaterialPriceCfg.vo;

import lombok.Data;

@Data
public class MaterialSize3D {
    private float width;
    private float height;
    private float depth;

    public MaterialSize3D(float width, float height, float depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public boolean valid(){
        return width>0 && height>0 && depth>0;
    }
}
