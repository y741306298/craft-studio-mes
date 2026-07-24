package com.piliofpala.craftstudio.pangolin.domain.logistics.vo;

import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Consignee;

public class LogisticsLabel {
    private String logisticsOrderId;
    private Consignee consignee;
    private LogisticsCloudPrintData logisticsCloudPrintData;

    public String getLogisticsOrderId() {
        return logisticsOrderId;
    }

    public void setLogisticsOrderId(String logisticsOrderId) {
        this.logisticsOrderId = logisticsOrderId;
    }

    public Consignee getConsignee() {
        return consignee;
    }

    public void setConsignee(Consignee consignee) {
        this.consignee = consignee;
    }

    public LogisticsCloudPrintData getLogisticsCloudPrintData() {
        return logisticsCloudPrintData;
    }

    public void setLogisticsCloudPrintData(LogisticsCloudPrintData logisticsCloudPrintData) {
        this.logisticsCloudPrintData = logisticsCloudPrintData;
    }
}
