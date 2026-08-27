package com.mes.interfaces.api.platform.manufacturerSide.statistics;

import com.mes.application.command.order.AppOrderService;
import com.mes.application.command.statistics.vo.TransferOrderItemVO;
import com.mes.application.command.statistics.vo.OrderStatisticsItemVO;
import com.mes.application.command.statistics.vo.OrderStatisticsListVO;
import com.mes.application.command.statistics.vo.OrderStatisticsFiltersVO;
import com.mes.application.command.statistics.vo.TransferOrderStatisticsVO;
import com.mes.application.command.statistics.vo.TransferFactoryVO;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.application.dto.req.statistics.OrderStatisticsListRequest;
import com.mes.application.dto.req.statistics.OrderStatisticsAllRequest;
import com.mes.application.dto.req.statistics.OrderStatisticsFiltersRequest;
import com.mes.application.dto.req.statistics.TransferOrderStatisticsRequest;
import com.mes.application.dto.req.statistics.TransferOrderStatisticsAllRequest;
import com.mes.application.dto.req.statistics.TransferSourceFactoryRequest;
import com.mes.application.dto.req.statistics.TransferTargetFactoryRequest;
import com.mes.application.dto.resp.PagedApiResponse;
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
import java.util.List;

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
                parseStartDate(request.getCreateDateStart()),
                parseEndDate(request.getCreateDateEnd()),
                request.getRouteId(),
                request.getMaterialId(),
                request.getMaterialName(),
                request.getMaterialType(),
                request.getOrgName(),
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
        response.getData().setStatusList(result.getStatusList());
        response.getData().setOrgNameList(result.getOrgNameList());
        return response;
    }

    /** 全量查询订单统计列表，筛选及汇总口径与分页接口一致。 */
    @PostMapping("/order/listAll")
    public ApiResponse<OrderStatisticsListVO> listAllOrderStatistics(
            @Valid @RequestBody OrderStatisticsAllRequest request) {
        return ApiResponse.success(appOrderService.findAllOrderStatistics(
                request.getManufacturerId(),
                request.getOrderId(),
                parseStartDate(request.getCreateDateStart()),
                parseEndDate(request.getCreateDateEnd()),
                request.getRouteId(),
                request.getMaterialId(),
                request.getMaterialName(),
                request.getMaterialType(),
                request.getOrgName()));
    }

    /** 按时间、源工厂和目标工厂查询转单项目及持久化统计。 */
    @PostMapping("/transfer/list")
    public PagedApiResponse<TransferOrderItemVO> listTransferOrderStatistics(
            @Valid @RequestBody TransferOrderStatisticsRequest request) {
        PagedQuery query = request.toPagedQuery();
        TransferOrderStatisticsVO result = appOrderService.findTransferOrderStatistics(
                request.getSourceId(), request.getTargetId(),
                parseStartDate(request.getCreateDateStart()),
                parseEndDate(request.getCreateDateEnd()), query);
        return PagedApiResponse.success(result.getItems(), query.getCurrent(), query.getSize(), result.getTotal(),
                result.getTotalOrderCount(), java.math.BigDecimal.ZERO, result.getTotalAmount());
    }

    /** 全量查询转单项目及持久化统计，筛选及汇总口径与分页接口一致。 */
    @PostMapping("/transfer/listAll")
    public ApiResponse<TransferOrderStatisticsVO> listAllTransferOrderStatistics(
            @Valid @RequestBody TransferOrderStatisticsAllRequest request) {
        return ApiResponse.success(appOrderService.findAllTransferOrderStatistics(
                request.getSourceId(), request.getTargetId(),
                parseStartDate(request.getCreateDateStart()),
                parseEndDate(request.getCreateDateEnd())));
    }

    /** 查询在指定时间段向目标工厂转单的来源工厂。 */
    @PostMapping("/transfer/sourceFactories")
    public ApiResponse<List<TransferFactoryVO>> listTransferSourceFactories(
            @Valid @RequestBody TransferSourceFactoryRequest request) {
        return ApiResponse.success(appOrderService.findTransferSourceFactories(
                request.getTargetId(), parseStartDate(request.getCreateDateStart()),
                parseEndDate(request.getCreateDateEnd())));
    }

    /** 查询在指定时间段接收来源工厂转单的目标工厂。 */
    @PostMapping("/transfer/targetFactories")
    public ApiResponse<List<TransferFactoryVO>> listTransferTargetFactories(
            @Valid @RequestBody TransferTargetFactoryRequest request) {
        return ApiResponse.success(appOrderService.findTransferTargetFactories(
                request.getSourceId(), parseStartDate(request.getCreateDateStart()),
                parseEndDate(request.getCreateDateEnd())));
    }

    /** Returns the distinct enterprise, material and route dimensions recorded in the period. */
    @PostMapping("/order/filters")
    public ApiResponse<OrderStatisticsFiltersVO> listOrderStatisticsFilters(
            @Valid @RequestBody OrderStatisticsFiltersRequest request) {
        LocalDate startDate = parseLocalDate(request.getCreateDateStart(), "开始日期");
        LocalDate endDate = parseLocalDate(request.getCreateDateEnd(), "结束日期");
        return ApiResponse.success(appOrderService.findOrderStatisticsFilters(
                request.getManufacturerId(), startDate, endDate));
    }

    private LocalDate parseLocalDate(String date, String fieldName) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return LocalDate.parse(date);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + "格式错误，应为 yyyy-MM-dd");
        }
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
