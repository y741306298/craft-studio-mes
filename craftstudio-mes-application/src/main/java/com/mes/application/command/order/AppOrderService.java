package com.mes.application.command.order;

import com.mes.application.command.order.vo.OrderItemVO;
import com.mes.application.command.order.vo.OrderQuery;
import com.mes.application.command.order.vo.OrderWithItemsVO;
import com.mes.application.command.orderPreprocessing.OrderPreprocessTaskQueue;
import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.dto.req.order.OrderAddRequest;
import com.mes.application.dto.req.order.OrderTransferRequest;
import com.mes.domain.auth.entity.ManufacturerUser;
import com.mes.domain.auth.repository.ManufacturerUserRepository;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerMetaRepository;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.orderTransferRecord.entity.OrderTransferRecord;
import com.mes.domain.order.orderTransferRecord.service.OrderTransferRecordService;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.alibaba.fastjson.JSON;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AppOrderService {

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
        List<OrderItem> orderItems = domainOrderItemService.filterListUrgentFirst(
                (int) pagedQuery.getCurrent(),
                (int) pagedQuery.getSize(),
                filters
        );
        total = domainOrderItemService.filterTotal(filters);
        List<OrderItemVO> result = buildOrderItemVOs(orderItems);
        return new PagedResult<>(result, total, pagedQuery.getSize(), pagedQuery.getCurrent());
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
        for (OrderItem item : orderItems) {
            if (item == null) {
                continue;
            }
            String oid = item.getOrderId();
            OrderInfo orderInfo = domainOrderInfoService.findByOrderId(oid);
            OrderItemVO orderWithItemsVO = new OrderItemVO();
            BeanUtils.copyProperties(item, orderWithItemsVO);
            if (orderInfo != null) {
                orderWithItemsVO.setCustomer(orderInfo.getCustomer());
                orderWithItemsVO.setRemark(orderInfo.getRemark());
                orderWithItemsVO.setOrgInfo(orderInfo.getOrgInfo());
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
        List<OrderItem> orderItems = request.toOrderItems();
        //先入库
        List<OrderItem> orderItemsResult = domainOrderInfoService.addOrderWithItems(orderInfo, orderItems);
        // 灰度图转 SVG 必须先同步完成，之后才能进入其他异步预处理。
        List<OrderItem> readyToPreprocessOrderItems = appOrderPreprocessingService.convertMaskGrayImgToSvgIfNecessary(orderItemsResult);
        // 入库和必要的灰度图转 SVG 成功后立即返回，后续预处理改为异步队列执行
        orderPreprocessTaskQueue.submit(readyToPreprocessOrderItems);
        return orderInfo;
    }

    /**
     * 批量添加订单及订单项。
     *
     * @return 添加后的订单信息列表
     */
    public List<OrderInfo> addOrdersWithItems(List<OrderAddRequest> requests) {
        List<OrderInfo> orderInfos = new ArrayList<>();
        if (requests == null) {
            return orderInfos;
        }
        for (OrderAddRequest request : requests) {
            orderInfos.add(addOrderWithItems(request));
        }
        return orderInfos;
    }

    /**
     * 重新处理订单项：删除该订单项已生成的所有生产工件，并重新提交预处理任务。
     *
     * @param orderItemId 订单项业务 ID
     * @return 被删除的生产工件数量
     */
    public long reprocessOrderItem(String orderItemId) {
        if (StringUtils.isBlank(orderItemId)) {
            throw new IllegalArgumentException("订单项 ID 不能为空");
        }

        OrderItem orderItem = domainOrderItemService.findByOrderItemId(orderItemId);
        if (orderItem == null) {
            throw new IllegalArgumentException("订单项不存在：" + orderItemId);
        }

        // 先更新预处理请求 ID，使重做前已发出的异步算法回调在返回时被识别为过期并丢弃。
        orderItem.setPreprocessRequestId(IdGenerator.generateId("OPR"));
        domainOrderItemService.updateOrderItem(orderItem);

        long deletedCount = productionPieceService.deleteProductionPiecesByOrderItemId(orderItemId);
        orderPreprocessTaskQueue.submit(List.of(orderItem));
        return deletedCount;
    }


    /**
     * 订单转单。
     * 将指定订单项的待生产数量转入目标工厂，并保留每个原订单项维度的转出记录。
     * request.targetId 使用目标工厂 manufacturerUser.account，再由账号关联 manufacturerMetaId。
     */
    public ApiResponse<String> transferOrder(OrderTransferRequest request) {
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

        OrderInfo sourceOrderInfo = domainOrderInfoService.findByOrderId(request.getOrderId());
        if (sourceOrderInfo == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "订单不存在：" + request.getOrderId());
        }

        Map<String, OrderItem> orderItemById = new HashMap<>();
        Map<String, List<ProductionPiece>> productionPiecesByOrderItemId = new HashMap<>();
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
        }

        OrderInfo targetOrderInfo = copyOrderInfoForTransfer(sourceOrderInfo);
        domainOrderInfoService.addOrder(targetOrderInfo);

        List<OrderTransferRecord> transferRecords = new ArrayList<>();
        for (OrderTransferRequest.OrderTransferItemDto itemDto : request.getOrderItemDtos()) {
            OrderItem sourceOrderItem = orderItemById.get(itemDto.getOrderItemId());
            Integer transferQuantity = itemDto.getQuantity();
            String newOrderItemId = IdGenerator.generateOrderItemId();

            OrderItem targetOrderItem = copyOrderItemForTransfer(sourceOrderItem, newOrderItemId, targetManufacturerMetaId, transferQuantity);
            domainOrderItemService.addOrderItem(targetOrderItem);

            for (ProductionPiece sourceProductionPiece : productionPiecesByOrderItemId.getOrDefault(sourceOrderItem.getOrderItemId(), new ArrayList<>())) {
                ProductionPiece targetProductionPiece = copyProductionPieceForTransfer(
                        sourceProductionPiece,
                        newOrderItemId,
                        targetManufacturerMetaId,
                        transferQuantity
                );
                productionPieceService.addProductionPiece(targetProductionPiece);
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

        return ApiResponse.success("success");
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
