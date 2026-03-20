package com.mes.application.command.transBox.vo;

import lombok.Data;

@Data
public class StorageInResult {
    private boolean success;
    private String message;
    private String storageTankId;
    private String storageTankName;
    private String slotId;
    private String slotCode;
    private String productionPieceId;
    private Integer quantity;
    private String recordId;
}
