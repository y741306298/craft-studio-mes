package com.mes.application.command.api.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Graphics correlation extends the existing callback convention without changing other algorithm payloads. */
@Data
@EqualsAndHashCode(callSuper=true)
public class GraphicsCallbackCustomValue extends CallbackCustomValue {
    private String workflowId;
    private String taskId;
}
