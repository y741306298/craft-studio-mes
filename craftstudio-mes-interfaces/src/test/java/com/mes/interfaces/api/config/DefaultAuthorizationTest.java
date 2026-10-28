package com.mes.interfaces.api.config;

import com.mes.application.command.auth.AppConfigLoginService;
import com.mes.application.command.auth.AppLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultAuthorizationTest {

    @Test
    void matchingAuthorizationBypassesLoginServicesOnBothSides() throws Exception {
        AppLoginService manufacturerLogin = mock(AppLoginService.class);
        AppConfigLoginService configLogin = mock(AppConfigLoginService.class);
        for (HandlerInterceptor interceptor : new HandlerInterceptor[]{
                new ManufacturerSideAuthInterceptor(manufacturerLogin),
                new ConfigSideAuthInterceptor(configLogin)}) {
            ReflectionTestUtils.setField(interceptor, "defaultAuthorization", "Bearer test-default");
            assertTrue(interceptor.preHandle(request("Bearer test-default"), new MockHttpServletResponse(), new Object()));
        }
        verifyNoInteractions(manufacturerLogin, configLogin);
    }

    @Test
    void nonMatchingBearerContinuesNormalLoginValidation() throws Exception {
        AppLoginService manufacturerLogin = mock(AppLoginService.class);
        AppConfigLoginService configLogin = mock(AppConfigLoginService.class);
        ManufacturerSideAuthInterceptor manufacturer = new ManufacturerSideAuthInterceptor(manufacturerLogin);
        ConfigSideAuthInterceptor config = new ConfigSideAuthInterceptor(configLogin);
        for (HandlerInterceptor interceptor : new HandlerInterceptor[]{manufacturer, config}) {
            ReflectionTestUtils.setField(interceptor, "defaultAuthorization", "Bearer test-default");
            assertTrue(interceptor.preHandle(request("Bearer login-token"), new MockHttpServletResponse(), new Object()));
        }
        verify(manufacturerLogin).getManufacturerMetaIdByToken("login-token");
        verify(configLogin).getConfigUserIdByToken("login-token");
    }

    @Test
    void missingMalformedAndDisabledAuthorizationDoNotBypassAuthentication() throws Exception {
        for (HandlerInterceptor interceptor : new HandlerInterceptor[]{
                new ManufacturerSideAuthInterceptor(mock(AppLoginService.class)),
                new ConfigSideAuthInterceptor(mock(AppConfigLoginService.class))}) {
            for (String configured : new String[]{"Bearer test-default", "", " "}) {
                ReflectionTestUtils.setField(interceptor, "defaultAuthorization", configured);
                for (String header : new String[]{null, "", " ", "test-default", "Bearer "}) {
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    assertFalse(interceptor.preHandle(request(header), response, new Object()));
                    assertEquals(401, response.getStatus());
                }
            }
        }
    }

    private MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
