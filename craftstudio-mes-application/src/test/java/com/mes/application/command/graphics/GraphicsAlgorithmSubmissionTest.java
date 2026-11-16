package com.mes.application.command.graphics;

import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.GraphicsGenerationRequest;
import com.mes.application.command.api.vo.*;
import com.mes.domain.shared.algorithm.entity.AlgorithmCoreApiCallRecord;
import com.mes.domain.shared.algorithm.repository.AlgorithmCoreApiCallRecordRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GraphicsAlgorithmSubmissionTest {
    @Test void sharesAlgorithmHeadersLogRecordAndSupportsEmpty202(){
        var http=new RestTemplate();var server=MockRestServiceServer.bindTo(http).build();var records=mock(AlgorithmCoreApiCallRecordRepository.class);
        var algorithms=new AlgorithmCoreApiService();ReflectionTestUtils.setField(algorithms,"restTemplate",http);ReflectionTestUtils.setField(algorithms,"algorithmCoreApiCallRecordRepository",records);
        ReflectionTestUtils.setField(algorithms,"generateGraphicsUrl","http://imgutil/api/graphics/tasks");ReflectionTestUtils.setField(algorithms,"graphicsApiKey","header-only-secret");
        var request=new GraphicsGenerationRequest();request.setRequestId("task-1");request.setMes(Map.of("operation","BLACK","parameters",Map.of("width",8,"height",3)));
        var custom=new GraphicsCallbackCustomValue();custom.setWorkflowId("workflow-1");custom.setTaskId("task-1");custom.setId("workflow-1");
        var callback=new CallbackConfig();callback.setCallbackUrl("http://mes/callback");callback.setCallbackCustomValue(custom);request.setCallbackConfig(callback);
        server.expect(requestTo("http://imgutil/api/graphics/tasks")).andExpect(method(HttpMethod.POST)).andExpect(header("X-Fc-Invocation-Type","Async"))
                .andExpect(header("X-Api-Key","header-only-secret")).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andRespond(withStatus(HttpStatus.ACCEPTED));
        assertNull(algorithms.generateGraphicsAsync(request));server.verify();
        var capture=ArgumentCaptor.forClass(AlgorithmCoreApiCallRecord.class);verify(records).add(capture.capture());
        assertEquals("generateGraphicsAsync",capture.getValue().getType());assertEquals("workflow-1",capture.getValue().getSourceId());assertEquals("async",capture.getValue().getMode());
        assertFalse(capture.getValue().getRequestBody().contains("header-only-secret"));assertTrue(capture.getValue().getRequestBody().contains("callbackConfig"));
    }
}
