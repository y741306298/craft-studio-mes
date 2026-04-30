package com.mes.application.dto.req.typesetting;

import lombok.Data;

import java.util.List;

@Data
public class BatchConfirmPrintRequest {
    private List<ConfirmPrintRequest> requests;
}
