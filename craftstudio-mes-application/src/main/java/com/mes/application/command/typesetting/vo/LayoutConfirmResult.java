package com.mes.application.command.typesetting.vo;

import lombok.Data;

@Data
public class LayoutConfirmResult {
    private boolean success;
    private String layoutId;
    private String layoutUrl;
    private Integer productionPieceCount;
    private String message;

    public static LayoutConfirmResult failed(String message) {
        LayoutConfirmResult result = new LayoutConfirmResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

}
