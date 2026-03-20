package com.mes.application.command.orderPreprocessing.vo;

import lombok.Data;

@Data
public class PltApiResponse {
    private String pltFileUrl;
    private String pltFileName;
    private Long fileSize;
    private String code;
    private String msg;


}
