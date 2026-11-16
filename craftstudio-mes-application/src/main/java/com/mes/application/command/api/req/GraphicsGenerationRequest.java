package com.mes.application.command.api.req;

import com.mes.application.command.api.vo.CallbackConfig;
import lombok.Data;
import java.util.Map;

@Data
public class GraphicsGenerationRequest {
    private String requestId;
    private Map<String,Object> mes;
    private CallbackConfig callbackConfig;
}
