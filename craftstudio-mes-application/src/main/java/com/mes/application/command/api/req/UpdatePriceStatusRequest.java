package com.mes.application.command.api.req;

import com.mes.domain.base.UnitPrice;

public class UpdatePriceStatusRequest {
    private UnitPrice price;
    private String status;

    public UnitPrice getPrice() { return price; }
    public void setPrice(UnitPrice price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}