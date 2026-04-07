package com.mes.application.command.order.vo;

import com.mes.domain.order.enums.OrderStatus;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import lombok.Data;

import java.util.Date;

@Data
public class OrderQuery {
    private String id;
    private String orderId;
    private String manufacturerId;
    private OrderStatus status;
    private Date startTime;
    private Date endTime;
    private String customerPhone;
    private PagedQuery pagedQuery;
}
