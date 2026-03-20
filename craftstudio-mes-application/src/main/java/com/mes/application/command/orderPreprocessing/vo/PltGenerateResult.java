package com.mes.application.command.orderPreprocessing.vo;

import lombok.Data;

@Data
public class PltGenerateResult {
    private boolean success;
    private String pltFileUrl;
    private String pltFileName;
    private Long fileSize;
    private String message;


}
