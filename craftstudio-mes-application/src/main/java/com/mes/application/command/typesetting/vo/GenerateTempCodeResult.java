package com.mes.application.command.typesetting.vo;

import lombok.Data;

@Data
public class GenerateTempCodeResult {
    private String manufacturerMetaId;
    private Long codeNumber;
    private String tempCode;
}
