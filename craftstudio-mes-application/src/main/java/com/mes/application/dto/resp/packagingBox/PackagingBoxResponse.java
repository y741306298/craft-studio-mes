package com.mes.application.dto.resp.packagingBox;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mes.domain.delivery.packagingBox.entity.PackagingBox;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PackagingBoxResponse {
    private String id;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    private String boxName;
    private String code;
    private BigDecimal cardboardDepth;
    private BigDecimal cardboardHeight;
    private BigDecimal cardboardWidth;
    private BigDecimal boxLength;
    private BigDecimal boxWidth;
    private BigDecimal height;
    private String cuttingKnife;
    private String rollingCutter;
    private String grindingKnife;

    public static PackagingBoxResponse from(PackagingBox box) {
        PackagingBoxResponse response = new PackagingBoxResponse();
        response.setId(box.getId());
        response.setCreateTime(box.getCreateTime());
        response.setUpdateTime(box.getUpdateTime());
        response.setBoxName(box.getBoxName());
        response.setCode(box.getCode());
        response.setCardboardDepth(box.getCardboardDepth());
        response.setCardboardHeight(box.getCardboardHeight());
        response.setCardboardWidth(box.getCardboardWidth());
        response.setBoxLength(box.getBoxLength());
        response.setBoxWidth(box.getBoxWidth());
        response.setHeight(box.getHeight());
        response.setCuttingKnife(box.getCuttingKnife());
        response.setRollingCutter(box.getRollingCutter());
        response.setGrindingKnife(box.getGrindingKnife());
        return response;
    }
}
