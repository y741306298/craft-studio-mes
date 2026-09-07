package com.mes.application.command.delivery;

import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AppDeliveryPkgServiceTest {

    private final AppDeliveryPkgService service = new AppDeliveryPkgService();

    @Test
    void usesDefaultRecipientNameWhenCustomerIsMissing() {
        assertThat(resolveRecipientName(null)).isEqualTo("神秘人");
    }

    @Test
    void usesDefaultRecipientNameWhenCustomerNameIsBlank() {
        OrderCustomer customer = new OrderCustomer();
        customer.setCustomerName(" ");

        assertThat(resolveRecipientName(customer)).isEqualTo("神秘人");
    }

    @Test
    void keepsProvidedCustomerName() {
        OrderCustomer customer = new OrderCustomer();
        customer.setCustomerName("张三");

        assertThat(resolveRecipientName(customer)).isEqualTo("张三");
    }

    private String resolveRecipientName(OrderCustomer customer) {
        return ReflectionTestUtils.invokeMethod(service, "resolveRecipientName", customer);
    }
}
