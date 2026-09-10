package com.mes.infra.dal.manufacurer.manufacturerMaterialLayoutSpecCfg;

import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.repository.ManufacturerMaterialLayoutSpecCfgRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.manufacturerMaterialLayoutSpecCfg.po.ManufacturerMaterialLayoutSpecCfgPo;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ManufacturerMaterialLayoutSpecCfgRepositoryImp
        extends BaseRepositoryImp<ManufacturerMaterialLayoutSpecCfg, ManufacturerMaterialLayoutSpecCfgPo>
        implements ManufacturerMaterialLayoutSpecCfgRepository {

    @Override
    public Class<ManufacturerMaterialLayoutSpecCfgPo> poClass() {
        return ManufacturerMaterialLayoutSpecCfgPo.class;
    }

    @Override
    public long migrateInsetCmToMm() {
        Document legacyInsetExists = new Document("insetSteps.insetCm", new Document("$exists", true));
        Document migrateEachStep = new Document("$map", new Document("input", "$insetSteps")
                .append("as", "step")
                .append("in", new Document("maxLengthMeter", "$$step.maxLengthMeter")
                        .append("insetMm", new Document("$multiply", List.of("$$step.insetCm", 10)))));
        Document migrateInsetSteps = new Document("$set", new Document("insetSteps", migrateEachStep));
        return mongoTemplate.getCollection("manufacturer_material_layout_spec_cfg")
                .updateMany(legacyInsetExists, List.of(migrateInsetSteps))
                .getModifiedCount();
    }
}
