package com.mes.application.command.typesetting.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReleaseLayoutResult {
    private boolean success;
    private String message;
    private Integer releasedPieceCount;
    private List<String> releasedPieceIds;
    private List<String> deletedLayoutIds;

}
