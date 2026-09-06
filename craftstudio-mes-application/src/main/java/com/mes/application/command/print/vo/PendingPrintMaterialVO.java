package com.mes.application.command.print.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingPrintMaterialVO {
    private String materialId;
    private String materialName;
}
