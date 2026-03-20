package com.mes.application.command.typesetting.vo;

import lombok.Data;

import java.util.List;

@Data
public class ConfirmPrintResult {
    private boolean success;
    private String message;
    private Integer updatedPieceCount;
    private List<String> updatedPieceIds;

}
