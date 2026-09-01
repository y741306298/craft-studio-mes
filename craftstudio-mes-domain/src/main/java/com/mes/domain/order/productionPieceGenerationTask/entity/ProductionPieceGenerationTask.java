package com.mes.domain.order.productionPieceGenerationTask.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/** Tracks the order items whose production pieces have not finished generating. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionPieceGenerationTask extends BaseEntity {
    private String orderId;
    private List<String> orderItemIdList = new ArrayList<>();
}
