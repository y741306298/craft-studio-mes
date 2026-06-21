package com.mes.domain.order.orderInfo.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderItemPriceInfo;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderItem extends BaseEntity {

    private String orderItemId;
    private String orderId;
    private String manufacturerId;
    private MTOProductSpecDTO mtoProduct;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private MaterialConfig material;
    private ProcedureFlow procedureFlow;
    private Integer quantity;
    private OrderStatus status;
    private Boolean isUrgent;
    private String processingFlow;
    private ImageFile productionImgFile;
    private ImageFile maskImgFile;
    private String failureReason;
    /**
     * 当前订单项正在等待的预处理请求 ID，用于区分重做前后的异步算法回调。
     */
    private String preprocessRequestId;
    private String kuaidiWay;
    private String kuaidiNum;
    private String routeId;
    private String routeNodeId;
    private List<ProductionPiece> productionPieces;
    private OrderItemPriceInfo price;

}
