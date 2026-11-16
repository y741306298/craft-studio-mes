package com.mes.interfaces.api.platform.manufacturerSide.graphics;

import com.mes.application.command.graphics.*;
import com.mes.domain.base.repository.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/manufacturerSide/graphics/callback")
public class GraphicsCallbackController {
    private final GraphicsCallbackService callbacks;
    public GraphicsCallbackController(GraphicsCallbackService callbacks){this.callbacks=callbacks;}
    @PostMapping("/generate_graphics")
    public ApiResponse<String> complete(HttpServletRequest request)throws IOException {
        byte[] body=request.getInputStream().readNBytes(65537);
        if(body.length>65536)throw new GraphicsWorkflowException(413,"图形回调过大");
        callbacks.accept(body,request.getHeader("X-Graphics-Signature"));return ApiResponse.success();
    }
}
