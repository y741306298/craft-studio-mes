package com.mes.domain.order.orderInfo.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.vo.ManufacturerMtsProductSpec;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.order.enums.OrderStatus;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderItem extends BaseEntity {

    private String orderItemId;
    private String orderId;
    private MTOProductSpecDTO mtoProduct;
    private String material;
    private ProcedureFlow procedureFlow;
    private Integer quantity;
    private OrderStatus status;
    private Boolean isUrgent;
    private String processingFlow;
    private ImageFile productionImgFile;
    private ImageFile maskImgFile;
    private String failureReason;

}
