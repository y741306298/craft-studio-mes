package com.mes.interfaces.api.platform.manufacturerSide.statistics;

import com.mes.application.command.order.AppOrderService;
import com.mes.application.command.statistics.vo.OrderStatisticsItemVO;
import com.mes.application.command.statistics.vo.OrderStatisticsListVO;
import com.mes.application.dto.req.statistics.OrderStatisticsListRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.domain.order.enums.OrderStatus;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

@RestController
@RequestMapping("/api/manufacturerSide/statistics")
public class StatisticsController {
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private AppOrderService appOrderService;

    /**
     * 分页查询订单统计列表。
     *
     * <p>返回订单维度的基础信息，并按查询到的订单项实时汇总总订单数、总面积和总金额。</p>
     */
    @PostMapping("/order/list")
    public PagedApiResponse<OrderStatisticsItemVO> listOrderStatistics(
            @Valid @RequestBody OrderStatisticsListRequest request) {
        OrderStatisticsListVO result = appOrderService.findOrderStatistics(
                request.getManufacturerId(),
                request.getOrderId(),
                resolveStatus(request.getStatus()),
                parseStartDate(request.getCreateDateStart()),
                parseEndDate(request.getCreateDateEnd()),
                request.getMaterialId(),
                request.getMaterialName(),
                request.getMaterialType(),
                request.toPagedQuery());

        PagedQuery query = request.toPagedQuery();
        PagedApiResponse<OrderStatisticsItemVO> response = PagedApiResponse.success(
                result.getItems(),
                query.getCurrent(),
                query.getSize(),
                result.getTotal(),
                result.getTotalOrderCount(),
                result.getTotalArea(),
                result.getTotalAmount());
        response.getData().setMaterialList(result.getMaterialList());
        return response;
    }

    private OrderStatus resolveStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return OrderStatus.valueOf(status);
    }

    private Date parseStartDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDate startDate = LocalDate.parse(date);
            LocalDateTime startDateTime = startDate.atStartOfDay();
            return Date.from(startDateTime.atZone(BEIJING_ZONE).toInstant());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("开始日期格式错误，应为 yyyy-MM-dd");
        }
    }

    private Date parseEndDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDate endDate = LocalDate.parse(date);
            LocalDateTime endDateTime = endDate.atTime(LocalTime.of(23, 59, 59));
            return Date.from(endDateTime.atZone(BEIJING_ZONE).toInstant());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("结束日期格式错误，应为 yyyy-MM-dd");
        }
    }
}
