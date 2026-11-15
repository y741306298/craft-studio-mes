package com.mes.application.command.typesetting.vo;

import lombok.Data;

@Data
public class LayoutConfirmResult {
    /** 接收成功并不代表图形、排版或制版已经完成。 */
    private String graphicsTaskId;
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
