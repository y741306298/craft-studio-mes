package com.mes.application.command.delivery;

import com.mes.domain.delivery.deliveryPkg.vo.AuthOrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppDeliveryPkgServiceKuaidi100ResponseTest {
    private final AppDeliveryPkgService service = new AppDeliveryPkgService();

    @Test
    void preservesOriginalErrorForFailedResponse() {
        AuthOrderResponse response = response(false, 500, null, "电子面单余额不足");
        assertFailure(response, "电子面单余额不足");
    }

    @Test
    void preservesOriginalMessageWhenSuccessfulResponseHasNoWaybill() {
        AuthOrderResponse response = response(true, 200, " ", "该网点未开通电子面单服务");
        assertFailure(response, "该网点未开通电子面单服务");
    }

    @Test
    void preservesOriginalMessageWhenResponseHasNoData() {
        AuthOrderResponse response = response(true, 200, null, "月结账号无效");
        response.setData(null);
        assertFailure(response, "月结账号无效");
    }

    @Test
    void usesFallbackOnlyWhenOriginalMessageIsMissing() {
        assertFailure(response(true, 200, null, " "), "快递100打印未返回快递单号");
        assertFailure(null, "快递100电子面单下单失败");
    }

    @Test
    void acceptsSuccessfulResponseWithWaybill() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service,
                "validateKuaidi100OrderResponse", response(true, 200, "SF123", "成功")));
    }

    private void assertFailure(AuthOrderResponse response, String message) {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateKuaidi100OrderResponse", response));
        assertEquals(message, exception.getMessage());
    }

    private AuthOrderResponse response(boolean success, int code, String waybill, String message) {
        AuthOrderResponse response = new AuthOrderResponse();
        response.setSuccess(success);
        response.setCode(code);
        response.setMessage(message);
        AuthOrderResponse.OrderResult data = response.new OrderResult();
        data.setKuaidinum(waybill);
        response.setData(data);
        return response;
    }
}
