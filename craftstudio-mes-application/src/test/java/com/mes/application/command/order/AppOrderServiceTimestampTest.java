package com.mes.application.command.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppOrderServiceTimestampTest {

    @Test
    void shouldReplaceRequestTimestampsWithOneServerTimestamp() {
        Date requestTime = new Date(0);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateTime(requestTime);
        orderInfo.setUpdateTime(requestTime);
        OrderItem firstItem = new OrderItem();
        firstItem.setCreateTime(requestTime);
        firstItem.setUpdateTime(requestTime);
        OrderItem secondItem = new OrderItem();
        secondItem.setCreateTime(requestTime);
        secondItem.setUpdateTime(requestTime);
        long before = System.currentTimeMillis();

        AppOrderService.applyCurrentBeijingTimestamps(orderInfo, List.of(firstItem, secondItem));

        long after = System.currentTimeMillis();
        assertNotEquals(requestTime, orderInfo.getCreateTime());
        assertTrue(orderInfo.getCreateTime().getTime() >= before);
        assertTrue(orderInfo.getCreateTime().getTime() <= after);
        assertEquals(orderInfo.getCreateTime(), orderInfo.getUpdateTime());
        assertEquals(orderInfo.getCreateTime(), firstItem.getCreateTime());
        assertEquals(orderInfo.getCreateTime(), firstItem.getUpdateTime());
        assertEquals(orderInfo.getCreateTime(), secondItem.getCreateTime());
        assertEquals(orderInfo.getCreateTime(), secondItem.getUpdateTime());
    }
}
