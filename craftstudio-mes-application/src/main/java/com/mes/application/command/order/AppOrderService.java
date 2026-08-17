package com.mes.application.command.order;

import com.mes.application.command.order.vo.OrderItemVO;
import com.mes.application.command.order.vo.OrderPackagingSyncResult;
import com.mes.application.command.order.vo.OrderQuery;
import com.mes.application.command.order.vo.OrderWithItemsVO;
import com.mes.application.command.orderPreprocessing.OrderPreprocessTaskQueue;
import com.mes.application.command.statistics.vo.OrderStatisticsItemVO;
import com.mes.application.command.statistics.vo.OrderStatisticsMaterialVO;
import com.mes.application.command.statistics.vo.OrderStatisticsListVO;
import com.mes.application.command.statistics.vo.OrderStatisticsStatusVO;
import com.mes.application.command.statistics.vo.OrderStatisticsDimensionVO;
import com.mes.application.command.statistics.vo.OrderStatisticsFiltersVO;
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
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.mes.domain.order.orderStatistics.entity.OrderStatisticsType;
import com.mes.domain.order.orderStatistics.service.OrderDailyStatisticsService;
import com.mes.domain.order.orderTransferRecord.entity.OrderTransferRecord;
import com.mes.domain.order.preOrderLabelTask.service.PreOrderLabelTaskService;
import com.mes.domain.order.orderTransferRecord.service.OrderTransferRecordService;
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

@Service
public class AppOrderService {

    private static final Logger log = LoggerFactory.getLogger(AppOrderService.class);
    private static final long TRANSFER_LOCK_EXPIRATION_MS = 24 * 60 * 60 * 1000L;

    @Autowired
    private OrderInfoService domainOrderInfoService;

    @Autowired
    private OrderItemService domainOrderItemService;

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
    private DeliveryRouteRepository deliveryRouteRepository;

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

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
        Map<String, String> routeNameByRouteId = deliveryRouteRepository.findByRouteIds(orderItems.stream()
                        .map(OrderItem::getRouteId)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream()
                .filter(route -> StringUtils.isNotBlank(route.getRouteId()) && route.getRouteName() != null)
                .collect(Collectors.toMap(DeliveryRoute::getRouteId, DeliveryRoute::getRouteName,
                        (first, ignored) -> first));
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
        Map<String, String> routeNameByRouteId = deliveryRouteRepository.findByRouteIds(orderItems.stream()
                        .map(OrderItem::getRouteId).filter(StringUtils::isNotBlank)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream()
                .filter(route -> StringUtils.isNotBlank(route.getRouteId()) && route.getRouteName() != null)
                .collect(Collectors.toMap(DeliveryRoute::getRouteId, DeliveryRoute::getRouteName,
                        (first, ignored) -> first));
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
        result.setOrderItemPrice(calculateStatisticsAmount(item));
        return result;
    }

    private OrderDailyStatistics findPersistedStatisticsTotals(String manufacturerId, Date startTime, Date endTime,
                                                               String routeId, String materialId, String orgName) {
        if (StringUtils.isBlank(manufacturerId) || startTime == null || endTime == null) return null;
        String indexId;
        OrderStatisticsType type;
        if (StringUtils.isNotBlank(materialId)) {
            indexId = materialId;
            type = OrderStatisticsType.MATERIAL;
        } else if (StringUtils.isNotBlank(routeId)) {
            indexId = routeId;
            type = OrderStatisticsType.ROUTE;
        } else if (StringUtils.isNotBlank(orgName)) {
            indexId = orgName;
            type = OrderStatisticsType.ENTERPRISE;
        } else {
            indexId = null;
            type = OrderStatisticsType.ENTERPRISE;
        }
        return orderDailyStatisticsService.sum(manufacturerId,
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
        orderInfo.setOrgInfo(PodvOrgInfoHelper.normalize(orderInfo.getPlatformCode(), orderInfo.getOrgInfo()));
        List<OrderItem> orderItems = request.toOrderItems();
        //先入库
        List<OrderItem> orderItemsResult = domainOrderInfoService.addOrderWithItems(orderInfo, orderItems);
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
        return orderDailyStatisticsService.findByManufacturerMetaIdAndStatisticsDate(manufacturerMetaId, statisticsDate);
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
        Map<OrderStatisticsType, LinkedHashMap<String, String>> dimensions = new HashMap<>();
        for (OrderDailyStatistics statistics : orderDailyStatisticsService.list(manufacturerId, startDate, endDate)) {
            if (statistics.getType() == null || StringUtils.isBlank(statistics.getIndexId())) {
                continue;
            }
            dimensions.computeIfAbsent(statistics.getType(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(statistics.getIndexId(), statistics.getIndexName());
        }
        return new OrderStatisticsFiltersVO(
                toDimensionVOs(dimensions.get(OrderStatisticsType.ENTERPRISE)),
                toDimensionVOs(dimensions.get(OrderStatisticsType.MATERIAL)),
                toDimensionVOs(dimensions.get(OrderStatisticsType.ROUTE)));
    }

    private List<OrderStatisticsDimensionVO> toDimensionVOs(Map<String, String> values) {
        if (values == null) return List.of();
        return values.entrySet().stream()
                .map(entry -> new OrderStatisticsDimensionVO(entry.getKey(), entry.getValue())).toList();
    }

    private void saveOrderDailyStatistics(OrderInfo orderInfo, List<OrderItem> orderItems) {
        String manufacturerMetaId = resolveManufacturerMetaId(orderInfo, orderItems);
        if (StringUtils.isBlank(manufacturerMetaId)) {
            log.warn("addOrderWithItems 跳过订单统计，manufacturerMetaId 为空");
            return;
        }
        BigDecimal totalArea = orderItems.stream()
                .map(this::calculateOrderItemArea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = orderItems.stream()
                .map(this::calculateStatisticsAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        incrementOrderDimensions(manufacturerMetaId, orderInfo, orderItems, 1, totalArea, totalAmount);
    }

    private void incrementOrderDimensions(String manufacturerMetaId, OrderInfo orderInfo, List<OrderItem> orderItems,
                                          long orderCount, BigDecimal area, BigDecimal amount) {
        Set<String> enterpriseNames = orderItems.stream()
                .map(OrderItem::getOrgInfo)
                .filter(Objects::nonNull)
                .map(orgInfo -> orgInfo.getName())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (enterpriseNames.isEmpty() && orderInfo.getOrgInfo() != null
                && StringUtils.isNotBlank(orderInfo.getOrgInfo().getName())) {
            enterpriseNames.add(orderInfo.getOrgInfo().getName());
        }
        enterpriseNames.forEach(enterpriseName -> incrementOrderDimension(manufacturerMetaId, enterpriseName, enterpriseName,
                OrderStatisticsType.ENTERPRISE, orderCount, area, amount));

        orderItems.stream()
                .map(OrderItem::getRouteId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .forEach(routeId -> incrementOrderDimension(manufacturerMetaId, routeId, resolveRouteName(routeId),
                        OrderStatisticsType.ROUTE, orderCount, area, amount));

        LinkedHashMap<String, String> materials = new LinkedHashMap<>();
        orderItems.stream().map(OrderItem::getMaterial).filter(Objects::nonNull).forEach(material -> {
            if (StringUtils.isNotBlank(material.getMaterialId())) {
                Object snapshotName = invokeGetter(invokeGetter(material, "getMaterialSnapshot"), "getName");
                materials.putIfAbsent(material.getMaterialId(), snapshotName == null ? null : snapshotName.toString());
            }
        });
        materials.forEach((materialId, materialName) -> incrementOrderDimension(manufacturerMetaId, materialId,
                materialName, OrderStatisticsType.MATERIAL, orderCount, area, amount));
    }

    private String resolveRouteName(String routeId) {
        DeliveryRoute route = deliveryRouteRepository.findByRouteId(routeId);
        return route == null ? null : route.getRouteName();
    }

    private void incrementOrderDimension(String manufacturerMetaId, String indexId, String indexName,
                                         OrderStatisticsType type,
                                         long orderCount, BigDecimal area, BigDecimal amount) {
        orderDailyStatisticsService.increment(
                manufacturerMetaId,
                LocalDate.now(BEIJING_ZONE),
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
        BigDecimal transferredAmount = BigDecimal.ZERO;
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
            if (safeProductionPieces.stream().anyMatch(this::hasQuantityAfterPendingTypesettingNode)) {
                return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, "该订单项已经开始生产，无法转单");
            }

            orderItemById.put(itemDto.getOrderItemId(), orderItem);
            productionPiecesByOrderItemId.put(itemDto.getOrderItemId(), safeProductionPieces);
            transferredAmount = transferredAmount.add(calculateTransferredAmount(
                    orderItem, itemDto.getQuantity()));
        }

        OrderInfo targetOrderInfo = copyOrderInfoForTransfer(sourceOrderInfo);
        domainOrderInfoService.addOrder(targetOrderInfo);

        // 先让本次转单涉及的旧预处理回调失效，再修改/删除源订单项。否则部分转单时，
        // addOrderWithItems 发出的回调仍会命中保留下来的源订单项并重新生成零件。
        for (OrderItem sourceOrderItem : orderItemById.values()) {
            sourceOrderItem.setPreprocessRequestId(IdGenerator.generateId("OPR"));
            domainOrderItemService.updateOrderItem(sourceOrderItem);
        }

        List<OrderTransferRecord> transferRecords = new ArrayList<>();
        List<OrderItem> targetItemsToPreprocess = new ArrayList<>();
        for (OrderTransferRequest.OrderTransferItemDto itemDto : request.getOrderItemDtos()) {
            OrderItem sourceOrderItem = orderItemById.get(itemDto.getOrderItemId());
            Integer transferQuantity = itemDto.getQuantity();
            String newOrderItemId = IdGenerator.generateOrderItemId();

            OrderItem targetOrderItem = copyOrderItemForTransfer(sourceOrderItem, newOrderItemId, targetManufacturerMetaId, transferQuantity);
            domainOrderItemService.addOrderItem(targetOrderItem);

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
                    transferQuantity
            ));
        }
        orderTransferRecordService.batchAdd(transferRecords);
        orderPreprocessTaskQueue.submit(targetItemsToPreprocess);
        List<OrderItem> transferredItems = request.getOrderItemDtos().stream()
                .map(dto -> orderItemById.get(dto.getOrderItemId())).toList();
        adjustOrderDailyStatisticsAmount(request.getManufacturerMetaId(), sourceOrderInfo,
                transferredItems, transferredAmount.negate());
        adjustOrderDailyStatisticsAmount(targetManufacturerMetaId, targetOrderInfo,
                transferredItems, transferredAmount);

        return ApiResponse.success("success");
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

        BigDecimal cancelledAmount = orderItems.stream()
                .map(this::calculateStatisticsAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String manufacturerMetaId = StringUtils.isNotBlank(orderInfo.getManufacturerId())
                ? orderInfo.getManufacturerId()
                : orderItems.stream()
                        .map(OrderItem::getManufacturerId)
                        .filter(StringUtils::isNotBlank)
                        .findFirst()
                        .orElse(null);
        adjustOrderDailyStatisticsAmount(manufacturerMetaId, orderInfo, orderItems,
                cancelledAmount == null ? BigDecimal.ZERO : cancelledAmount.negate());

        return ApiResponse.success("success");
    }

    private BigDecimal calculateTransferredAmount(OrderItem orderItem, int transferQuantity) {
        int sourceQuantity = safeQuantity(orderItem.getQuantity());
        if (sourceQuantity <= 0) {
            return BigDecimal.ZERO;
        }
        return calculateStatisticsAmount(orderItem)
                .multiply(BigDecimal.valueOf(transferQuantity))
                .divide(BigDecimal.valueOf(sourceQuantity), 8, RoundingMode.HALF_UP);
    }

    /**
     * Calculates one order item's statistics amount from its own manufacturer
     * snapshot. An incomplete floor-price manifest falls back to the item's captured
     * manufacturer payment price, then to the item's actual price.
     */
    BigDecimal calculateStatisticsAmount(OrderItem orderItem) {
        if (orderItem == null) {
            return BigDecimal.ZERO;
        }
        ManufacturerInfo manufacturerInfo = orderItem.getManufacturerInfo();
        if (manufacturerInfo != null && manufacturerInfo.getFloorPriceEffectManifest() != null) {
            List<ManufacturerInfo.FloorPriceEffectItem> items =
                    manufacturerInfo.getFloorPriceEffectManifest().getFloorPriceEffectItems();
            if (items != null && !items.isEmpty() && items.stream().allMatch(
                    item -> item != null && item.getFloorPrice() != null)) {
                return items.stream()
                        .map(ManufacturerInfo.FloorPriceEffectItem::getFloorPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }
        if (manufacturerInfo != null && manufacturerInfo.getPrice() != null
                && manufacturerInfo.getPrice().getPaymentPrice() != null) {
            return manufacturerInfo.getPrice().getPaymentPrice();
        }
        return orderItem.getPrice() == null || orderItem.getPrice().getActualPrice() == null
                ? BigDecimal.ZERO : orderItem.getPrice().getActualPrice();
    }

    private void adjustOrderDailyStatisticsAmount(String manufacturerMetaId, OrderInfo orderInfo,
                                                  List<OrderItem> orderItems, BigDecimal amount) {
        if (StringUtils.isBlank(manufacturerMetaId) || amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        incrementOrderDimensions(manufacturerMetaId, orderInfo, orderItems,
                0, BigDecimal.ZERO, amount);
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

    private OrderInfo copyOrderInfoForTransfer(OrderInfo sourceOrderInfo) {
        OrderInfo targetOrderInfo = deepCopy(sourceOrderInfo, OrderInfo.class);
        targetOrderInfo.setId(null);
        targetOrderInfo.setCreateTime(null);
        targetOrderInfo.setUpdateTime(null);
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
                                                          Integer quantity) {
        OrderTransferRecord record = new OrderTransferRecord();
        record.setOrderId(request.getOrderId());
        record.setSourceId(request.getManufacturerMetaId());
        record.setSourceName(sourceManufacturerMeta == null ? null : sourceManufacturerMeta.getName());
        record.setTargetId(targetManufacturerMeta == null ? null : targetManufacturerMeta.getManufacturerMetaId());
        record.setTargetName(targetManufacturerMeta == null ? null : targetManufacturerMeta.getName());
        record.setOrderItemId(sourceOrderItem.getOrderItemId());
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
