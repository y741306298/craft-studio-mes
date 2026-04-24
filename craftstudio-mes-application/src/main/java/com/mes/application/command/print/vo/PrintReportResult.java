package com.mes.application.command.print.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrintReportResult {
    private boolean completed;
    private int transferRecordCount;
}
