package com.mes.domain.order.orderInfo.vo;


import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;

public class MaterialConfig {
    public enum MaterialType {
        WIDE,
        PERFORM
    }
    public static class MaterialSnapshot {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class SpecifyMTSProductSpecSnapshot {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
    private MaterialType materialType;
    private String materialId;
    private MaterialSnapshot materialSnapshot;
    private String specifyMTSProductSpecId;
    private SpecifyMTSProductSpecSnapshot specifyMTSProductSpecSnapshot;
    private Size3D usageSize3D;

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public MaterialSnapshot getMaterialSnapshot() {
        return materialSnapshot;
    }

    public void setMaterialSnapshot(MaterialSnapshot materialSnapshot) {
        this.materialSnapshot = materialSnapshot;
    }

    public String getSpecifyMTSProductSpecId() {
        return specifyMTSProductSpecId;
    }

    public void setSpecifyMTSProductSpecId(String specifyMTSProductSpecId) {
        this.specifyMTSProductSpecId = specifyMTSProductSpecId;
    }

    public SpecifyMTSProductSpecSnapshot getSpecifyMTSProductSpecSnapshot() {
        return specifyMTSProductSpecSnapshot;
    }

    public void setSpecifyMTSProductSpecSnapshot(SpecifyMTSProductSpecSnapshot specifyMTSProductSpecSnapshot) {
        this.specifyMTSProductSpecSnapshot = specifyMTSProductSpecSnapshot;
    }

    public Size3D getUsageSize3D() {
        return usageSize3D;
    }

    public void setUsageSize3D(Size3D usageSize3D) {
        this.usageSize3D = usageSize3D;
    }
}
