package com.mes.application.command.order;

import com.mes.application.command.order.vo.OrderItemVO;
import com.mes.application.command.order.vo.OrderPackagingSyncResult;
import com.mes.application.command.order.vo.OrderPriceStatisticsVO;
import com.mes.application.command.order.vo.OrderQuery;
import com.mes.application.command.order.vo.OrderWithItemsVO;
import com.mes.application.command.orderPreprocessing.OrderPreprocessTaskQueue;
import com.mes.application.command.statistics.vo.OrderStatisticsItemVO;
import com.mes.application.command.statistics.vo.OrderStatisticsMaterialVO;
import com.mes.application.command.statistics.vo.OrderStatisticsListVO;
import com.mes.application.command.statistics.vo.OrderStatisticsStatusVO;
import com.mes.application.command.statistics.vo.OrderStatisticsDimensionVO;
import com.mes.application.command.statistics.vo.OrderStatisticsFiltersVO;
import com.mes.application.command.statistics.vo.TransferOrderStatisticsVO;
import com.mes.application.command.statistics.vo.TransferOrderItemVO;
import com.mes.application.command.statistics.vo.TransferFactoryVO;
import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.dto.req.order.OrderAddRequest;
import com.mes.application.dto.req.order.OrderTransferRequest;
import com.mes.application.support.PodvOrgInfoHelper;
import com.mes.domain.auth.entity.ManufacturerUser;
import com.mes.domain.auth.repository.ManufacturerUserRepository;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerMetaRepository;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.vo.ManufacturerInfo;
import com.mes.domain.order.orderItemPriceAllocation.entity.OrderItemPriceAllocation;
import com.mes.domain.order.orderItemPriceAllocation.repository.OrderItemPriceAllocationRepository;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.mes.domain.order.orderStatistics.entity.OrderStatisticsType;
import com.mes.domain.order.orderStatistics.service.OrderDailyStatisticsService;
import com.mes.domain.order.orderTransferRecord.entity.OrderTransferRecord;
import com.mes.domain.order.preOrderLabelTask.service.PreOrderLabelTaskService;
import com.mes.domain.order.productionPieceGenerationTask.service.ProductionPieceGenerationTaskService;
import com.mes.domain.order.orderTransferRecord.service.OrderTransferRecordService;
import com.mes.domain.order.transferStatistics.entity.TransferDailyStatistics;
import com.mes.domain.order.transferStatistics.service.TransferDailyStatisticsService;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.alibaba.fastjson.JSON;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AppOrderService {

    private static final Logger log = LoggerFactory.getLogger(AppOrderService.class);
    private static final long TRANSFER_LOCK_EXPIRATION_MS = 24 * 60 * 60 * 1000L;

    @Autowired
    private OrderInfoService domainOrderInfoService;

    @Autowired
    private OrderItemService domainOrderItemService;

    @Autowired
    private ProductionPieceGenerationTaskService productionPieceGenerationTaskService;

    @Autowired
    private ProductionPieceService productionPieceService;

    @Autowired
    private ManufacturerMetaRepository manufacturerMetaRepository;

    @Autowired
    private ManufacturerUserRepository manufacturerUserRepository;

    @Autowired
    private OrderTransferRecordService orderTransferRecordService;

    @Autowired
    private OrderPreprocessTaskQueue orderPreprocessTaskQueue;

    @Autowired
    private AppOrderPreprocessingService appOrderPreprocessingService;

    @Autowired
    private PreOrderLabelTaskService preOrderLabelTaskService;

    @Autowired
    private OrderDailyStatisticsService orderDailyStatisticsService;

    @Autowired
    private TransferDailyStatisticsService transferDailyStatisticsService;

    @Autowired
    private DeliveryRouteRepository deliveryRouteRepository;

    @Autowired
    private OrderItemPriceAllocationRepository orderItemPriceAllocationRepository;

    @Autowired
    private PodvOrgInfoHelper podvOrgInfoHelper;

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String NO_ROUTE_ID = "NO_ROUTE";
    private static final String NO_ROUTE_NAME = "无路线";

    /**
     * Rebuilds persisted order and transfer statistics from source documents. The operation is
     * idempotent: statistics in the requested range are removed before being recreated.
     */
    public String calibrateDailyStatistics(String manufacturerMetaId, LocalDate startDate) {
        if (StringUtils.isBlank(manufacturerMetaId) || startDate == null) {
            throw new IllegalArgumentException("工厂和开始日期不能为空");
        }
        LocalDate endDate = LocalDate.now(BEIJING_ZONE);
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于今天");
        }

        Date startTime = Date.from(startDate.atStartOfDay(BEIJING_ZONE).toInstant());
        Date endTime = Date.from(endDate.plusDays(1).atStartOfDay(BEIJING_ZONE).minusNanos(1).toInstant());
        List<OrderInfo> orders = findAllOrders(manufacturerMetaId, startTime, endTime);

        orderDailyStatisticsService.deleteRange(manufacturerMetaId, startDate, endDate);
        int calibratedOrders = 0;
        for (OrderInfo order : orders) {
            if (order == null || order.getStatus() == OrderStatus.RETURNED || order.getCreateTime() == null) {
                continue;
            }
            List<OrderItem> items = findAllOrderItems(order.getOrderId(), manufacturerMetaId);
            if (items.isEmpty()) {
                continue;
            }
            LocalDate statisticsDate = order.getCreateTime().toInstant().atZone(BEIJING_ZONE).toLocalDate();
            OrderStatisticsAmounts amounts = calculateCalibratedStatisticsAmounts(manufacturerMetaId, items);
            incrementOrderDimensions(manufacturerMetaId, statisticsDate, order, items, 1L, BigDecimal.ONE,
                    amounts, amounts.totalAmount());
            calibratedOrders++;
        }

        int calibratedTransfers = rebuildTransferDailyStatistics(manufacturerMetaId, startDate, endDate,
                startTime, endTime);
        return "统计校准完成，订单数：" + calibratedOrders + "，转单数：" + calibratedTransfers;
    }

    /**
     * 校准时以当前实际存在的订单项为准，并优先使用订单项已持久化的金额分摊。
     * 没有金额分摊的订单项回退到自身的工厂实际价，不再读取订单快照的 actualPrice。
     */
    private OrderStatisticsAmounts calculateCalibratedStatisticsAmounts(String manufacturerMetaId,
                                                                         List<OrderItem> orderItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        LinkedHashMap<String, BigDecimal> amountByMaterialId = new LinkedHashMap<>();
        LinkedHashMap<String, BigDecimal> areaByMaterialId = new LinkedHashMap<>();
        for (OrderItem orderItem : orderItems) {
            OrderItemPriceAllocation allocation = orderItemPriceAllocationRepository
                    .findByOrderItemIdAndManufacturerMetaId(orderItem.getOrderItemId(), manufacturerMetaId);
            BigDecimal itemAmount = allocation != null && allocation.getPrice() != null
                    ? allocation.getPrice() : resolveManufacturerActualPrice(orderItem);
            totalAmount = totalAmount.add(itemAmount);

            String materialId = resolveMaterialId(orderItem);
            if (StringUtils.isNotBlank(materialId)) {
                amountByMaterialId.merge(materialId, itemAmount, BigDecimal::add);
                areaByMaterialId.merge(materialId, calculateOrderItemArea(orderItem), BigDecimal::add);
            }
        }
        amountByMaterialId.replaceAll((ignored, amount) -> scaleStatisticsDecimal(amount));
        return new OrderStatisticsAmounts(scaleStatisticsDecimal(totalAmount), amountByMaterialId,
                areaByMaterialId, Map.of());
    }

    private List<OrderInfo> findAllOrders(String manufacturerMetaId, Date startTime, Date endTime) {
        List<OrderInfo> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            List<OrderInfo> values = domainOrderInfoService.findOrdersByManufacturerAndCreateTime(
                    manufacturerMetaId, startTime, endTime, page, 100);
            result.addAll(values);
            if (values.size() < 100) return result;
        }
    }

    private List<OrderItem> findAllOrderItems(String orderId, String manufacturerMetaId) {
        List<OrderItem> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            List<OrderItem> values = domainOrderItemService.findByOrderId(orderId, manufacturerMetaId, page, 100);
            result.addAll(values);
            if (values.size() < 100) return result;
        }
    }

    private int rebuildTransferDailyStatistics(String manufacturerMetaId, LocalDate startDate, LocalDate endDate,
                                               Date startTime, Date endTime) {
        List<OrderTransferRecord> allRecords = orderTransferRecordService.findAllTransferRecords(
                manufacturerMetaId, null, null, null);
        List<OrderTransferRecord> records = allRecords.stream()
                .filter(record -> record.getCreateTime() != null
                        && !record.getCreateTime().before(startTime) && !record.getCreateTime().after(endTime))
                .toList();
        transferDailyStatisticsService.deleteRange(manufacturerMetaId, startDate, endDate);

        Map<String, Integer> originalQuantities = new HashMap<>();
        for (OrderTransferRecord record : allRecords) {
            originalQuantities.merge(record.getOrderItemId(), safeQuantity(record.getQuantity()), Integer::sum);
        }
        for (String itemId : new ArrayList<>(originalQuantities.keySet())) {
            OrderItem remainingItem = domainOrderItemService.findByOrderItemId(itemId);
            if (remainingItem != null && Objects.equals(manufacturerMetaId, remainingItem.getManufacturerId())) {
                originalQuantities.merge(itemId, safeQuantity(remainingItem.getQuantity()), Integer::sum);
            }
        }

        Map<String, List<OrderTransferRecord>> transferGroups = records.stream().collect(Collectors.groupingBy(
                record -> record.getTargetId() + "|" + record.getOrderId() + "|" + record.getCreateTime().getTime(),
                LinkedHashMap::new, Collectors.toList()));
        for (List<OrderTransferRecord> group : transferGroups.values()) {
            OrderTransferRecord first = group.get(0);
            BigDecimal amount = group.stream().map(record -> calculateHistoricalTransferAmount(
                    manufacturerMetaId, record, originalQuantities.getOrDefault(record.getOrderItemId(), 0)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDate statisticsDate = first.getCreateTime().toInstant().atZone(BEIJING_ZONE).toLocalDate();
            transferDailyStatisticsService.increment(manufacturerMetaId, first.getTargetId(), first.getTargetName(),
                    statisticsDate, 1L, scaleStatisticsDecimal(amount));
        }
        return transferGroups.size();
    }

    private BigDecimal calculateHistoricalTransferAmount(String manufacturerMetaId, OrderTransferRecord record,
                                                          int originalQuantity) {
        if (originalQuantity <= 0 || record.getQuantity() == null) return BigDecimal.ZERO;
        OrderItemPriceAllocation allocation = orderItemPriceAllocationRepository
                .findByOrderItemIdAndManufacturerMetaId(record.getOrderItemId(), manufacturerMetaId);
        OrderItem priceSource = domainOrderItemService.findByOrderItemId(record.getOrderItemId());
        if (priceSource == null) priceSource = domainOrderItemService.findByOrderItemId(record.getTargetOrderItemId());
        BigDecimal itemPrice = allocation != null && allocation.getPrice() != null
                ? allocation.getPrice() : resolveManufacturerActualPrice(priceSource);
        return itemPrice.multiply(BigDecimal.valueOf(record.getQuantity()))
                .divide(BigDecimal.valueOf(originalQuantity), 12, RoundingMode.HALF_UP);
    }

    /**
     * 查询指定工厂和创建时间范围内的全部订单，并汇总非退单订单的工厂价格快照。
     */
    public OrderPriceStatisticsVO findOrderPriceStatistics(String manufacturerId, Date startTime, Date endTime) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new IllegalArgumentException("工厂 ID 不能为空");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为空");
        }
        if (startTime.after(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }

        List<OrderInfo> orderInfos = new ArrayList<>();
        int current = 1;
        int size = 100;
        while (true) {
            List<OrderInfo> page = domainOrderInfoService.findOrdersByManufacturerAndCreateTime(
                    manufacturerId, startTime, endTime, current, size);
            if (page == null || page.isEmpty()) {
                break;
            }
            orderInfos.addAll(page);
            if (page.size() < size) {
                break;
            }
            current++;
        }

        BigDecimal actualPriceTotal = BigDecimal.ZERO;
        BigDecimal logisticsPriceTotal = BigDecimal.ZERO;
        BigDecimal paymentPriceTotal = BigDecimal.ZERO;
        for (OrderInfo orderInfo : orderInfos) {
            if (orderInfo == null || orderInfo.getStatus() == OrderStatus.RETURNED
                    || orderInfo.getManufacturerInfo() == null
                    || orderInfo.getManufacturerInfo().getPrice() == null) {
                continue;
            }
            var price = orderInfo.getManufacturerInfo().getPrice();
            actualPriceTotal = actualPriceTotal.add(zeroIfNull(price.getActualPrice()));
            logisticsPriceTotal = logisticsPriceTotal.add(zeroIfNull(price.getLogisticsPrice()));
            paymentPriceTotal = paymentPriceTotal.add(zeroIfNull(price.getPaymentPrice()));
        }
        return new OrderPriceStatisticsVO(orderInfos, actualPriceTotal, logisticsPriceTotal, paymentPriceTotal);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 同步生产中订单项的已打包状态。
     * 查询所有状态为生产中的订单项，统计其关联生产工件中“已打包”节点的数量，
     * 当已打包数量大于等于订单项数量时，将订单项状态更新为“已打包”。
     *
     * @return 同步结果
     */
    public OrderPackagingSyncResult syncPackagedOrderItems() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("status", OrderStatus.IN_PRODUCTION.getCode());

        List<OrderItem> orderItems = domainOrderItemService.filterAllUrgentFirst(filters);
        List<OrderPackagingSyncResult.ItemPackagingSyncResult> itemResults = new ArrayList<>();
        long updatedCount = 0;

        for (OrderItem orderItem : orderItems) {
            if (orderItem == null || StringUtils.isBlank(orderItem.getOrderItemId())) {
                continue;
            }
            long packedQuantity = countPackedQuantity(orderItem.getOrderItemId());
            int orderItemQuantity = safeQuantity(orderItem.getQuantity());
            boolean updated = false;
            if (packedQuantity >= orderItemQuantity && orderItem.getStatus() == OrderStatus.IN_PRODUCTION) {
                orderItem.setStatus(OrderStatus.PACKAGED);
                domainOrderItemService.updateOrderItem(orderItem);
                updated = true;
                updatedCount++;
            }
            itemResults.add(new OrderPackagingSyncResult.ItemPackagingSyncResult(
                    orderItem.getOrderItemId(),
                    orderItem.getQuantity(),
                    packedQuantity,
                    updated
            ));
        }

        return new OrderPackagingSyncResult(orderItems.size(), updatedCount, itemResults);
    }

    private long countPackedQuantity(String orderItemId) {
        long packedQuantity = 0;
        int current = 1;
        int size = 100;
        while (true) {
            List<ProductionPiece> pieces = productionPieceService.findProductionPiecesByOrderItemId(orderItemId, current, size);
            if (pieces == null || pieces.isEmpty()) {
                break;
            }
            for (ProductionPiece piece : pieces) {
                packedQuantity += countPackedQuantity(piece);
            }
            if (pieces.size() < size) {
                break;
            }
            current++;
        }
        return packedQuantity;
    }

    private long countPackedQuantity(ProductionPiece piece) {
        if (piece == null) {
            return 0;
        }
        ProcedureFlow flow = piece.getProcedureFlow();
        if (flow == null || flow.getNodes() == null) {
            return 0;
        }
        return flow.getNodes().stream()
                .filter(Objects::nonNull)
                .filter(this::isPackedNode)
                .mapToLong(node -> node.getPieceQuantity() == null ? 0 : node.getPieceQuantity())
                .sum();
    }

    private boolean isPackedNode(ProcedureFlowNode node) {
        return "NODE_PACKED".equals(node.getNodeId()) || "已打包".equals(node.getNodeName());
    }

    /**
     * 根据多条件分页查询订单项列表，同时查询关联的订单
     * 如果指定了 manufacturerId，则查询该制造商的订单项并聚合到订单中
     * @param query 查询参数
     * @return 分页结果，包含订单和对应的订单项
     */
    public PagedResult<OrderItemVO> findOrdersWithItems(OrderQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
        if (query.getPagedQuery() == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getPagedQuery().getSize() <= 0 || query.getPagedQuery().getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        String orderId = query.getOrderId();
        String manufacturerId = query.getManufacturerId();
        String status = query.getStatus() != null ? query.getStatus().getCode() : null;
        String customerName = query.getCustomerName();
        String customerPhone = query.getCustomerPhone();
        String routeId = query.getRouteId();
        String orgName = query.getOrgName();
        var startTime = query.getStartTime();
        var endTime = query.getEndTime();
        var pagedQuery = query.getPagedQuery();

        long total;

        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerId", manufacturerId);
        if (StringUtils.isNotBlank(orderId)) {
            filters.put("orderId_like", orderId.trim());
        }
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (StringUtils.isNotBlank(routeId)) {
            filters.put("routeId", routeId);
        }
        if (startTime != null) {
            filters.put("createTime_gte", startTime);
        }
        if (endTime != null) {
            filters.put("createTime_lte", endTime);
        }
        if (StringUtils.isNotBlank(customerName) || StringUtils.isNotBlank(customerPhone)) {
            Set<String> customerMatchedOrderIds = new LinkedHashSet<>(
                    domainOrderInfoService.findOrderIdsByCustomerConditions(customerName, customerPhone)
            );
            if (StringUtils.isNotBlank(orderId)) {
                String normalizedOrderId = orderId.trim().toLowerCase(Locale.ROOT);
                customerMatchedOrderIds.removeIf(matchedOrderId ->
                        StringUtils.isBlank(matchedOrderId)
                                || !matchedOrderId.toLowerCase(Locale.ROOT).contains(normalizedOrderId));
                filters.remove("orderId_like");
            }
            if (customerMatchedOrderIds.isEmpty()) {
                return new PagedResult<>(List.of(), 0, pagedQuery.getSize(), pagedQuery.getCurrent());
            }
            filters.put("orderId_in", customerMatchedOrderIds);
        }
        if (StringUtils.isNotBlank(orgName)) {
            Set<String> orgMatchedOrderIds = new LinkedHashSet<>(
                    domainOrderInfoService.findOrderIdsByOrgName(orgName)
            );
            if (StringUtils.isNotBlank(orderId)) {
                String normalizedOrderId = orderId.trim().toLowerCase(Locale.ROOT);
                orgMatchedOrderIds.removeIf(matchedOrderId ->
                        StringUtils.isBlank(matchedOrderId)
                                || !matchedOrderId.toLowerCase(Locale.ROOT).contains(normalizedOrderId));
                filters.remove("orderId_like");
            }
            if (filters.get("orderId_in") instanceof Set<?> existingOrderIds) {
                orgMatchedOrderIds.retainAll(existingOrderIds);
            }
            if (orgMatchedOrderIds.isEmpty()) {
                return new PagedResult<>(List.of(), 0, pagedQuery.getSize(), pagedQuery.getCurrent());
            }
            filters.put("orderId_in", orgMatchedOrderIds);
        }
        List<OrderItem> orderItems = domainOrderItemService.filterListUrgentFirst(
                (int) pagedQuery.getCurrent(),
                (int) pagedQuery.getSize(),
                filters
        );
        total = domainOrderItemService.filterTotal(filters);
        List<OrderItemVO> result = buildOrderItemVOs(orderItems);
        return new PagedResult<>(result, total, pagedQuery.getSize(), pagedQuery.getCurrent());
    }


    public OrderStatisticsListVO findOrderStatistics(String manufacturerId,
                                                     String orderId,
                                                     java.util.Date startTime,
                                                     java.util.Date endTime,
                                                     String routeId,
                                                     String materialId,
                                                     String materialName,
                                                     String materialType,
                                                     String orgName,
                                                     PagedQuery pagedQuery) {
        if (pagedQuery == null || pagedQuery.getSize() <= 0 || pagedQuery.getSize() > 100) {
            throw new IllegalArgumentException("分页参数不能为空且每页大小必须在 1-100 之间");
        }
        Map<String, Object> filters = buildOrderStatisticsFilters(manufacturerId, orderId, startTime,
                endTime, routeId, materialId, materialName, materialType, orgName);

        List<OrderItem> orderItems = domainOrderItemService.filterListUrgentFirst(
                (int) pagedQuery.getCurrent(), (int) pagedQuery.getSize(), filters);
        long total = domainOrderItemService.filterTotal(filters);
        Map<String, String> routeNameByRouteId = findRouteNames(orderItems);
        List<OrderStatisticsItemVO> pageItems = orderItems.stream()
                .map(item -> toOrderStatisticsItemVO(item, routeNameByRouteId.get(item.getRouteId())))
                .toList();

        OrderDailyStatistics totals = findPersistedStatisticsTotals(
                manufacturerId, startTime, endTime, routeId, materialId, orgName);
        return new OrderStatisticsListVO(pageItems, total,
                totals == null ? 0L : totals.getTotalOrderCount(),
                totals == null ? BigDecimal.ZERO : totals.getTotalArea(),
                totals == null ? BigDecimal.ZERO : totals.getTotalAmount(),
                List.of(), buildOrderStatisticsStatusList(), List.of());
    }

    public OrderStatisticsListVO findAllOrderStatistics(String manufacturerId,
                                                        String orderId,
                                                        Date startTime,
                                                        Date endTime,
                                                        String routeId,
                                                        String materialId,
                                                        String materialName,
                                                        String materialType,
                                                        String orgName) {
        Map<String, Object> filters = buildOrderStatisticsFilters(manufacturerId, orderId, startTime,
                endTime, routeId, materialId, materialName, materialType, orgName);
        List<OrderItem> orderItems = domainOrderItemService.filterAllUrgentFirst(filters);
        Map<String, String> routeNameByRouteId = findRouteNames(orderItems);
        List<OrderStatisticsItemVO> items = orderItems.stream()
                .map(item -> toOrderStatisticsItemVO(item, routeNameByRouteId.get(item.getRouteId())))
                .toList();

        OrderDailyStatistics totals = findPersistedStatisticsTotals(
                manufacturerId, startTime, endTime, routeId, materialId, orgName);
        return new OrderStatisticsListVO(items, items.size(),
                totals == null ? 0L : totals.getTotalOrderCount(),
                totals == null ? BigDecimal.ZERO : totals.getTotalArea(),
                totals == null ? BigDecimal.ZERO : totals.getTotalAmount(),
                List.of(), buildOrderStatisticsStatusList(), List.of());
    }

    private Map<String, Object> buildOrderStatisticsFilters(String manufacturerId, String orderId,
                                                            Date startTime, Date endTime,
                                                            String routeId, String materialId, String materialName,
                                                            String materialType, String orgName) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerId", manufacturerId);
        if (StringUtils.isNotBlank(orderId)) filters.put("orderId_like", orderId.trim());
        filters.put("status_in", List.of(
                OrderStatus.IN_PRODUCTION.getCode(),
                OrderStatus.PACKAGED.getCode()));
        if (startTime != null) filters.put("createTime_gte", startTime);
        if (endTime != null) filters.put("createTime_lte", endTime);
        if (StringUtils.isNotBlank(routeId)) filters.put("routeId", routeId);
        if (StringUtils.isNotBlank(materialId)) filters.put("material.materialId", materialId);
        if (StringUtils.isNotBlank(materialName)) filters.put("material.materialSnapshot.name_like", materialName);
        if (StringUtils.isNotBlank(materialType)) filters.put("material.materialType", materialType);
        if (StringUtils.isNotBlank(orgName)) filters.put("orgInfo.name", orgName);
        return filters;
    }

    private OrderStatisticsItemVO toOrderStatisticsItemVO(OrderItem item, String routeName) {
        OrderStatisticsItemVO result = new OrderStatisticsItemVO();
        BeanUtils.copyProperties(item, result);
        result.setRouteName(routeName);
        result.setPaymentPrice(item.getPrice() == null ? BigDecimal.ZERO : item.getPrice().getActualPrice());
        return result;
    }

    private Map<String, String> findRouteNames(List<OrderItem> orderItems) {
        Set<String> routeIds = orderItems.stream()
                .map(OrderItem::getRouteId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> routeNames = new HashMap<>();
        deliveryRouteRepository.findByIdsOrRouteIds(routeIds).stream()
                .filter(route -> route.getRouteName() != null)
                .forEach(route -> {
                    if (StringUtils.isNotBlank(route.getId())) {
                        routeNames.putIfAbsent(route.getId(), route.getRouteName());
                    }
                    if (StringUtils.isNotBlank(route.getRouteId())) {
                        routeNames.putIfAbsent(route.getRouteId(), route.getRouteName());
                    }
                });
        return routeNames;
    }

    private OrderDailyStatistics findPersistedStatisticsTotals(String manufacturerMetaId, Date startTime, Date endTime,
                                                               String routeId, String materialId, String orgName) {
        if (StringUtils.isBlank(manufacturerMetaId) || startTime == null || endTime == null) return null;
        String indexId;
        OrderStatisticsType type;
        if (StringUtils.isNotBlank(materialId)) {
            indexId = materialId;
            type = OrderStatisticsType.MATERIAL;
        } else if (StringUtils.isNotBlank(routeId)) {
            indexId = routeId;
            type = OrderStatisticsType.ROUTE;
        } else if (StringUtils.isNotBlank(orgName)) {
            return orderDailyStatisticsService.sumByIndexName(manufacturerMetaId,
                    startTime.toInstant().atZone(BEIJING_ZONE).toLocalDate(),
                    endTime.toInstant().atZone(BEIJING_ZONE).toLocalDate(),
                    orgName, OrderStatisticsType.ENTERPRISE);
        } else {
            // manufacturerMetaId remains the required manufacturer condition. A null indexId
            // only means summing every enterprise index belonging to that manufacturer.
            indexId = null;
            type = OrderStatisticsType.ENTERPRISE;
        }
        return orderDailyStatisticsService.sum(manufacturerMetaId,
                startTime.toInstant().atZone(BEIJING_ZONE).toLocalDate(),
                endTime.toInstant().atZone(BEIJING_ZONE).toLocalDate(), indexId, type);
    }

    private List<OrderStatisticsStatusVO> buildOrderStatisticsStatusList() {
        return Arrays.stream(OrderStatus.values())
                .map(status -> new OrderStatisticsStatusVO(status.getCode(), status.getDescription()))
                .toList();
    }

    private List<OrderInfo> findStatisticOrders(String orderId,
                                                OrderStatus status,
                                                java.util.Date startTime,
                                                java.util.Date endTime,
                                                String routeId) {
        List<OrderInfo> orders = new ArrayList<>();
        int current = 1;
        int size = 100;
        String statusCode = status == null ? null : status.getCode();
        while (true) {
            List<OrderInfo> page = domainOrderInfoService.findOrdersByConditions(
                    orderId,
                    statusCode,
                    startTime,
                    endTime,
                    routeId,
                    current,
                    size);
            if (page == null || page.isEmpty()) {
                break;
            }
            if (status == null) {
                page.stream()
                        .filter(order -> order != null && order.getStatus() != OrderStatus.RETURNED)
                        .forEach(orders::add);
            } else {
                orders.addAll(page);
            }
            if (page.size() < size) {
                break;
            }
            current++;
        }
        return orders;
    }

    private List<OrderItem> findAllOrderItemsByOrder(String orderId, String manufacturerId, String materialId) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("orderId", orderId);
        if (StringUtils.isNotBlank(manufacturerId)) {
            filters.put("manufacturerId", manufacturerId);
        }
        if (StringUtils.isNotBlank(materialId)) {
            filters.put("material.materialId", materialId.trim());
        }
        return domainOrderItemService.filterAllUrgentFirst(filters);
    }

    private void collectMaterial(LinkedHashMap<String, OrderStatisticsMaterialVO> materialMap, Object material) {
        if (material == null) {
            return;
        }
        Object materialId = invokeGetter(material, "getMaterialId");
        if (materialId == null || StringUtils.isBlank(String.valueOf(materialId))) {
            return;
        }
        String materialIdText = String.valueOf(materialId);
        if (materialMap.containsKey(materialIdText)) {
            return;
        }
        Object materialSnapshot = invokeGetter(material, "getMaterialSnapshot");
        Object materialName = invokeGetter(materialSnapshot, "getName");
        Object materialType = invokeGetter(material, "getMaterialType");
        materialMap.put(materialIdText, new OrderStatisticsMaterialVO(
                materialIdText,
                materialName == null ? null : String.valueOf(materialName),
                materialType == null ? null : String.valueOf(materialType)));
    }

    /**
     * 根据订单 ID 全量查询数量不为 0 的订单项，返回结构与订单列表项一致。
     */
    public List<OrderItemVO> findNonZeroQuantityOrderItemsByOrderId(OrderQuery query) {
        if (query == null || StringUtils.isBlank(query.getOrderId())) {
            throw new IllegalArgumentException("订单 ID 不能为空");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("orderId", query.getOrderId().trim());
        if (StringUtils.isNotBlank(query.getManufacturerId())) {
            filters.put("manufacturerId", query.getManufacturerId());
        }

        List<OrderItem> orderItems = domainOrderItemService.filterAllUrgentFirst(filters).stream()
                .filter(item -> item != null && item.getQuantity() != null && item.getQuantity() != 0)
                .toList();
        return buildOrderItemVOs(orderItems);
    }

    private List<OrderItemVO> buildOrderItemVOs(List<OrderItem> orderItems) {
        List<OrderItemVO> result = new ArrayList<>();
        if (orderItems == null) {
            return result;
        }
        Map<String, OrderInfo> ordersById = domainOrderInfoService.findByOrderIds(orderItems.stream()
                        .filter(Objects::nonNull).map(OrderItem::getOrderId)
                        .filter(StringUtils::isNotBlank).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(OrderInfo::getOrderId, order -> order, (first, ignored) -> first));
        for (OrderItem item : orderItems) {
            if (item == null) {
                continue;
            }
            String oid = item.getOrderId();
            OrderInfo orderInfo = ordersById.get(oid);
            OrderItemVO orderWithItemsVO = new OrderItemVO();
            BeanUtils.copyProperties(item, orderWithItemsVO);
            if (orderInfo != null) {
                orderWithItemsVO.setCustomer(orderInfo.getCustomer());
                orderWithItemsVO.setRemark(orderInfo.getRemark());
                orderWithItemsVO.setOrgInfo(orderInfo.getOrgInfo());
                orderWithItemsVO.setManufacturerInfo(orderInfo.getManufacturerInfo());
                orderWithItemsVO.setKuaidiNum(orderInfo.getKuaidiNum());
                if (orderInfo.getPrice() != null && orderInfo.getPrice().getPaymentPrice() != null) {
                    orderWithItemsVO.setPaymentPrice(scaleStatisticsDecimal(orderInfo.getPrice().getPaymentPrice()));
                } else {
                    orderWithItemsVO.setPaymentPrice(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                }
            }
            result.add(orderWithItemsVO);
        }
        return result;
    }


    /**
     * 根据ID 获取订单详情（包含订单项）
     * @return 订单及订单项信息
     */
    public OrderWithItemsVO getOrderWithItemsById(OrderQuery query) {
        String id = query.getId();
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("订单 ID 不能为空");
        }

        OrderInfo order = domainOrderInfoService.findById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在：" + id);
        }

        OrderWithItemsVO vo = new OrderWithItemsVO();
        vo.setOrderInfo(order);

        List<OrderItem> orderItems = domainOrderItemService.findByOrderId(
                order.getOrderId(),
                query.getManufacturerId(),
                1,
                100
        );

        // 为每个订单项查询相关的生产工件
        if (orderItems != null && !orderItems.isEmpty()) {
            for (OrderItem item : orderItems) {
                List<ProductionPiece> productionPieces = productionPieceService.findProductionPiecesByOrderItemId(
                        item.getOrderItemId(),
                        1,
                        100
                );
                item.setProductionPieces(productionPieces != null ? productionPieces : new ArrayList<>());
            }
        }

        vo.setOrderItems(orderItems != null ? orderItems : new ArrayList<>());

        return vo;
    }

    /**
     * 根据订单号获取订单详情（包含订单项）
     *
     * @return 订单及订单项信息
     */
    public OrderWithItemsVO getOrderWithItemsByOrderId(OrderQuery query) {
        String orderId = query.getOrderId();
        if (StringUtils.isBlank(orderId)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        OrderInfo order = domainOrderInfoService.findByOrderId(orderId);
        if (order == null) {
            return null;
        }

        OrderWithItemsVO vo = new OrderWithItemsVO();
        vo.setOrderInfo(order);

        List<OrderItem> orderItems = domainOrderItemService.findByOrderId(
                order.getOrderId(),
                query.getManufacturerId(),
                1,
                100
        );

        // 为每个订单项查询相关的生产工件
        if (orderItems != null && !orderItems.isEmpty()) {
            for (OrderItem item : orderItems) {
                List<ProductionPiece> productionPieces = productionPieceService.findProductionPiecesByOrderItemId(
                        item.getOrderItemId(),
                        1,
                        100
                );
                item.setProductionPieces(productionPieces != null ? productionPieces : new ArrayList<>());
            }
        }

        vo.setOrderItems(orderItems != null ? orderItems : new ArrayList<>());

        return vo;
    }

    /**
     * 根据订单项 ID 获取详情（包含生产工件）
     *
     * @param orderItemId 订单项 ID
     * @return 订单项及生产工件信息
     */
    public OrderItem getOrderItemWithProductionPieces(String orderItemId) {
        if (StringUtils.isBlank(orderItemId)) {
            throw new IllegalArgumentException("订单项 ID 不能为空");
        }

        // 查询订单项
        OrderItem orderItem = domainOrderItemService.findByOrderItemId(orderItemId);
        if (orderItem == null) {
            throw new IllegalArgumentException("订单项不存在：" + orderItemId);
        }

        // 查询相关的生产工件
        List<ProductionPiece> productionPieces = productionPieceService.findProductionPiecesByOrderItemId(
                orderItemId,
                1,
                100
        );

        // 将生产工件列表设置到订单项中
        orderItem.setProductionPieces(productionPieces != null ? productionPieces : new ArrayList<>());

        return orderItem;
    }

    /**
     * 添加订单及订单项
     *
     * @return 添加后的订单信息
     */
    public OrderInfo addOrderWithItems(OrderAddRequest request) {
        //订单对象转化
        OrderInfo orderInfo = request.toOrderInfo();
        orderInfo.setOrgInfo(podvOrgInfoHelper.normalize(orderInfo.getPlatformCode(), orderInfo.getOrgInfo()));
        List<OrderItem> orderItems = request.toOrderItems();
        //先入库
        List<OrderItem> orderItemsResult = domainOrderInfoService.addOrderWithItems(orderInfo, orderItems);
        productionPieceGenerationTaskService.create(orderInfo.getOrderId(), orderItemsResult.stream()
                .map(OrderItem::getOrderItemId)
                .filter(Objects::nonNull)
                .toList());
        saveOrderDailyStatistics(orderInfo, orderItemsResult);
        preOrderLabelTaskService.createFromOrderInfo(orderInfo);
        // 灰度图转 SVG 必须先同步完成，之后才能进入其他异步预处理。
        List<OrderItem> readyToPreprocessOrderItems = appOrderPreprocessingService.convertMaskGrayImgToSvgIfNecessary(orderItemsResult);
        // 入库和必要的灰度图转 SVG 成功后立即返回，后续预处理改为异步队列执行
        log.info("addOrderWithItems 准备提交订单预处理任务: orderId={}, itemCount={}, readyItemCount={}",
                orderInfo.getOrderId(),
                orderItemsResult == null ? 0 : orderItemsResult.size(),
                readyToPreprocessOrderItems == null ? 0 : readyToPreprocessOrderItems.size());
        orderPreprocessTaskQueue.submit(readyToPreprocessOrderItems);
        log.info("addOrderWithItems 已提交订单预处理任务: orderId={}", orderInfo.getOrderId());
        return orderInfo;
    }

    /**
     * 批量添加订单及订单项。
     *
     * @return 添加后的订单信息列表
     */
    public List<OrderInfo> addOrdersWithItems(List<OrderAddRequest> requests) {
        List<OrderInfo> orderInfos = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            return orderInfos;
        }
        for (OrderAddRequest request : requests) {
            orderInfos.add(addOrderWithItems(request));
        }
        return orderInfos;
    }

    public OrderDailyStatistics findOrderDailyStatistics(String manufacturerMetaId, LocalDate statisticsDate) {
        if (StringUtils.isBlank(manufacturerMetaId) || statisticsDate == null) {
            return null;
        }
        return orderDailyStatisticsService.findByManufacturerMetaIdAndStatisticsDate(
                manufacturerMetaId,
                statisticsDate);
    }

    public OrderDailyStatistics sumOrderDailyStatistics(String manufacturerId, LocalDate startDate, LocalDate endDate) {
        if (StringUtils.isBlank(manufacturerId) || startDate == null || endDate == null) {
            return null;
        }
        return orderDailyStatisticsService.sumByManufacturerMetaIdAndStatisticsDateBetween(manufacturerId, startDate, endDate);
    }

    public OrderStatisticsFiltersVO findOrderStatisticsFilters(String manufacturerId,
                                                                LocalDate startDate,
                                                                LocalDate endDate) {
        if (StringUtils.isBlank(manufacturerId) || startDate == null || endDate == null) {
            throw new IllegalArgumentException("工厂和统计日期不能为空");
        }
        Map<OrderStatisticsType, LinkedHashMap<String, OrderStatisticsDimensionSummary>> dimensions = new HashMap<>();
        for (OrderDailyStatistics statistics : orderDailyStatisticsService.list(manufacturerId, startDate, endDate)) {
            if (statistics.getType() == null || StringUtils.isBlank(statistics.getIndexId())) {
                continue;
            }
            dimensions.computeIfAbsent(statistics.getType(), ignored -> new LinkedHashMap<>())
                    .compute(statistics.getIndexId(), (ignored, summary) -> {
                        if (summary == null) {
                            summary = new OrderStatisticsDimensionSummary(statistics.getIndexName());
                        } else if (StringUtils.isBlank(summary.indexName)
                                && StringUtils.isNotBlank(statistics.getIndexName())) {
                            summary.indexName = statistics.getIndexName();
                        }
                        summary.totalOrderCount += statistics.getTotalOrderCount() == null
                                ? 0L : statistics.getTotalOrderCount();
                        return summary;
                    });
        }
        return new OrderStatisticsFiltersVO(
                toDimensionVOs(dimensions.get(OrderStatisticsType.ENTERPRISE)),
                toDimensionVOs(dimensions.get(OrderStatisticsType.MATERIAL)),
                toDimensionVOs(dimensions.get(OrderStatisticsType.ROUTE)));
    }

    private List<OrderStatisticsDimensionVO> toDimensionVOs(Map<String, OrderStatisticsDimensionSummary> values) {
        if (values == null) return List.of();
        return values.entrySet().stream()
                .filter(entry -> entry.getValue().totalOrderCount > 0)
                .map(entry -> new OrderStatisticsDimensionVO(entry.getKey(), entry.getValue().indexName)).toList();
    }

    private static final class OrderStatisticsDimensionSummary {
        private String indexName;
        private long totalOrderCount;

        private OrderStatisticsDimensionSummary(String indexName) {
            this.indexName = indexName;
        }
    }

    private void saveOrderDailyStatistics(OrderInfo orderInfo, List<OrderItem> orderItems) {
        String manufacturerMetaId = resolveManufacturerMetaId(orderInfo, orderItems);
        if (StringUtils.isBlank(manufacturerMetaId)) {
            log.warn("addOrderWithItems 跳过订单统计，manufacturerMetaId 为空");
            return;
        }
        OrderStatisticsAmounts amounts = calculateStatisticsAmounts(orderInfo, orderItems);
        amounts.itemAllocations().forEach((orderItemId, price) -> {
            OrderItemPriceAllocation allocation = new OrderItemPriceAllocation();
            allocation.setOrderItemId(orderItemId);
            allocation.setManufacturerMetaId(manufacturerMetaId);
            allocation.setPrice(scaleStatisticsDecimal(price));
            orderItemPriceAllocationRepository.add(allocation);
        });
        incrementOrderDimensions(manufacturerMetaId, orderInfo, orderItems, 1, BigDecimal.ONE, amounts);
    }

    private void incrementOrderDimensions(String manufacturerMetaId, OrderInfo orderInfo, List<OrderItem> orderItems,
                                          long orderCount, BigDecimal multiplier) {
        incrementOrderDimensions(manufacturerMetaId, orderInfo, orderItems, orderCount, multiplier,
                calculateStatisticsAmounts(orderInfo, orderItems));
    }

    private void incrementOrderDimensions(String manufacturerMetaId, OrderInfo orderInfo, List<OrderItem> orderItems,
                                          long orderCount, BigDecimal multiplier, OrderStatisticsAmounts amounts) {
        incrementOrderDimensions(manufacturerMetaId, orderInfo, orderItems, orderCount, multiplier, amounts, null);
    }

    private void incrementOrderDimensions(String manufacturerMetaId, OrderInfo orderInfo, List<OrderItem> orderItems,
                                          long orderCount, BigDecimal multiplier, OrderStatisticsAmounts amounts,
                                          BigDecimal orderAmount) {
        incrementOrderDimensions(manufacturerMetaId, LocalDate.now(BEIJING_ZONE), orderInfo, orderItems,
                orderCount, multiplier, amounts, orderAmount);
    }

    private void incrementOrderDimensions(String manufacturerMetaId, LocalDate statisticsDate, OrderInfo orderInfo,
                                          List<OrderItem> orderItems, long orderCount, BigDecimal multiplier,
                                          OrderStatisticsAmounts amounts, BigDecimal orderAmount) {
        BigDecimal totalArea = orderItems.stream().map(this::calculateOrderItemArea)
                .reduce(BigDecimal.ZERO, BigDecimal::add).multiply(multiplier);
        BigDecimal manufacturerActualAmount = (orderAmount == null
                ? resolveManufacturerActualPrice(orderInfo) : orderAmount).multiply(multiplier);
        String enterpriseName = orderInfo.getOrgInfo() == null ? null : orderInfo.getOrgInfo().getName();
        String enterpriseId = orderInfo.getOrgId() == null ? enterpriseName : orderInfo.getOrgId().toString();
        if (StringUtils.isNotBlank(enterpriseId)) {
            incrementOrderDimension(manufacturerMetaId, statisticsDate, enterpriseId, enterpriseName,
                    OrderStatisticsType.ENTERPRISE, orderCount, totalArea, manufacturerActualAmount);
        }

        String routeId = StringUtils.isNotBlank(orderInfo.getRouteId()) ? orderInfo.getRouteId()
                : orderItems.stream().map(OrderItem::getRouteId).filter(StringUtils::isNotBlank).findFirst().orElse(null);
        incrementOrderDimension(manufacturerMetaId, statisticsDate,
                StringUtils.isBlank(routeId) ? NO_ROUTE_ID : routeId,
                StringUtils.isBlank(routeId) ? NO_ROUTE_NAME : resolveRouteName(routeId),
                OrderStatisticsType.ROUTE, orderCount, totalArea, manufacturerActualAmount);

        LinkedHashMap<String, String> materials = new LinkedHashMap<>();
        orderItems.stream().map(OrderItem::getMaterial).filter(Objects::nonNull).forEach(material -> {
            if (StringUtils.isNotBlank(material.getMaterialId())) {
                Object snapshotName = invokeGetter(invokeGetter(material, "getMaterialSnapshot"), "getName");
                materials.putIfAbsent(material.getMaterialId(), snapshotName == null ? null : snapshotName.toString());
            }
        });
        amounts.amountByMaterialId().keySet().forEach(materialId -> materials.putIfAbsent(materialId, null));
        materials.forEach((materialId, materialName) -> incrementOrderDimension(manufacturerMetaId, statisticsDate, materialId,
                materialName, OrderStatisticsType.MATERIAL, orderCount,
                amounts.areaByMaterialId().getOrDefault(materialId, BigDecimal.ZERO).multiply(multiplier),
                amounts.amountByMaterialId().getOrDefault(materialId, BigDecimal.ZERO).multiply(multiplier)));
    }

    private String resolveRouteName(String routeId) {
        DeliveryRoute route = deliveryRouteRepository.findByRouteId(routeId);
        return route == null ? null : route.getRouteName();
    }

    private void incrementOrderDimension(String manufacturerMetaId, LocalDate statisticsDate,
                                         String indexId, String indexName,
                                         OrderStatisticsType type,
                                         long orderCount, BigDecimal area, BigDecimal amount) {
        orderDailyStatisticsService.increment(
                manufacturerMetaId,
                statisticsDate,
                indexId,
                indexName,
                type,
                orderCount,
                scaleStatisticsDecimal(area),
                scaleStatisticsDecimal(amount));
    }

    private String resolveManufacturerMetaId(OrderInfo orderInfo, List<OrderItem> orderItems) {
        if (StringUtils.isNotBlank(orderInfo.getManufacturerId())) {
            return orderInfo.getManufacturerId();
        }
        return orderItems.stream().map(OrderItem::getManufacturerId)
                .filter(StringUtils::isNotBlank).findFirst().orElse(null);
    }

    private BigDecimal calculateOrderItemArea(OrderItem orderItem) {
        Object mtoProduct = orderItem.getMtoProduct();
        Object materialConfig = invokeGetter(mtoProduct, "getMaterialConfig");
        Object usageSize3D = invokeGetter(materialConfig, "getUsageSize3D");
        BigDecimal widthCm = toBigDecimal(invokeGetter(usageSize3D, "getWidth"));
        BigDecimal heightCm = toBigDecimal(invokeGetter(usageSize3D, "getHeight"));
        BigDecimal quantity = toBigDecimal(invokeGetter(mtoProduct, "getQuantity"));
        if (quantity.compareTo(BigDecimal.ZERO) == 0 && orderItem.getQuantity() != null) {
            quantity = BigDecimal.valueOf(orderItem.getQuantity());
        }
        return widthCm.multiply(heightCm).divide(BigDecimal.valueOf(10000)).multiply(quantity);
    }

    private BigDecimal scaleStatisticsDecimal(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private Object invokeGetter(Object target, String getterName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(getterName).invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 重新处理订单项：删除已生成的所有生产工件，并重新提交预处理任务。
     * 当传入 orderId 时，重新处理该订单下的所有订单项；否则按 orderItemId 重新处理单个订单项。
     *
     * @param orderItemId 订单项业务 ID
     * @param orderId 订单 ID
     * @return 被删除的生产工件数量
     */
    public long reprocessOrderItem(String orderItemId, String orderId) {
        if (StringUtils.isBlank(orderItemId) && StringUtils.isBlank(orderId)) {
            throw new IllegalArgumentException("订单项 ID 或订单 ID 不能为空");
        }

        List<OrderItem> orderItems = StringUtils.isNotBlank(orderId)
                ? findAllOrderItemsByOrderId(orderId.trim())
                : List.of(resolveOrderItem(orderItemId.trim()));

        long deletedCount = 0;
        for (OrderItem orderItem : orderItems) {
            // 先更新预处理请求 ID，使重做前已发出的异步算法回调在返回时被识别为过期并丢弃。
            orderItem.setPreprocessRequestId(IdGenerator.generateId("OPR"));
            domainOrderItemService.updateOrderItem(orderItem);
            deletedCount += productionPieceService.deleteProductionPiecesByOrderItemId(orderItem.getOrderItemId());
        }
        orderPreprocessTaskQueue.submit(orderItems);
        return deletedCount;
    }

    public long reprocessOrderItem(String orderItemId) {
        return reprocessOrderItem(orderItemId, null);
    }

    /**
     * 重新处理订单下所有待处理或处理失败的订单项。
     *
     * @param orderId 订单 ID
     * @return 被删除的生产工件数量
     */
    public long reprocessPendingOrFailedOrderItems(String orderId) {
        if (StringUtils.isBlank(orderId)) {
            throw new IllegalArgumentException("订单 ID 不能为空");
        }

        List<OrderItem> orderItems = findAllOrderItemsByOrderId(orderId.trim()).stream()
                .filter(orderItem -> orderItem.getStatus() == OrderStatus.PENDING
                        || orderItem.getStatus() == OrderStatus.FAILED)
                .toList();

        long deletedCount = 0;
        for (OrderItem orderItem : orderItems) {
            // 生成新的请求 ID，确保重新处理前发出的异步回调不会覆盖本次处理结果。
            orderItem.setPreprocessRequestId(IdGenerator.generateId("OPR"));
            domainOrderItemService.updateOrderItem(orderItem);
            deletedCount += productionPieceService.deleteProductionPiecesByOrderItemId(orderItem.getOrderItemId());
        }
        orderPreprocessTaskQueue.submit(orderItems);
        return deletedCount;
    }

    private OrderItem resolveOrderItem(String orderItemId) {
        OrderItem orderItem = domainOrderItemService.findByOrderItemId(orderItemId);
        if (orderItem == null) {
            throw new IllegalArgumentException("订单项不存在：" + orderItemId);
        }
        return orderItem;
    }

    private List<OrderItem> findAllOrderItemsByOrderId(String orderId) {
        List<OrderItem> orderItems = new ArrayList<>();
        int current = 1;
        int size = 100;
        while (true) {
            Map<String, Object> filters = new HashMap<>();
            filters.put("orderId", orderId);
            List<OrderItem> pageItems = domainOrderItemService.filterList(current, size, filters);
            if (pageItems == null || pageItems.isEmpty()) {
                break;
            }
            orderItems.addAll(pageItems);
            if (pageItems.size() < size) {
                break;
            }
            current++;
        }
        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("订单不存在或订单项为空：" + orderId);
        }
        return orderItems;
    }


    /**
     * 订单转单。
     * 将指定订单项的待生产数量转入目标工厂，并保留每个原订单项维度的转出记录。
     * request.targetId 使用目标工厂 manufacturerUser.account，再由账号关联 manufacturerMetaId。
     */
    public ApiResponse<String> transferOrder(OrderTransferRequest request) {
        if (request == null || StringUtils.isBlank(request.getOrderId())) {
            return doTransferOrder(request, null);
        }
        OrderInfo sourceOrderInfo = domainOrderInfoService.findByOrderId(request.getOrderId());
        if (sourceOrderInfo == null) {
            return doTransferOrder(request, null);
        }

        String transferLockToken = IdGenerator.generateId("OTL");
        Date expiredBefore = new Date(System.currentTimeMillis() - TRANSFER_LOCK_EXPIRATION_MS);
        if (!domainOrderInfoService.tryAcquireTransferLock(sourceOrderInfo.getId(), transferLockToken, expiredBefore)) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, "订单正在转单处理中，请勿重复转单");
        }
        try {
            return doTransferOrder(request, sourceOrderInfo);
        } finally {
            domainOrderInfoService.releaseTransferLock(sourceOrderInfo.getId(), transferLockToken);
        }
    }

    private ApiResponse<String> doTransferOrder(OrderTransferRequest request, OrderInfo lockedSourceOrderInfo) {
        if (request == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "转单参数不能为空");
        }
        if (StringUtils.isBlank(request.getOrderId())) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "订单号不能为空");
        }
        if (StringUtils.isBlank(request.getManufacturerMetaId())) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "转出工厂不能为空");
        }
        if (StringUtils.isBlank(request.getTargetId())) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "转入工厂不能为空");
        }
        if (request.getOrderItemDtos() == null || request.getOrderItemDtos().isEmpty()) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "转单订单项不能为空");
        }

        ManufacturerMeta sourceManufacturerMeta = findManufacturerMetaByManufacturerMetaId(request.getManufacturerMetaId());
        ManufacturerUser targetManufacturerUser = findUniqueManufacturerUserByAccount(request.getTargetId());
        ManufacturerMeta targetManufacturerMeta = targetManufacturerUser == null
                ? null
                : findManufacturerMetaByManufacturerMetaId(targetManufacturerUser.getManufacturerMetaId());
        if (sourceManufacturerMeta == null || targetManufacturerMeta == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "未匹配到正确的工厂，无法转单");
        }
        String targetManufacturerMetaId = targetManufacturerMeta.getManufacturerMetaId();

        OrderInfo sourceOrderInfo = lockedSourceOrderInfo != null
                ? lockedSourceOrderInfo
                : domainOrderInfoService.findByOrderId(request.getOrderId());
        if (sourceOrderInfo == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "订单不存在：" + request.getOrderId());
        }

        Map<String, OrderItem> orderItemById = new HashMap<>();
        Map<String, List<ProductionPiece>> productionPiecesByOrderItemId = new HashMap<>();
        List<OrderItem> sourceItemsBeforeTransfer = domainOrderItemService.findByOrderId(
                request.getOrderId(), request.getManufacturerMetaId(), 1, 100);
        for (OrderTransferRequest.OrderTransferItemDto itemDto : request.getOrderItemDtos()) {
            if (itemDto == null || StringUtils.isBlank(itemDto.getOrderItemId())) {
                return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "订单项 ID 不能为空");
            }
            if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "转单数量必须大于 0");
            }

            OrderItem orderItem = domainOrderItemService.findByOrderItemId(itemDto.getOrderItemId());
            if (orderItem == null
                    || !Objects.equals(request.getOrderId(), orderItem.getOrderId())
                    || !Objects.equals(request.getManufacturerMetaId(), orderItem.getManufacturerId())) {
                return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "订单项不存在：" + itemDto.getOrderItemId());
            }
            if (itemDto.getQuantity() > safeQuantity(orderItem.getQuantity())) {
                return ApiResponse.fail(ApiResponse.RepStatusCode.badParams,
                        itemDto.getOrderItemId() + "订单项转单数量不能大于自身数量");
            }

            List<ProductionPiece> productionPieces = productionPieceService.findProductionPiecesByOrderItemId(
                    orderItem.getOrderItemId(),
                    1,
                    100
            );
            List<ProductionPiece> safeProductionPieces = productionPieces != null ? productionPieces : new ArrayList<>();
            if (safeProductionPieces.stream().anyMatch(productionPiece ->
                    hasPendingTypesettingQuantityAtMost(productionPiece, itemDto.getQuantity()))) {
                return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError,
                        "转单数量必须小于该订单项所有零件的待排版数量");
            }
            if (safeProductionPieces.stream().anyMatch(this::hasQuantityAfterPendingTypesettingNode)) {
                return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, "该订单项已经开始生产，无法转单");
            }

            orderItemById.put(itemDto.getOrderItemId(), orderItem);
            productionPiecesByOrderItemId.put(itemDto.getOrderItemId(), safeProductionPieces);
        }
        OrderStatisticsAmounts transferredAmounts = calculateTransferStatisticsAmounts(
                request.getManufacturerMetaId(), request.getOrderItemDtos(), orderItemById);
        BigDecimal transferAmount = transferredAmounts.totalAmount();

        OrderInfo targetOrderInfo = copyOrderInfoForTransfer(sourceOrderInfo, targetManufacturerMeta);
        domainOrderInfoService.addOrder(targetOrderInfo);

        // 先让本次转单涉及的旧预处理回调失效，再修改/删除源订单项。否则部分转单时，
        // addOrderWithItems 发出的回调仍会命中保留下来的源订单项并重新生成零件。
        for (OrderItem sourceOrderItem : orderItemById.values()) {
            sourceOrderItem.setPreprocessRequestId(IdGenerator.generateId("OPR"));
            domainOrderItemService.updateOrderItem(sourceOrderItem);
        }

        List<OrderTransferRecord> transferRecords = new ArrayList<>();
        List<OrderItem> targetItemsToPreprocess = new ArrayList<>();
        List<OrderItem> transferredItems = new ArrayList<>();
        for (OrderTransferRequest.OrderTransferItemDto itemDto : request.getOrderItemDtos()) {
            OrderItem sourceOrderItem = orderItemById.get(itemDto.getOrderItemId());
            Integer transferQuantity = itemDto.getQuantity();
            String newOrderItemId = IdGenerator.generateOrderItemId();

            OrderItem targetOrderItem = copyOrderItemForTransfer(sourceOrderItem, newOrderItemId, targetManufacturerMetaId, transferQuantity);
            targetOrderItem = domainOrderItemService.addOrderItem(targetOrderItem);
            synchronizeTransferredItemPrices(request.getManufacturerMetaId(), targetManufacturerMetaId,
                    sourceOrderItem, targetOrderItem, transferQuantity);
            transferredItems.add(targetOrderItem);

            List<ProductionPiece> sourceProductionPieces = productionPiecesByOrderItemId.getOrDefault(
                    sourceOrderItem.getOrderItemId(), new ArrayList<>());
            for (ProductionPiece sourceProductionPiece : sourceProductionPieces) {
                ProductionPiece targetProductionPiece = copyProductionPieceForTransfer(
                        sourceProductionPiece,
                        newOrderItemId,
                        targetManufacturerMetaId,
                        transferQuantity
                );
                productionPieceService.addProductionPiece(targetProductionPiece);
            }
            // A source item can be transferred while its asynchronous preprocessing is still pending.
            // In that case there are no pieces to copy and the new item needs its own preprocessing task.
            if (sourceProductionPieces.isEmpty()) {
                targetItemsToPreprocess.add(targetOrderItem);
            }

            int remainQuantity = safeQuantity(sourceOrderItem.getQuantity()) - transferQuantity;
            if (remainQuantity == 0) {
                for (ProductionPiece productionPiece : productionPiecesByOrderItemId.getOrDefault(sourceOrderItem.getOrderItemId(), new ArrayList<>())) {
                    productionPieceService.deleteProductionPiece(productionPiece.getId());
                }
                domainOrderItemService.deleteOrderItem(sourceOrderItem.getId());
            } else {
                sourceOrderItem.setQuantity(remainQuantity);
                domainOrderItemService.updateOrderItem(sourceOrderItem);
                for (ProductionPiece productionPiece : productionPiecesByOrderItemId.getOrDefault(sourceOrderItem.getOrderItemId(), new ArrayList<>())) {
                    productionPiece.setQuantity(remainQuantity);
                    setPendingTypesettingNodeQuantity(productionPiece, remainQuantity);
                    productionPieceService.updateProductionPiece(productionPiece);
                }
            }

            transferRecords.add(buildOrderTransferRecord(
                    request,
                    sourceManufacturerMeta,
                    targetManufacturerMeta,
                    sourceOrderItem,
                    newOrderItemId,
                    transferQuantity
            ));
        }
        orderTransferRecordService.batchAdd(transferRecords);
        orderPreprocessTaskQueue.submit(targetItemsToPreprocess);
        List<OrderItem> sourceItemsAfterTransfer = domainOrderItemService.findByOrderId(
                request.getOrderId(), request.getManufacturerMetaId(), 1, 100);
        updateSourceOrderAfterTransfer(sourceOrderInfo, sourceItemsAfterTransfer, transferAmount);
        transferDailyStatisticsService.increment(
                request.getManufacturerMetaId(),
                targetManufacturerMetaId,
                targetManufacturerMeta == null ? null : targetManufacturerMeta.getName(),
                LocalDate.now(BEIJING_ZONE),
                1L,
                transferAmount);
        adjustTransferOrderDailyStatistics(request.getManufacturerMetaId(), sourceOrderInfo,
                sourceItemsBeforeTransfer, sourceItemsAfterTransfer, transferredAmounts);
        adjustTransferredInOrderDailyStatistics(targetManufacturerMetaId, targetOrderInfo,
                transferredItems, transferredAmounts);

        return ApiResponse.success("success");
    }

    /**
     * 按转单数量同步拆分订单项工厂实际价或已持久化的订单项金额分摊。
     */
    private void synchronizeTransferredItemPrices(String sourceManufacturerMetaId,
                                                   String targetManufacturerMetaId,
                                                   OrderItem sourceOrderItem,
                                                   OrderItem targetOrderItem,
                                                   int transferQuantity) {
        int sourceQuantity = safeQuantity(sourceOrderItem.getQuantity());
        if (sourceQuantity <= 0) {
            return;
        }
        int remainQuantity = sourceQuantity - transferQuantity;
        OrderItemPriceAllocation sourceAllocation = orderItemPriceAllocationRepository
                .findByOrderItemIdAndManufacturerMetaId(
                        sourceOrderItem.getOrderItemId(), sourceManufacturerMetaId);
        if (sourceAllocation != null && sourceAllocation.getPrice() != null) {
            BigDecimal transferredPrice = calculateTransferredItemPrice(
                    sourceAllocation.getPrice(), transferQuantity, sourceQuantity);
            OrderItemPriceAllocation targetAllocation = new OrderItemPriceAllocation();
            targetAllocation.setOrderItemId(targetOrderItem.getOrderItemId());
            targetAllocation.setManufacturerMetaId(targetManufacturerMetaId);
            targetAllocation.setPrice(transferredPrice);
            orderItemPriceAllocationRepository.add(targetAllocation);

            if (remainQuantity == 0) {
                orderItemPriceAllocationRepository.delete(sourceAllocation);
            } else {
                sourceAllocation.setPrice(scaleStatisticsDecimal(
                        sourceAllocation.getPrice().subtract(transferredPrice)));
                orderItemPriceAllocationRepository.update(sourceAllocation);
            }
            return;
        }

        if (sourceOrderItem.getManufacturerPrice() == null
                || sourceOrderItem.getManufacturerPrice().getActualPrice() == null
                || targetOrderItem.getManufacturerPrice() == null) {
            return;
        }
        BigDecimal sourceActualPrice = sourceOrderItem.getManufacturerPrice().getActualPrice();
        BigDecimal transferredPrice = calculateTransferredItemPrice(
                sourceActualPrice, transferQuantity, sourceQuantity);
        targetOrderItem.getManufacturerPrice().setActualPrice(transferredPrice);
        domainOrderItemService.updateOrderItem(targetOrderItem);
        if (remainQuantity > 0) {
            sourceOrderItem.getManufacturerPrice().setActualPrice(
                    scaleStatisticsDecimal(sourceActualPrice.subtract(transferredPrice)));
        }
    }

    private BigDecimal calculateTransferredItemPrice(BigDecimal sourcePrice,
                                                     int transferQuantity,
                                                     int sourceQuantity) {
        return scaleStatisticsDecimal(sourcePrice
                .multiply(BigDecimal.valueOf(transferQuantity))
                .divide(BigDecimal.valueOf(sourceQuantity), 12, RoundingMode.HALF_UP));
    }

    /**
     * 全部订单项均已转出时删除源订单；部分转出时同步扣减源订单的工厂实际价。
     */
    void updateSourceOrderAfterTransfer(OrderInfo sourceOrderInfo,
                                        List<OrderItem> sourceItemsAfterTransfer,
                                        BigDecimal transferAmount) {
        if (sourceItemsAfterTransfer != null && sourceItemsAfterTransfer.isEmpty()) {
            domainOrderInfoService.deleteOrder(sourceOrderInfo.getId());
            return;
        }

        ManufacturerInfo manufacturerInfo = sourceOrderInfo.getManufacturerInfo();
        if (manufacturerInfo == null || manufacturerInfo.getPrice() == null
                || manufacturerInfo.getPrice().getActualPrice() == null) {
            return;
        }
        BigDecimal safeTransferAmount = transferAmount == null ? BigDecimal.ZERO : transferAmount;
        if (manufacturerInfo.getPrice().getOriActualPrice() == null) {
            manufacturerInfo.getPrice().setOriActualPrice(manufacturerInfo.getPrice().getActualPrice());
        }
        manufacturerInfo.getPrice().setActualPrice(
                manufacturerInfo.getPrice().getActualPrice().subtract(safeTransferAmount));
        domainOrderInfoService.updateOrder(sourceOrderInfo);
    }


    /**
     * 分页查询指定工厂的转入记录。
     * manufacturerMetaId 对应转单记录 targetId。
     */
    public PagedResult<OrderTransferRecord> findTransferInRecords(String manufacturerMetaId, int current, int size) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("制造商 ID 不能为空");
        }
        List<OrderTransferRecord> items = orderTransferRecordService.findTransferInRecords(manufacturerMetaId, current, size);
        long total = orderTransferRecordService.countTransferInRecords(manufacturerMetaId);
        return new PagedResult<>(items, total, size, current);
    }

    /**
     * 分页查询指定工厂的转出记录。
     * manufacturerMetaId 对应转单记录 sourceId。
     */
    public PagedResult<OrderTransferRecord> findTransferOutRecords(String manufacturerMetaId, int current, int size) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("制造商 ID 不能为空");
        }
        List<OrderTransferRecord> items = orderTransferRecordService.findTransferOutRecords(manufacturerMetaId, current, size);
        long total = orderTransferRecordService.countTransferOutRecords(manufacturerMetaId);
        return new PagedResult<>(items, total, size, current);
    }

    /** 按转单时间和工厂流向分页查询目标订单项，并读取对应的持久化统计。 */
    public TransferOrderStatisticsVO findTransferOrderStatistics(String sourceId,
                                                                  String targetId,
                                                                  Date startTime,
                                                                  Date endTime,
                                                                  PagedQuery pagedQuery) {
        validateTransferStatisticsQuery(startTime, endTime);
        if (pagedQuery == null || pagedQuery.getCurrent() <= 0
                || pagedQuery.getSize() <= 0 || pagedQuery.getSize() > 100) {
            throw new IllegalArgumentException("分页参数不能为空且每页大小必须在 1-100 之间");
        }
        return findTransferOrderStatistics(sourceId, targetId, startTime, endTime, pagedQuery, false);
    }

    /** 按转单时间和工厂流向全量查询目标订单项，并读取对应的持久化统计。 */
    public TransferOrderStatisticsVO findAllTransferOrderStatistics(String sourceId,
                                                                     String targetId,
                                                                     Date startTime,
                                                                     Date endTime) {
        validateTransferStatisticsQuery(startTime, endTime);
        return findTransferOrderStatistics(sourceId, targetId, startTime, endTime, null, true);
    }

    private TransferOrderStatisticsVO findTransferOrderStatistics(String sourceId,
                                                                  String targetId,
                                                                  Date startTime,
                                                                  Date endTime,
                                                                  PagedQuery pagedQuery,
                                                                  boolean all) {
        List<OrderTransferRecord> transferRecords = orderTransferRecordService
                .findAllTransferRecords(sourceId, targetId, startTime, endTime);
        TransferOrderItemsQuery transferItemsQuery = buildTransferOrderItemsQuery(transferRecords, targetId);
        List<TransferOrderItemVO> items = List.of();
        long total = 0;
        if (!transferItemsQuery.orderIds().isEmpty()) {
            List<OrderItem> orderItems = all
                    ? domainOrderItemService.filterAllUrgentFirst(transferItemsQuery.filters())
                    : domainOrderItemService.filterListUrgentFirst(
                    (int) pagedQuery.getCurrent(), (int) pagedQuery.getSize(), transferItemsQuery.filters());
            items = buildTransferOrderItemVOs(orderItems, transferRecords);
            total = all ? items.size() : domainOrderItemService.filterTotal(transferItemsQuery.filters());
        }
        LocalDate startDate = startTime.toInstant().atZone(BEIJING_ZONE).toLocalDate();
        LocalDate endDate = endTime.toInstant().atZone(BEIJING_ZONE).toLocalDate();
        TransferDailyStatistics statistics = transferDailyStatisticsService.sum(
                sourceId, targetId, startDate, endDate);
        return new TransferOrderStatisticsVO(items, total,
                statistics == null ? 0L : statistics.getTotalOrderCount(),
                statistics == null ? BigDecimal.ZERO : statistics.getTotalAmount());
    }

    private void validateTransferStatisticsQuery(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }
        if (startTime.after(endTime)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    private TransferOrderItemsQuery buildTransferOrderItemsQuery(List<OrderTransferRecord> transferRecords,
                                                                 String targetId) {
        LinkedHashSet<String> orderIds = transferRecords
                .stream()
                .map(OrderTransferRecord::getOrderId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Object> filters = new HashMap<>();
        if (!orderIds.isEmpty()) {
            LinkedHashSet<String> targetOrderItemIds = transferRecords.stream()
                    .map(OrderTransferRecord::getTargetOrderItemId)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            boolean allRecordsHaveTargetItemId = targetOrderItemIds.size() == transferRecords.size();
            if (allRecordsHaveTargetItemId) {
                filters.put("orderItemId_in", targetOrderItemIds);
            } else {
                // Historical records did not retain the generated target item ID.
                filters.put("orderId_in", orderIds);
            }
            LinkedHashSet<String> targetIds = transferRecords.stream()
                    .map(OrderTransferRecord::getTargetId)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (StringUtils.isNotBlank(targetId)) {
                filters.put("manufacturerId", targetId);
            } else if (!allRecordsHaveTargetItemId && !targetIds.isEmpty()) {
                filters.put("manufacturerId_in", targetIds);
            }
        }
        return new TransferOrderItemsQuery(orderIds, filters);
    }

    private List<TransferOrderItemVO> buildTransferOrderItemVOs(List<OrderItem> orderItems,
                                                                List<OrderTransferRecord> transferRecords) {
        Map<String, OrderTransferRecord> recordByTargetItemId = transferRecords.stream()
                .filter(record -> StringUtils.isNotBlank(record.getTargetOrderItemId()))
                .collect(Collectors.toMap(OrderTransferRecord::getTargetOrderItemId, record -> record,
                        (first, ignored) -> first));
        Map<String, OrderTransferRecord> historicalRecordByOrderAndTarget = transferRecords.stream()
                .collect(Collectors.toMap(
                        record -> record.getOrderId() + "\u0000" + record.getTargetId(),
                        record -> record, (first, ignored) -> first));
        return buildOrderItemVOs(orderItems).stream().map(item -> {
            OrderTransferRecord record = recordByTargetItemId.get(item.getOrderItemId());
            if (record == null) {
                record = historicalRecordByOrderAndTarget.get(
                        item.getOrderId() + "\u0000" + item.getManufacturerId());
            }
            TransferOrderItemVO transferItem = new TransferOrderItemVO();
            BeanUtils.copyProperties(item, transferItem);
            if (record != null) {
                transferItem.setSourceId(record.getSourceId());
                transferItem.setSourceName(record.getSourceName());
                transferItem.setTargetId(record.getTargetId());
                transferItem.setTargetName(record.getTargetName());
            }
            return transferItem;
        }).toList();
    }

    private record TransferOrderItemsQuery(LinkedHashSet<String> orderIds, Map<String, Object> filters) {
    }

    /** 查询指定目标工厂在一段时间内的所有转单来源工厂。 */
    public List<TransferFactoryVO> findTransferSourceFactories(String targetId, Date startTime, Date endTime) {
        validateTransferFactoryQuery(targetId, startTime, endTime, "目标工厂不能为空");
        return distinctTransferFactories(
                orderTransferRecordService.findAllTransferRecords(null, targetId, startTime, endTime), true);
    }

    /** 查询指定来源工厂在一段时间内的所有转单目标工厂。 */
    public List<TransferFactoryVO> findTransferTargetFactories(String sourceId, Date startTime, Date endTime) {
        validateTransferFactoryQuery(sourceId, startTime, endTime, "来源工厂不能为空");
        return distinctTransferFactories(
                orderTransferRecordService.findAllTransferRecords(sourceId, null, startTime, endTime), false);
    }

    private void validateTransferFactoryQuery(String factoryId, Date startTime, Date endTime, String message) {
        if (StringUtils.isBlank(factoryId)) {
            throw new IllegalArgumentException(message);
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }
        if (startTime.after(endTime)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    private List<TransferFactoryVO> distinctTransferFactories(List<OrderTransferRecord> records, boolean source) {
        Map<String, String> factories = new LinkedHashMap<>();
        for (OrderTransferRecord record : records) {
            String id = source ? record.getSourceId() : record.getTargetId();
            String name = source ? record.getSourceName() : record.getTargetName();
            if (StringUtils.isNotBlank(id)) {
                factories.putIfAbsent(id, name);
            }
        }
        return factories.entrySet().stream()
                .map(entry -> new TransferFactoryVO(entry.getKey(), entry.getValue()))
                .toList();
    }


    /**
     * 修复历史转单记录。
     * 将历史记录中保存为 manufacturerUser.account 的 targetId 转换为对应 manufacturerMetaId。
     *
     * @return 修复结果
     */
    public ApiResponse<String> repairTransferRecordTargetIds() {
        List<OrderTransferRecord> records = orderTransferRecordService.findAllTransferRecords();
        int updatedCount = 0;
        int skippedCount = 0;
        Map<String, String> accountManufacturerMetaIdCache = new HashMap<>();
        List<OrderTransferRecord> recordsToUpdate = new ArrayList<>();

        for (OrderTransferRecord record : records) {
            if (record == null || StringUtils.isBlank(record.getTargetId())) {
                skippedCount++;
                continue;
            }

            String targetId = record.getTargetId();
            String manufacturerMetaId = accountManufacturerMetaIdCache.get(targetId);
            if (!accountManufacturerMetaIdCache.containsKey(targetId)) {
                ManufacturerUser manufacturerUser = findUniqueManufacturerUserByAccount(targetId);
                manufacturerMetaId = manufacturerUser == null ? null : manufacturerUser.getManufacturerMetaId();
                accountManufacturerMetaIdCache.put(targetId, manufacturerMetaId);
            }

            if (StringUtils.isBlank(manufacturerMetaId) || Objects.equals(targetId, manufacturerMetaId)) {
                skippedCount++;
                continue;
            }

            record.setTargetId(manufacturerMetaId);
            recordsToUpdate.add(record);
            updatedCount++;
        }

        orderTransferRecordService.batchUpdate(recordsToUpdate);
        return ApiResponse.success("修复完成，更新记录数：" + updatedCount + "，跳过记录数：" + skippedCount);
    }

    /**
     * 取消订单。
     * 根据订单号查询订单；平台号有值时，按订单号和平台号共同查询。若生产工件在“待排版”之后的任意节点已有数量，则不允许取消。
     *
     * @param platformCode 平台号（可为空）
     * @param orderId 订单号
     * @return 操作结果
     */
    public ApiResponse<String> cancelOrder(String platformCode, String orderId) {
        if (StringUtils.isBlank(orderId)) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "订单号不能为空");
        }

        OrderInfo orderInfo = domainOrderInfoService.findByOrderIdAndPlatformCode(orderId, platformCode);
        if (orderInfo == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "订单不存在：" + orderId);
        }
        if (orderInfo.getStatus() == OrderStatus.RETURNED) {
            return ApiResponse.success("success");
        }

        List<OrderItem> orderItems = domainOrderItemService.findByOrderId(orderId, null, 1, 100);
        if (orderItems == null || orderItems.isEmpty()) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.CANTCANCELORDER, "未找到对应订单项");
        }

        Map<String, List<ProductionPiece>> piecesByOrderItemId = new HashMap<>();
        for (OrderItem orderItem : orderItems) {
            List<ProductionPiece> productionPieces = productionPieceService.findProductionPiecesByOrderItemId(
                    orderItem.getOrderItemId(),
                    1,
                    99
            );
            List<ProductionPiece> safeProductionPieces = productionPieces != null ? productionPieces : new ArrayList<>();
            piecesByOrderItemId.put(orderItem.getOrderItemId(), safeProductionPieces);
            if (safeProductionPieces.stream().anyMatch(this::hasQuantityAfterPendingTypesettingNode)) {
                return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, "该订单已经开始生产，无法取消");
            }
        }

        orderInfo.setStatus(OrderStatus.RETURNED);
        domainOrderInfoService.updateOrder(orderInfo);

        for (OrderItem orderItem : orderItems) {
            orderItem.setStatus(OrderStatus.RETURNED);
            // 刷新预处理请求 ID，使取消前已发出的异步算法回调在返回时被识别为过期并丢弃。
            orderItem.setPreprocessRequestId(IdGenerator.generateId("OPR"));
            domainOrderItemService.updateOrderItem(orderItem);
            for (ProductionPiece productionPiece : piecesByOrderItemId.getOrDefault(orderItem.getOrderItemId(), new ArrayList<>())) {
                productionPieceService.deleteProductionPiece(productionPiece.getId());
            }
        }

        String manufacturerMetaId = StringUtils.isNotBlank(orderInfo.getManufacturerId())
                ? orderInfo.getManufacturerId()
                : orderItems.stream()
                        .map(OrderItem::getManufacturerId)
                        .filter(StringUtils::isNotBlank)
                        .findFirst()
                        .orElse(null);
        adjustOrderDailyStatistics(manufacturerMetaId, orderInfo, orderItems, -1, BigDecimal.valueOf(-1));

        return ApiResponse.success("success");
    }

    BigDecimal calculateStatisticsAmount(OrderInfo orderInfo, List<OrderItem> orderItems) {
        return calculateStatisticsAmounts(orderInfo, orderItems).totalAmount();
    }

    /**
     * 按实际转单数量计算转单金额。订单项有落库的金额分摊时以分摊金额为准，
     * 否则使用订单项的工厂实际价；两者均按“转单数量 / 转单前数量”等比分摊。
     */
    private OrderStatisticsAmounts calculateTransferStatisticsAmounts(String manufacturerMetaId,
                                                                       List<OrderTransferRequest.OrderTransferItemDto> transferItems,
                                                                       Map<String, OrderItem> sourceItemById) {
        if (transferItems == null || sourceItemById == null) {
            return new OrderStatisticsAmounts(scaleStatisticsDecimal(BigDecimal.ZERO), Map.of(), Map.of(), Map.of());
        }
        BigDecimal total = BigDecimal.ZERO;
        LinkedHashMap<String, BigDecimal> amountByMaterialId = new LinkedHashMap<>();
        for (OrderTransferRequest.OrderTransferItemDto transferItem : transferItems) {
            if (transferItem == null || transferItem.getQuantity() == null) {
                continue;
            }
            OrderItem sourceItem = sourceItemById.get(transferItem.getOrderItemId());
            int sourceQuantity = sourceItem == null ? 0 : safeQuantity(sourceItem.getQuantity());
            if (sourceQuantity <= 0) {
                continue;
            }
            OrderItemPriceAllocation allocation = orderItemPriceAllocationRepository
                    .findByOrderItemIdAndManufacturerMetaId(transferItem.getOrderItemId(), manufacturerMetaId);
            BigDecimal itemPrice = allocation != null && allocation.getPrice() != null
                    ? allocation.getPrice()
                    : resolveManufacturerActualPrice(sourceItem);
            BigDecimal transferredPrice = itemPrice
                    .multiply(BigDecimal.valueOf(transferItem.getQuantity()))
                    .divide(BigDecimal.valueOf(sourceQuantity), 12, RoundingMode.HALF_UP);
            total = total.add(transferredPrice);
            String materialId = resolveMaterialId(sourceItem);
            if (StringUtils.isNotBlank(materialId)) {
                amountByMaterialId.merge(materialId, transferredPrice, BigDecimal::add);
            }
        }
        amountByMaterialId.replaceAll((ignored, amount) -> scaleStatisticsDecimal(amount));
        return new OrderStatisticsAmounts(scaleStatisticsDecimal(total), amountByMaterialId, Map.of(), Map.of());
    }

    private OrderStatisticsAmounts calculateStatisticsAmounts(OrderInfo orderInfo, List<OrderItem> orderItems) {
        List<OrderItem> safeItems = orderItems == null ? List.of() : orderItems;
        LinkedHashMap<String, BigDecimal> amountByMaterialId = new LinkedHashMap<>();
        LinkedHashMap<String, BigDecimal> areaByMaterialId = new LinkedHashMap<>();
        LinkedHashMap<String, BigDecimal> itemAllocations = new LinkedHashMap<>();
        BigDecimal amountWithoutMaterial = BigDecimal.ZERO;
        for (OrderItem orderItem : safeItems) {
            String materialId = resolveMaterialId(orderItem);
            if (StringUtils.isBlank(materialId)) {
                continue;
            }
            areaByMaterialId.merge(materialId, calculateOrderItemArea(orderItem), BigDecimal::add);
        }

        List<ManufacturerInfo.FloorPriceEffectItem> effects = resolveFloorPriceEffects(orderInfo);
        Set<String> matchedOrderItemIds = new HashSet<>();
        for (ManufacturerInfo.FloorPriceEffectItem effect : effects) {
            List<OrderItem> matchedItems = safeItems.stream()
                    .filter(item -> matchesFloorPriceEffect(item, effect))
                    .toList();
            BigDecimal actualPriceTotal = matchedItems.stream()
                    .map(this::resolveManufacturerActualPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (actualPriceTotal.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            for (OrderItem item : matchedItems) {
                BigDecimal allocatedPrice = effect.getFloorPrice()
                        .multiply(resolveManufacturerActualPrice(item))
                        .divide(actualPriceTotal, 12, RoundingMode.HALF_UP);
                itemAllocations.merge(item.getOrderItemId(), allocatedPrice, BigDecimal::add);
                matchedOrderItemIds.add(item.getOrderItemId());
            }
        }

        for (OrderItem orderItem : safeItems) {
            String orderItemId = orderItem.getOrderItemId();
            BigDecimal itemAmount = matchedOrderItemIds.contains(orderItemId)
                    ? itemAllocations.getOrDefault(orderItemId, BigDecimal.ZERO)
                    : resolveActualPrice(orderItem);
            if (!effects.isEmpty()) {
                itemAllocations.putIfAbsent(orderItemId, itemAmount);
            }
            String materialId = resolveMaterialId(orderItem);
            if (StringUtils.isBlank(materialId)) {
                amountWithoutMaterial = amountWithoutMaterial.add(itemAmount);
            } else {
                amountByMaterialId.merge(materialId, itemAmount, BigDecimal::add);
            }
        }
        BigDecimal totalAmount = amountByMaterialId.values().stream()
                .reduce(amountWithoutMaterial, BigDecimal::add);
        return new OrderStatisticsAmounts(totalAmount, amountByMaterialId, areaByMaterialId, itemAllocations);
    }

    private List<ManufacturerInfo.FloorPriceEffectItem> resolveFloorPriceEffects(OrderInfo orderInfo) {
        ManufacturerInfo manufacturerInfo = orderInfo == null ? null : orderInfo.getManufacturerInfo();
        ManufacturerInfo.FloorPriceEffectManifest manifest = manufacturerInfo == null
                ? null : manufacturerInfo.getFloorPriceEffectManifest();
        List<ManufacturerInfo.FloorPriceEffectItem> floorPriceItems = manifest == null
                ? null : manifest.getFloorPriceEffectItems();
        if (floorPriceItems == null) return List.of();
        return floorPriceItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> ("MATERIAL".equalsIgnoreCase(item.getRefType())
                        || "PROCESS".equalsIgnoreCase(item.getRefType())))
                .filter(item -> StringUtils.isNotBlank(item.getRefId()) && item.getFloorPrice() != null)
                .toList();
    }

    private boolean matchesFloorPriceEffect(OrderItem orderItem, ManufacturerInfo.FloorPriceEffectItem effect) {
        if ("MATERIAL".equalsIgnoreCase(effect.getRefType())) {
            return effect.getRefId().equals(resolveMaterialId(orderItem));
        }
        ProcedureFlow flow = orderItem == null ? null : orderItem.getProcedureFlow();
        return flow != null && flow.getNodes() != null && flow.getNodes().stream()
                .filter(Objects::nonNull)
                .flatMap(node -> node.getParamConfigs() == null ? Stream.empty() : node.getParamConfigs().stream())
                .anyMatch(config -> effect.getRefId().equals(String.valueOf(invokeGetter(config, "getProcessParamMetaId"))));
    }

    private String resolveMaterialId(OrderItem orderItem) {
        return orderItem == null || orderItem.getMaterial() == null
                ? null : orderItem.getMaterial().getMaterialId();
    }

    private BigDecimal resolveActualPrice(OrderItem orderItem) {
        return orderItem == null || orderItem.getPrice() == null || orderItem.getPrice().getActualPrice() == null
                ? BigDecimal.ZERO : orderItem.getPrice().getActualPrice();
    }

    private BigDecimal resolveManufacturerActualPrice(OrderItem orderItem) {
        return orderItem == null || orderItem.getManufacturerPrice() == null
                || orderItem.getManufacturerPrice().getActualPrice() == null
                ? BigDecimal.ZERO : orderItem.getManufacturerPrice().getActualPrice();
    }

    BigDecimal resolveManufacturerActualPrice(OrderInfo orderInfo) {
        ManufacturerInfo manufacturerInfo = orderInfo == null ? null : orderInfo.getManufacturerInfo();
        if (manufacturerInfo != null && manufacturerInfo.getPrice() != null
                && manufacturerInfo.getPrice().getActualPrice() != null) {
            return manufacturerInfo.getPrice().getActualPrice();
        }
        return orderInfo == null || orderInfo.getPrice() == null || orderInfo.getPrice().getActualPrice() == null
                ? BigDecimal.ZERO : orderInfo.getPrice().getActualPrice();
    }

    private void adjustOrderDailyStatistics(String manufacturerMetaId, OrderInfo orderInfo,
                                            List<OrderItem> orderItems, long orderCount,
                                            BigDecimal multiplier) {
        if (StringUtils.isBlank(manufacturerMetaId) || orderItems == null || orderItems.isEmpty()) {
            return;
        }
        incrementOrderDimensions(manufacturerMetaId, orderInfo, orderItems, orderCount, multiplier);
    }

    private void adjustTransferOrderDailyStatistics(String manufacturerMetaId, OrderInfo orderInfo,
                                                    List<OrderItem> itemsBeforeTransfer,
                                                    List<OrderItem> itemsAfterTransfer,
                                                    OrderStatisticsAmounts transferredAmounts) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            return;
        }
        OrderStatisticsAmounts beforeAmounts = calculateStatisticsAmounts(orderInfo, itemsBeforeTransfer);
        OrderStatisticsAmounts transferredBeforeAmounts = new OrderStatisticsAmounts(
                transferredAmounts.totalAmount(), transferredAmounts.amountByMaterialId(),
                beforeAmounts.areaByMaterialId(), Map.of());
        incrementOrderDimensions(manufacturerMetaId, orderInfo, itemsBeforeTransfer, -1,
                BigDecimal.valueOf(-1), transferredBeforeAmounts, transferredAmounts.totalAmount());

        if (itemsAfterTransfer != null && !itemsAfterTransfer.isEmpty()) {
            OrderStatisticsAmounts afterAmounts = calculateStatisticsAmounts(orderInfo, itemsAfterTransfer);
            OrderStatisticsAmounts zeroAmountAfterTransfer = new OrderStatisticsAmounts(
                    BigDecimal.ZERO, Map.of(), afterAmounts.areaByMaterialId(), Map.of());
            incrementOrderDimensions(manufacturerMetaId, orderInfo, itemsAfterTransfer, 1,
                    BigDecimal.ONE, zeroAmountAfterTransfer, BigDecimal.ZERO);
        }
    }

    private void adjustTransferredInOrderDailyStatistics(String manufacturerMetaId, OrderInfo orderInfo,
                                                          List<OrderItem> transferredItems,
                                                          OrderStatisticsAmounts transferredAmounts) {
        if (StringUtils.isBlank(manufacturerMetaId) || transferredItems == null || transferredItems.isEmpty()) {
            return;
        }
        OrderStatisticsAmounts targetAmounts = calculateStatisticsAmounts(orderInfo, transferredItems);
        OrderStatisticsAmounts transferredTargetAmounts = new OrderStatisticsAmounts(
                transferredAmounts.totalAmount(), transferredAmounts.amountByMaterialId(),
                targetAmounts.areaByMaterialId(), Map.of());
        incrementOrderDimensions(manufacturerMetaId, orderInfo, transferredItems, 1,
                BigDecimal.ONE, transferredTargetAmounts, transferredAmounts.totalAmount());
    }

    private record OrderStatisticsAmounts(BigDecimal totalAmount,
                                          Map<String, BigDecimal> amountByMaterialId,
                                          Map<String, BigDecimal> areaByMaterialId,
                                          Map<String, BigDecimal> itemAllocations) {
    }


    private ManufacturerMeta findManufacturerMetaByManufacturerMetaId(String manufacturerMetaId) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            return null;
        }
        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerMetaId", manufacturerMetaId);
        List<ManufacturerMeta> results = manufacturerMetaRepository.filterList(1, 1, filters);
        return results == null || results.isEmpty() ? null : results.get(0);
    }

    private ManufacturerUser findUniqueManufacturerUserByAccount(String account) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        List<ManufacturerUser> users = manufacturerUserRepository.listByAccount(account);
        if (users == null || users.isEmpty()) {
            return null;
        }
        if (users.size() > 1) {
            throw new IllegalArgumentException("查询到多条账号信息，无法转单");
        }
        return users.get(0);
    }

    private OrderInfo copyOrderInfoForTransfer(OrderInfo sourceOrderInfo, ManufacturerMeta targetManufacturerMeta) {
        OrderInfo targetOrderInfo = deepCopy(sourceOrderInfo, OrderInfo.class);
        targetOrderInfo.setId(null);
        targetOrderInfo.setCreateTime(null);
        targetOrderInfo.setUpdateTime(null);
        if (targetManufacturerMeta != null) {
            targetOrderInfo.setManufacturerId(targetManufacturerMeta.getManufacturerMetaId());
            targetOrderInfo.setManufacturerName(targetManufacturerMeta.getName());
            ManufacturerInfo manufacturerInfo = targetOrderInfo.getManufacturerInfo();
            if (manufacturerInfo == null) {
                manufacturerInfo = new ManufacturerInfo();
                targetOrderInfo.setManufacturerInfo(manufacturerInfo);
            }
            manufacturerInfo.setId(targetManufacturerMeta.getManufacturerMetaId());
            manufacturerInfo.setName(targetManufacturerMeta.getName());
        }
        return targetOrderInfo;
    }

    private OrderItem copyOrderItemForTransfer(OrderItem sourceOrderItem,
                                               String newOrderItemId,
                                               String targetManufacturerMetaId,
                                               Integer quantity) {
        OrderItem targetOrderItem = deepCopy(sourceOrderItem, OrderItem.class);
        targetOrderItem.setId(null);
        targetOrderItem.setCreateTime(null);
        targetOrderItem.setUpdateTime(null);
        targetOrderItem.setOrderItemId(newOrderItemId);
        targetOrderItem.setManufacturerId(targetManufacturerMetaId);
        targetOrderItem.setQuantity(quantity);
        targetOrderItem.setProductionPieces(null);
        normalizeProcedureFlowForTransfer(targetOrderItem.getProcedureFlow(), quantity);
        return targetOrderItem;
    }

    private ProductionPiece copyProductionPieceForTransfer(ProductionPiece sourceProductionPiece,
                                                           String newOrderItemId,
                                                           String targetManufacturerMetaId,
                                                           Integer quantity) {
        ProductionPiece targetProductionPiece = deepCopy(sourceProductionPiece, ProductionPiece.class);
        targetProductionPiece.setId(null);
        targetProductionPiece.setCreateTime(null);
        targetProductionPiece.setUpdateTime(null);
        targetProductionPiece.setProductionPieceId(null);
        targetProductionPiece.setOrderItemId(newOrderItemId);
        targetProductionPiece.setManufacturerId(targetManufacturerMetaId);
        targetProductionPiece.setQuantity(quantity);
        normalizeProcedureFlowForTransfer(targetProductionPiece.getProcedureFlow(), quantity);
        return targetProductionPiece;
    }

    private OrderTransferRecord buildOrderTransferRecord(OrderTransferRequest request,
                                                          ManufacturerMeta sourceManufacturerMeta,
                                                          ManufacturerMeta targetManufacturerMeta,
                                                          OrderItem sourceOrderItem,
                                                          String targetOrderItemId,
                                                          Integer quantity) {
        OrderTransferRecord record = new OrderTransferRecord();
        record.setOrderId(request.getOrderId());
        record.setSourceId(request.getManufacturerMetaId());
        record.setSourceName(sourceManufacturerMeta == null ? null : sourceManufacturerMeta.getName());
        record.setTargetId(targetManufacturerMeta == null ? null : targetManufacturerMeta.getManufacturerMetaId());
        record.setTargetName(targetManufacturerMeta == null ? null : targetManufacturerMeta.getName());
        record.setOrderItemId(sourceOrderItem.getOrderItemId());
        record.setTargetOrderItemId(targetOrderItemId);
        record.setPreviewUrl(extractPreviewUrl(sourceOrderItem.getProductionImgFile()));
        record.setQuantity(quantity);
        return record;
    }

    private void normalizeProcedureFlowForTransfer(com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow procedureFlow,
                                                    Integer quantity) {
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            return;
        }
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node == null) {
                continue;
            }
            if (Objects.equals("待排版", node.getNodeName())) {
                node.setPieceQuantity(quantity);
            } else {
                node.setPieceQuantity(0);
            }
        }
    }

    private void setPendingTypesettingNodeQuantity(ProductionPiece productionPiece, Integer quantity) {
        if (productionPiece == null
                || productionPiece.getProcedureFlow() == null
                || productionPiece.getProcedureFlow().getNodes() == null) {
            return;
        }
        for (ProcedureFlowNode node : productionPiece.getProcedureFlow().getNodes()) {
            if (node != null && Objects.equals("待排版", node.getNodeName())) {
                node.setPieceQuantity(quantity);
            }
        }
    }

    private String extractPreviewUrl(ImageFile imageFile) {
        if (imageFile == null || imageFile.getFilePreview() == null) {
            return null;
        }
        return imageFile.getFilePreview().getPreview();
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    private <T> T deepCopy(T source, Class<T> clazz) {
        if (source == null) {
            return null;
        }
        return JSON.parseObject(JSON.toJSONString(source), clazz);
    }

    private boolean hasQuantityAfterPendingTypesettingNode(ProductionPiece productionPiece) {
        if (productionPiece == null
                || productionPiece.getProcedureFlow() == null
                || productionPiece.getProcedureFlow().getNodes() == null
                || productionPiece.getProcedureFlow().getNodes().isEmpty()) {
            return false;
        }

        List<ProcedureFlowNode> nodes = productionPiece.getProcedureFlow().getNodes();
        int pendingTypesettingIndex = -1;
        Integer pendingTypesettingOrder = null;
        for (int i = 0; i < nodes.size(); i++) {
            ProcedureFlowNode node = nodes.get(i);
            if (node != null && Objects.equals("待排版", node.getNodeName())) {
                pendingTypesettingIndex = i;
                pendingTypesettingOrder = node.getNodeOrder();
                break;
            }
        }
        if (pendingTypesettingIndex < 0) {
            return false;
        }

        for (int i = 0; i < nodes.size(); i++) {
            ProcedureFlowNode node = nodes.get(i);
            if (node == null || node.getPieceQuantity() == null || node.getPieceQuantity() <= 0) {
                continue;
            }
            boolean isAfterPendingTypesetting = pendingTypesettingOrder != null && node.getNodeOrder() != null
                    ? node.getNodeOrder() > pendingTypesettingOrder
                    : i > pendingTypesettingIndex;
            if (isAfterPendingTypesetting) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPendingTypesettingQuantityAtMost(ProductionPiece productionPiece, int transferQuantity) {
        if (productionPiece == null
                || productionPiece.getProcedureFlow() == null
                || productionPiece.getProcedureFlow().getNodes() == null) {
            return false;
        }
        return productionPiece.getProcedureFlow().getNodes().stream()
                .filter(Objects::nonNull)
                .filter(node -> Objects.equals("待排版", node.getNodeName()))
                .map(ProcedureFlowNode::getPieceQuantity)
                .filter(Objects::nonNull)
                .anyMatch(pendingQuantity -> transferQuantity >= pendingQuantity);
    }

    /**
     * 切换订单项加急状态。
     *
     * @param id 订单项 MongoDB ID
     */
    public void toggleOrderItemUrgent(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("订单项 ID 不能为空");
        }

        OrderItem orderItem = domainOrderItemService.findById(id);
        if (orderItem == null) {
            throw new IllegalArgumentException("订单项不存在：" + id);
        }
        if (StringUtils.isBlank(orderItem.getOrderItemId())) {
            throw new IllegalArgumentException("订单项业务 ID 不能为空：" + id);
        }

        // 切换当前状态（true -> false, false -> true）
        Boolean currentStatus = orderItem.getIsUrgent() != null ? orderItem.getIsUrgent() : false;
        Boolean nextStatus = !currentStatus;
        orderItem.setIsUrgent(nextStatus);
        domainOrderItemService.updateOrderItem(orderItem);

        // productionPiece.orderItemId 保存的是订单项业务 ID，不是 MongoDB _id。
        productionPieceService.updateUrgentByOrderItemId(orderItem.getOrderItemId(), nextStatus);
    }
}
