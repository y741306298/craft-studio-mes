package com.mes.application.command.typesetting.vo;

import lombok.Data;

import java.util.List;

@Data
public class ConfirmPrintResult {
    private String graphicsTaskId;
    /** Whether a durable workflow was accepted; inspect status for the final outcome. */
    private Boolean accepted;
    private String status;
    private boolean success;
    private String message;
    private Integer updatedPieceCount;
    private List<String> updatedPieceIds;

}
