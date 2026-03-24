package com.mes.infra.dal.order.po;

import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "orderItem")
public class OrderItemPo extends BasePO<OrderItem> {

    private String orderItemId;
    private String orderId;
    private MTOProductSpecDTO mtoProduct;
    private String material;
    private String procedureFlowId;
    private Integer quantity;
    private String status;
    private Boolean isUrgent;
    private String processingFlow;
    private ImageFile productionImgFile;
    private ImageFile maskImgFile;
    private String failureReason;

    @Override
    public OrderItem toDO() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(getId());
        orderItem.setCreateTime(getCreateTime());
        orderItem.setUpdateTime(getUpdateTime());

        orderItem.setOrderItemId(this.orderItemId);
        orderItem.setOrderId(this.orderId);
        orderItem.setMtoProduct(this.mtoProduct);
        orderItem.setMaterial(this.material);
        orderItem.setQuantity(this.quantity);
        if (this.status != null) {
            orderItem.setStatus(OrderStatus.getByCode(this.status));
        }
        orderItem.setIsUrgent(this.isUrgent);
        orderItem.setProcessingFlow(this.processingFlow);
        orderItem.setProductionImgFile(this.productionImgFile);
        orderItem.setMaskImgFile(this.maskImgFile);
        orderItem.setFailureReason(this.failureReason);

        return orderItem;
    }

    @Override
    protected BasePO<OrderItem> fromDO(OrderItem _do) {
        this.orderItemId = _do.getOrderItemId();
        this.orderId = _do.getOrderId();
        this.mtoProduct = _do.getMtoProduct();
        this.material = _do.getMaterial();
        this.quantity = _do.getQuantity();
        this.status = _do.getStatus() != null ? _do.getStatus().getCode() : null;
        this.isUrgent = _do.getIsUrgent();
        this.processingFlow = _do.getProcessingFlow();
        this.productionImgFile = _do.getProductionImgFile();
        this.maskImgFile = _do.getMaskImgFile();
        this.failureReason = _do.getFailureReason();
        return this;
    }
}
