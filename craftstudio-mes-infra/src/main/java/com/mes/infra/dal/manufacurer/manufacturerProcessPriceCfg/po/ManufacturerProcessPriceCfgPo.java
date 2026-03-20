package com.mes.infra.dal.manufacurer.manufacturerProcessPriceCfg.po;

import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity.ManufacturerProcessPriceCfg;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "manufacturerProcessPriceCfg")
public class ManufacturerProcessPriceCfgPo extends BasePO<ManufacturerProcessPriceCfg> {

    private String processId;
    private String manufacturerId;
    private Double processPricePrice;
    private String processPriceUnit;
    private Double basePrice;
    private String processName;
    private String processCode;
    private String processDescription;
    private String processType;
    private String capacity;
    private String unit;
    private String procedureFlowId;
    private String procedureFlowName;
    private String flowDescription;
    private String flowStatus;
    private String materials;
    private String status;
    private String materialProcessPricesJson;

    @Override
    public ManufacturerProcessPriceCfg toDO() {
        ManufacturerProcessPriceCfg cfg = new ManufacturerProcessPriceCfg();
        copyBaseFieldsToDO(cfg);
        cfg.setProcessId(this.processId);
        cfg.setManufacturerId(this.manufacturerId);
        if (this.processPricePrice != null) {
            cfg.setProcessPrice(new com.mes.domain.base.UnitPrice());
            cfg.getProcessPrice().setPrice(this.processPricePrice);
            cfg.getProcessPrice().setUnit(this.processPriceUnit);
        }
        cfg.setBasePrice(this.basePrice);
        cfg.setProcessName(this.processName);
        cfg.setProcessCode(this.processCode);
        cfg.setProcessDescription(this.processDescription);
        cfg.setProcessType(this.processType);
        cfg.setCapacity(this.capacity);
        cfg.setUnit(this.unit);
        if (this.procedureFlowId != null) {
            com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow flow = 
                    new com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow();
            flow.setId(this.procedureFlowId);
            flow.setProcedureFlowName(this.procedureFlowName);
            flow.setFlowDescription(this.flowDescription);
            if (this.flowStatus != null) {
                for (com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus status : 
                     com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus.values()) {
                    if (status.getDescription().equals(this.flowStatus)) {
                        flow.setFlowStatus(status);
                        break;
                    }
                }
            }
            cfg.setProcedureFlow(flow);
        }
        cfg.setMaterials(this.materials);
        if (this.status != null) {
            cfg.setStatus(com.mes.domain.manufacturer.enums.CfgStatus.getByCode(this.status));
        }
        return cfg;
    }

    @Override
    protected BasePO<ManufacturerProcessPriceCfg> fromDO(ManufacturerProcessPriceCfg _do) {
        this.processId = _do.getProcessId();
        this.manufacturerId = _do.getManufacturerId();
        if (_do.getProcessPrice() != null) {
            this.processPricePrice = _do.getProcessPrice().getPrice();
            this.processPriceUnit = _do.getProcessPrice().getUnit();
        }
        this.basePrice = _do.getBasePrice();
        this.processName = _do.getProcessName();
        this.processCode = _do.getProcessCode();
        this.processDescription = _do.getProcessDescription();
        this.processType = _do.getProcessType();
        this.capacity = _do.getCapacity();
        this.unit = _do.getUnit();
        if (_do.getProcedureFlow() != null) {
            this.procedureFlowId = _do.getProcedureFlow().getId();
            this.procedureFlowName = _do.getProcedureFlow().getProcedureFlowName();
            this.flowDescription = _do.getProcedureFlow().getFlowDescription();
            if (_do.getProcedureFlow().getFlowStatus() != null) {
                this.flowStatus = _do.getProcedureFlow().getFlowStatus().getDescription();
            }
        }
        this.materials = _do.getMaterials();
        if (_do.getStatus() != null) {
            this.status = _do.getStatus().getCode();
        }
        return this;
    }
}
