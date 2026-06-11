package com.mes.infra.dal.delivery.deliveryRoute.po;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.RouteNode;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "deliveryRoute")
public class DeliveryRoutePo extends BasePO<DeliveryRoute> {
    
    private String routeId;
    private String routeName;
    private String manufacturerMetaId;
    private List<RouteNode> routeNodes;
    private String status;

    @Override
    public DeliveryRoute toDO() {
        DeliveryRoute deliveryRoute = new DeliveryRoute();
        copyBaseFieldsToDO(deliveryRoute);
        
        deliveryRoute.setRouteId(this.routeId);
        deliveryRoute.setRouteName(this.routeName);
        deliveryRoute.setStatus(this.status);
        deliveryRoute.setManufacturerMetaId(this.manufacturerMetaId);
        deliveryRoute.setRouteNodes(this.routeNodes);
        return deliveryRoute;
    }

    @Override
    protected BasePO<DeliveryRoute> fromDO(DeliveryRoute _do) {
        if (_do == null) {
            return null;
        }
        this.routeId = _do.getRouteId();
        this.routeName = _do.getRouteName();
        this.status = _do.getStatus();
        this.manufacturerMetaId = _do.getManufacturerMetaId();
        this.routeNodes = _do.getRouteNodes();
        return this;
    }
}
