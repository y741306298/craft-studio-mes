package com.mes.domain.manufacturer.typesetting.vo;

import lombok.Data;

import java.util.List;

@Data
public class TypesettingDownloadTaskData {
    private String id;
    /**
     * 兼容下游既有字段名（历史拼写）。
     */
    private List<String> imamges;
    private List<String> plts;
    private List<String> jsons;
    private String deviceCode;
}
