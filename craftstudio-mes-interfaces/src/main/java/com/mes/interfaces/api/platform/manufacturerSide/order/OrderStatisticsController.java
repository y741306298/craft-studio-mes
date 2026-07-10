package com.mes.interfaces.api.platform.manufacturerSide.order;

import com.mes.application.command.order.AppOrderService;
import com.mes.application.command.order.vo.OrderQuery;
import com.mes.application.dto.req.order.OrderListRequest;
import com.mes.application.dto.resp.order.OrderStatisticsResponse;
import com.mes.domain.base.repository.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/manufacturerSide/order/statistics")
public class OrderStatisticsController {
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private AppOrderService appOrderService;

    /**
     * 根据订单列表查询条件全量统计订单数据，不使用分页，也不读取 orderDailyStatistics。
     *
     * @param request 查询参数，入参与 listOrders 一致
     * @return 实时计算的订单总数、总面积和总金额
     */
    @PostMapping
    public ApiResponse<OrderStatisticsResponse> statistics(@RequestBody OrderListRequest request) {
        OrderQuery orderQuery = buildOrderQuery(request);
        return ApiResponse.success(appOrderService.calculateOrderStatistics(orderQuery));
    }

    private OrderQuery buildOrderQuery(OrderListRequest request) {
        String orderId = request.getOrderId();
        String status = request.getStatus();
        String customerName = request.getCustomerName();
        String customerPhone = request.getCustomerPhone();
        String createDateStart = request.getCreateDateStart();
        String createDateEnd = request.getCreateDateEnd();

        OrderQuery orderQuery = new OrderQuery();
        orderQuery.setOrderId(orderId);
        orderQuery.setManufacturerId(request.getManufacturerId());
        if (status != null && !status.trim().isEmpty()) {
            orderQuery.setStatus(com.mes.domain.order.enums.OrderStatus.valueOf(status));
        }
        orderQuery.setCustomerName(customerName);
        orderQuery.setCustomerPhone(customerPhone);
        orderQuery.setRouteId(request.getRouteId());
        orderQuery.setOrgName(request.getOrgName());

        if (createDateStart != null && !createDateStart.trim().isEmpty()) {
            try {
                LocalDate startDate = LocalDate.parse(createDateStart);
                LocalDateTime startDateTime = startDate.atStartOfDay();
                orderQuery.setStartTime(java.util.Date.from(startDateTime.atZone(BEIJING_ZONE).toInstant()));
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("开始日期格式错误，应为 yyyy-MM-dd");
            }
        }
        if (createDateEnd != null && !createDateEnd.trim().isEmpty()) {
            try {
                LocalDate endDate = LocalDate.parse(createDateEnd);
                LocalDateTime endDateTime = endDate.atTime(LocalTime.of(23, 59, 59));
                orderQuery.setEndTime(java.util.Date.from(endDateTime.atZone(BEIJING_ZONE).toInstant()));
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("结束日期格式错误，应为 yyyy-MM-dd");
            }
        }
        return orderQuery;
    }
}
