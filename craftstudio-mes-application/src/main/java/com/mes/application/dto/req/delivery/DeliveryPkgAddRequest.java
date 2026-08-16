package com.mes.application.dto.req.delivery;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryPkgAddRequest {

    private List<DeliveryPkgPieceItem> pieces;
    private String deliveryManId;
    private String deliverySiidId;
    /** 快递100云打印设备编码，保存为包裹默认重打设备 */
    private String siid;
    private String carrierId;
    private String manufacturerMetaId;
    private String routeId;
    private String routeNodeId;

    @Data
    public static class DeliveryPkgPieceItem {
        private DeliveryPkgPieceVO piece;
        private Integer quantity;
    }
}
