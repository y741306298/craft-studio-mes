package com.mes.application.command.graphics;

import com.mes.infra.dal.graphics.GraphicsCallbackInbox;
import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GraphicsCallbackServiceTest {
    private byte[] payload(){return "{\"id\":\"task\",\"status\":\"success\",\"result\":{\"taskId\":\"task\",\"artifact\":{\"objectName\":\"imgutil/test.png\"}},\"callbackCustomValue\":{\"workflowId\":\"workflow\",\"taskId\":\"task\"}}".getBytes(StandardCharsets.UTF_8);}
    private String sign(byte[] body)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(body));}
    @Test void verifiesSignaturePersistsBeforeWakingWorkerAndAllowsDuplicate()throws Exception{
        var inbox=mock(GraphicsCallbackInbox.class);var notifier=mock(GraphicsCallbackNotifier.class);var service=new GraphicsCallbackService(inbox,notifier,"secret");
        byte[] body=payload();service.accept(body,sign(body));service.accept(body,sign(body));
        var order=inOrder(inbox,notifier);order.verify(inbox).accept(eq("task"),eq("workflow"),anyString());order.verify(notifier).publish("task");
        order.verify(inbox).accept(eq("task"),eq("workflow"),anyString());order.verify(notifier).publish("task");
    }
    @Test void rejectsUnsignedAndTamperedCallbacksBeforeWriting()throws Exception{
        var inbox=mock(GraphicsCallbackInbox.class);var notifier=mock(GraphicsCallbackNotifier.class);var service=new GraphicsCallbackService(inbox,notifier,"secret");
        assertThrows(GraphicsWorkflowException.class,()->service.accept(payload(),null));
        byte[] altered=new String(payload(),StandardCharsets.UTF_8).replace("workflow","different").getBytes(StandardCharsets.UTF_8);
        assertThrows(GraphicsWorkflowException.class,()->service.accept(altered,sign(payload())));verifyNoInteractions(inbox,notifier);
    }
}
