package com.mes.application.command.order;

import com.mes.application.command.order.vo.OrderItemVO;
import com.mes.application.command.statistics.vo.OrderStatisticsListVO;
import com.mes.application.command.statistics.vo.OrderStatisticsStatusVO;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.mes.domain.order.orderStatistics.entity.OrderStatisticsType;
import com.mes.domain.order.orderStatistics.service.OrderDailyStatisticsService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Read-only order statistics query, separated from order command processing. */
@Service
public class AppOrderStatisticsQueryService {
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    private final OrderItemService orderItemService;
    private final OrderInfoService orderInfoService;
    private final DeliveryRouteRepository deliveryRouteRepository;
    private final OrderDailyStatisticsService dailyStatisticsService;
    private final AppOrderService appOrderService;

    public AppOrderStatisticsQueryService(OrderItemService orderItemService,
                                          OrderInfoService orderInfoService,
                                          DeliveryRouteRepository deliveryRouteRepository,
                                          OrderDailyStatisticsService dailyStatisticsService,
                                          AppOrderService appOrderService) {
        this.orderItemService = orderItemService;
        this.orderInfoService = orderInfoService;
        this.deliveryRouteRepository = deliveryRouteRepository;
        this.dailyStatisticsService = dailyStatisticsService;
        this.appOrderService = appOrderService;
    }

    public OrderStatisticsListVO find(String manufacturerId, String orderId, OrderStatus status,
                                      Date startTime, Date endTime, String routeId, String materialId,
                                      String materialName, String materialType, String orgName,
                                      PagedQuery pagedQuery) {
        if (pagedQuery == null || pagedQuery.getSize() <= 0 || pagedQuery.getSize() > 100) {
            throw new IllegalArgumentException("分页参数不能为空且每页大小必须在 1-100 之间");
        }
        Map<String, Object> filters = buildFilters(manufacturerId, orderId, status, startTime, endTime,
                routeId, materialId, materialName, materialType, orgName);
        List<OrderItem> orderItems = orderItemService.filterListUrgentFirst(
                (int) pagedQuery.getCurrent(), (int) pagedQuery.getSize(), filters);
        long total = orderItemService.filterTotal(filters);

        Map<String, OrderInfo> orders = orderInfoService.findByOrderIds(orderItems.stream()
                        .map(OrderItem::getOrderId).filter(StringUtils::isNotBlank)
                        .collect(Collectors.toCollection(LinkedHashSet::new))).stream()
                .collect(Collectors.toMap(OrderInfo::getOrderId, order -> order, (first, ignored) -> first));
        Map<String, String> routes = findRouteNames(orderItems);
        List<OrderItemVO> items = orderItems.stream()
                .map(item -> toVO(item, orders.get(item.getOrderId()), routes.get(item.getRouteId())))
                .toList();
        OrderDailyStatistics totals = findTotals(manufacturerId, startTime, endTime, routeId, materialId, orgName);
        return new OrderStatisticsListVO(items, total,
                totals == null ? 0L : totals.getTotalOrderCount(),
                totals == null ? BigDecimal.ZERO : totals.getTotalArea(),
                totals == null ? BigDecimal.ZERO : totals.getTotalAmount(),
                List.of(), buildStatuses(), List.of());
    }

    private Map<String, Object> buildFilters(String manufacturerId, String orderId, OrderStatus status,
                                             Date startTime, Date endTime, String routeId, String materialId,
                                             String materialName, String materialType, String orgName) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerId", manufacturerId);
        if (StringUtils.isNotBlank(orderId)) filters.put("orderId_like", orderId.trim());
        if (status != null) filters.put("status", status.getCode());
        if (startTime != null) filters.put("createTime_gte", startTime);
        if (endTime != null) filters.put("createTime_lte", endTime);
        if (StringUtils.isNotBlank(routeId)) filters.put("routeId", routeId);
        if (StringUtils.isNotBlank(materialId)) filters.put("material.materialId", materialId);
        if (StringUtils.isNotBlank(materialName)) filters.put("material.materialSnapshot.name_like", materialName);
        if (StringUtils.isNotBlank(materialType)) filters.put("material.materialType", materialType);
        if (StringUtils.isNotBlank(orgName)) filters.put("orgInfo.name", orgName);
        return filters;
    }

    private Map<String, String> findRouteNames(List<OrderItem> items) {
        Set<String> routeIds = items.stream().filter(Objects::nonNull).map(OrderItem::getRouteId)
                .filter(StringUtils::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
        return deliveryRouteRepository.findByRouteIds(routeIds).stream()
                .collect(Collectors.toMap(DeliveryRoute::getRouteId, DeliveryRoute::getRouteName,
                        (first, ignored) -> first));
    }

    private OrderItemVO toVO(OrderItem item, OrderInfo order, String routeName) {
        OrderItemVO result = new OrderItemVO();
        BeanUtils.copyProperties(item, result);
        result.setRouteName(routeName);
        if (order == null) {
            result.setOrderItemPrice(BigDecimal.ZERO);
            return result;
        }
        result.setCustomer(order.getCustomer());
        result.setRemark(order.getRemark());
        result.setOrgInfo(order.getOrgInfo());
        BigDecimal paymentPrice = order.getPrice() == null ? null : order.getPrice().getPaymentPrice();
        result.setPaymentPrice(scale(paymentPrice));
        result.setOrderItemPrice(scale(appOrderService.calculateStatisticsAmount(order)));
        return result;
    }

    private OrderDailyStatistics findTotals(String manufacturerId, Date startTime, Date endTime,
                                            String routeId, String materialId, String orgName) {
        if (StringUtils.isBlank(manufacturerId) || startTime == null || endTime == null) return null;
        String indexId = null;
        OrderStatisticsType type = OrderStatisticsType.ENTERPRISE;
        if (StringUtils.isNotBlank(materialId)) {
            indexId = materialId; type = OrderStatisticsType.MATERIAL;
        } else if (StringUtils.isNotBlank(routeId)) {
            indexId = routeId; type = OrderStatisticsType.ROUTE;
        } else if (StringUtils.isNotBlank(orgName)) {
            indexId = orgName;
        }
        return dailyStatisticsService.sum(manufacturerId,
                startTime.toInstant().atZone(BEIJING_ZONE).toLocalDate(),
                endTime.toInstant().atZone(BEIJING_ZONE).toLocalDate(), indexId, type);
    }

    private List<OrderStatisticsStatusVO> buildStatuses() {
        return Arrays.stream(OrderStatus.values())
                .map(status -> new OrderStatisticsStatusVO(status.getCode(), status.getDescription())).toList();
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
