package com.mes.infra.dal.delivery.deliveryRoute.po;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
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
    private List<DeliveryRouteNode> routeNodes;
    private String status;

    @Override
    public DeliveryRoute toDO() {
        DeliveryRoute deliveryRoute = new DeliveryRoute();
        copyBaseFieldsToDO(deliveryRoute);
        
        deliveryRoute.setRouteId(this.routeId);
        deliveryRoute.setRouteName(this.routeName);
        deliveryRoute.setDeliveryRouteNodes(this.routeNodes);
        deliveryRoute.setStatus(this.status);
        return deliveryRoute;
    }

    @Override
    protected BasePO<DeliveryRoute> fromDO(DeliveryRoute _do) {
        if (_do == null) {
            return null;
        }
        this.routeId = _do.getRouteId();
        this.routeName = _do.getRouteName();
        this.routeNodes = _do.getDeliveryRouteNodes();
        this.status = _do.getStatus();
        return this;
    }
}
