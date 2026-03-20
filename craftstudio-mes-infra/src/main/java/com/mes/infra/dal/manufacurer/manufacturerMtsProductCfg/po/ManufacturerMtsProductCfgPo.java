package com.mes.infra.dal.manufacurer.manufacturerMtsProductCfg.po;

import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity.ManufacturerMtsProductCfg;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.vo.ManufacturerMtsProductSpec;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "manufacturerMtsProductCfg")
public class ManufacturerMtsProductCfgPo extends BasePO<ManufacturerMtsProductCfg> {

    private String manufacturerId;
    private String productId;
    private String productName;
    private String productPreviewUrl;
    private List<ManufacturerMtsProductSpec> mtsProductSpecs;
    private CfgStatus status;

    @Override
    public ManufacturerMtsProductCfg toDO() {
        ManufacturerMtsProductCfg cfg = new ManufacturerMtsProductCfg();
        copyBaseFieldsToDO(cfg);
        cfg.setManufacturerId(this.manufacturerId);
        cfg.setProductId(this.productId);
        cfg.setProductName(this.productName);
        cfg.setProductPreviewUrl(this.productPreviewUrl);
        cfg.setMtsProductSpecs(this.mtsProductSpecs);
        cfg.setStatus(this.status);
        return cfg;
    }

    @Override
    protected BasePO<ManufacturerMtsProductCfg> fromDO(ManufacturerMtsProductCfg _do) {
        this.manufacturerId = _do.getManufacturerId();
        this.productId = _do.getProductId();
        this.productName = _do.getProductName();
        this.productPreviewUrl = _do.getProductPreviewUrl();
        this.mtsProductSpecs = _do.getMtsProductSpecs();
        this.status = _do.getStatus();
        return this;
    }
}
