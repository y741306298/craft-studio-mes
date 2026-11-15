package com.mes.application.command.graphics;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ImgUtilClientTest {
    @Test void submitsAsynchronouslyPollsAndReturnsOssReference(){
        var client=new ImgUtilClient("http://imgutil","key","https://bucket.example.com",2000,100);
        var server=MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(client,"http")).build();
        server.expect(requestTo("http://imgutil/api/graphics/tasks")).andExpect(method(HttpMethod.POST)).andExpect(header("X-Api-Key","key")).andRespond(withStatus(HttpStatus.ACCEPTED).body("{}"));
        server.expect(anything()).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{\"data\":{\"status\":\"QUEUED\"}}",MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withSuccess("{\"data\":{\"taskId\":\"remote\",\"status\":\"SUCCEEDED\",\"artifact\":{\"objectName\":\"imgutil/result.png\",\"width\":8,\"height\":3}}}",MediaType.APPLICATION_JSON));
        var image=client.render(UUID.randomUUID().toString(),"BLACK",Map.of("width",8,"height",3));
        assertEquals("https://bucket.example.com/imgutil/result.png",image.url());assertEquals(8,image.getWidth());assertEquals(3,image.getHeight());server.verify();
    }
    @Test void remoteFailureIsNotReplacedByLocalRendering(){
        var client=new ImgUtilClient("http://imgutil","key","https://bucket.example.com",2000,100);
        var server=MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(client,"http")).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.ACCEPTED).body("{}"));
        server.expect(anything()).andRespond(withSuccess("{\"data\":{\"status\":\"FAILED\",\"errorCode\":\"INVALID_GRAPHICS\",\"message\":\"too big\"}}",MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class,()->client.render(UUID.randomUUID().toString(),"BLACK",Map.of("width",8,"height",3)));server.verify();
    }
    @Test void renderingOutsideWorkflowIsForbidden(){assertThrows(IllegalStateException.class,()->RemoteGraphics.png("BLACK",Map.of()));}
}
