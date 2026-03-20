package com.mes.application.command.orderPreprocessing.vo;

import lombok.Data;

@Data
public class CutResult {
    private String imageUrl;
    private Double x;
    private Double y;
    private Double width;
    private Double height;

}
