package com.mes.infra.dal.order.po;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.mes.infra.base.BasePO;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "orderItem")
public class OrderItemPo extends BasePO<OrderItem> {

    private String orderItemId;
    private String orderId;
    private String manufacturerId;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private MTOProductSpecDTO mtoProduct;
    private MaterialConfig material;
    private ProcedureFlow procedureFlow;
    private Integer quantity;
    private String status;
    private Boolean isUrgent;
    private String processingFlow;
    private ImageFile productionImgFile;
    private ImageFile maskImgFile;
    private String failureReason;
    private String kuaidiWay;
    private String kuaidiNum;

    @Override
    public OrderItem toDO() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(getId());
        orderItem.setCreateTime(getCreateTime());
        orderItem.setUpdateTime(getUpdateTime());

        orderItem.setOrderItemId(this.orderItemId);
        orderItem.setOrderId(this.orderId);
        orderItem.setManufacturerId(this.manufacturerId);
        orderItem.setLogisticsCarrierInfo(this.logisticsCarrierInfo);
        orderItem.setMtoProduct(this.mtoProduct);
        orderItem.setMaterial(this.material);
        orderItem.setQuantity(this.quantity);
        orderItem.setProcedureFlow(this.procedureFlow);
        if (this.status != null) {
            orderItem.setStatus(OrderStatus.getByCode(this.status));
        }
        orderItem.setIsUrgent(this.isUrgent);
        orderItem.setProcessingFlow(this.processingFlow);
        orderItem.setProductionImgFile(this.productionImgFile);
        orderItem.setMaskImgFile(this.maskImgFile);
        orderItem.setFailureReason(this.failureReason);
        orderItem.setKuaidiWay(this.kuaidiWay);
        orderItem.setKuaidiNum(this.kuaidiNum);

        return orderItem;
    }

    @Override
    protected BasePO<OrderItem> fromDO(OrderItem _do) {
        this.orderItemId = _do.getOrderItemId();
        this.orderId = _do.getOrderId();
        this.manufacturerId = _do.getManufacturerId();
        this.logisticsCarrierInfo = _do.getLogisticsCarrierInfo();
        this.mtoProduct = _do.getMtoProduct();
        this.material = _do.getMaterial();
        this.quantity = _do.getQuantity();
        this.status = _do.getStatus() != null ? _do.getStatus().getCode() : null;
        this.isUrgent = _do.getIsUrgent();
        this.processingFlow = _do.getProcessingFlow();
        this.productionImgFile = _do.getProductionImgFile();
        this.maskImgFile = _do.getMaskImgFile();
        this.failureReason = _do.getFailureReason();
        this.procedureFlow = _do.getProcedureFlow();
        this.kuaidiWay = _do.getKuaidiWay();
        this.kuaidiNum = _do.getKuaidiNum();
        return this;
    }
}
