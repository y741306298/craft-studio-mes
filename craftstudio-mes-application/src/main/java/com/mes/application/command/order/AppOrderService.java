package com.mes.application.command.order;

import com.mes.application.command.order.vo.OrderItemVO;
import com.mes.application.command.order.vo.OrderQuery;
import com.mes.application.command.order.vo.OrderWithItemsVO;
import com.mes.application.command.orderPreprocessing.OrderPreprocessTaskQueue;
import com.mes.application.dto.req.order.OrderAddRequest;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AppOrderService {

    @Autowired
    private OrderInfoService domainOrderInfoService;

    @Autowired
    private OrderItemService domainOrderItemService;

    @Autowired
    private ProductionPieceService productionPieceService;

    @Autowired
    private OrderPreprocessTaskQueue orderPreprocessTaskQueue;

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
        var startTime = query.getStartTime();
        var endTime = query.getEndTime();
        var pagedQuery = query.getPagedQuery();

        long total;

        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerId", manufacturerId);
        if (StringUtils.isNotBlank(orderId)) {
            filters.put("orderId", orderId);
        }
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (startTime != null) {
            filters.put("createTime_gte", startTime);
        }
        if (endTime != null) {
            filters.put("createTime_lte", endTime);
        }
        List<OrderItem> orderItems = domainOrderItemService.filterList(
                (int) pagedQuery.getCurrent(),
                (int) pagedQuery.getSize(),
                filters
        );
        total = domainOrderItemService.filterTotal(filters);
        List<OrderItemVO> result = new ArrayList<OrderItemVO>();
        for (OrderItem item : orderItems) {
            String oid = item.getOrderId();
            OrderInfo orderInfo = domainOrderInfoService.findByOrderId(oid);
            OrderItemVO orderWithItemsVO = new OrderItemVO();
            BeanUtils.copyProperties(item, orderWithItemsVO);
            orderWithItemsVO.setCustomer(orderInfo.getCustomer());
            result.add(orderWithItemsVO);
        }
        return new PagedResult<>(result, total, pagedQuery.getSize(), pagedQuery.getCurrent());
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
        // 入库成功后立即返回，预处理改为异步队列执行
        orderPreprocessTaskQueue.submit(orderItemsResult);
        return orderInfo;
    }

    /**
     * 取消订单。
     * 根据订单号和制造商 ID 查找订单及订单项，若生产工件在“待排版”之后的任意节点已有数量，则不允许取消。
     *
     * @param manufacturerMetaId 制造商 ID
     * @param orderId 订单号
     * @return 操作结果
     */
    @Transactional
    public ApiResponse<String> cancelOrder(String manufacturerMetaId, String orderId) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId 不能为空");
        }
        if (StringUtils.isBlank(orderId)) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "订单号不能为空");
        }

        OrderInfo orderInfo = domainOrderInfoService.findByOrderId(orderId);
        if (orderInfo == null) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "订单不存在：" + orderId);
        }

        List<OrderItem> orderItems = domainOrderItemService.findByOrderId(orderId, manufacturerMetaId, 1, 100);
        if (orderItems == null || orderItems.isEmpty()) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "未找到对应制造商的订单项");
        }

        Map<String, List<ProductionPiece>> piecesByOrderItemId = new HashMap<>();
        for (OrderItem orderItem : orderItems) {
            List<ProductionPiece> productionPieces = productionPieceService.findProductionPiecesByConditions(
                    manufacturerMetaId,
                    null,
                    null,
                    null,
                    orderItem.getOrderItemId(),
                    null,
                    null,
                    1,
                    Integer.MAX_VALUE
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
            domainOrderItemService.updateOrderItem(orderItem);
            for (ProductionPiece productionPiece : piecesByOrderItemId.getOrDefault(orderItem.getOrderItemId(), new ArrayList<>())) {
                productionPieceService.deleteProductionPiece(productionPiece.getId());
            }
        }

        return ApiResponse.success("success");
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
     * 切换订单项加急状态
     *
     * @param orderItemId 订单项 ID
     */
    public void toggleOrderItemUrgent(String orderItemId) {
        if (StringUtils.isBlank(orderItemId)) {
            throw new IllegalArgumentException("订单项 ID 不能为空");
        }

        OrderItem orderItem = domainOrderItemService.findById(orderItemId);
        if (orderItem == null) {
            throw new IllegalArgumentException("订单项不存在：" + orderItemId);
        }

        // 切换当前状态（true -> false, false -> true）
        Boolean currentStatus = orderItem.getIsUrgent() != null ? orderItem.getIsUrgent() : false;
        orderItem.setIsUrgent(!currentStatus);
        domainOrderItemService.updateOrderItem(orderItem);
    }
}
