package com.mes.infra.dal.delivery.deliveryRoute.po;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNodeBinding;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "deliveryRouteNodeBinding")
public class DeliveryRouteNodeBindingPo extends BasePO<DeliveryRouteNodeBinding> {

    private String manufacturerMetaId;
    private String terminalRegionCode;
    private String detailAddress;
    private String routeNodeId;

    @Override
    public DeliveryRouteNodeBinding toDO() {
        DeliveryRouteNodeBinding binding = new DeliveryRouteNodeBinding();
        copyBaseFieldsToDO(binding);
        binding.setManufacturerMetaId(this.manufacturerMetaId);
        binding.setTerminalRegionCode(this.terminalRegionCode);
        binding.setDetailAddress(this.detailAddress);
        binding.setRouteNodeId(this.routeNodeId);
        return binding;
    }

    @Override
    protected BasePO<DeliveryRouteNodeBinding> fromDO(DeliveryRouteNodeBinding _do) {
        if (_do == null) {
            return null;
        }
        this.manufacturerMetaId = _do.getManufacturerMetaId();
        this.terminalRegionCode = _do.getTerminalRegionCode();
        this.detailAddress = _do.getDetailAddress();
        this.routeNodeId = _do.getRouteNodeId();
        return this;
    }
}
