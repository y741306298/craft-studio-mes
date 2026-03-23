package com.mes.application.command.order;

import com.mes.application.command.order.vo.OrderQuery;
import com.mes.application.command.order.vo.OrderWithItemsVO;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppOrderService {

    @Autowired
    private OrderInfoService domainOrderInfoService;

    @Autowired
    private OrderItemService domainOrderItemService;

    /**
     * 根据多条件分页查询订单列表，同时查询关联的订单项
     * @param query 查询参数
     * @return 分页结果，包含订单和对应的订单项
     */
    public PagedResult<OrderWithItemsVO> findOrdersWithItems(OrderQuery query) {
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
        String status = query.getStatus() != null ? query.getStatus().getCode() : null;
        var startTime = query.getStartTime();
        var endTime = query.getEndTime();
        var pagedQuery = query.getPagedQuery();

        List<OrderInfo> orders = domainOrderInfoService.findOrdersByConditions(
            orderId, 
            status, 
            startTime, 
            endTime, 
            (int) pagedQuery.getCurrent(), 
            pagedQuery.getSize()
        );

        long total = domainOrderInfoService.countByConditions(orderId, status, startTime, endTime);

        List<OrderWithItemsVO> items = new ArrayList<>();
        for (OrderInfo order : orders) {
            OrderWithItemsVO vo = new OrderWithItemsVO();
            vo.setOrderInfo(order);

            List<OrderItem> orderItems = domainOrderItemService.findByOrderId(
                order.getOrderId(), 
                1, 
                100
            );
            vo.setOrderItems(orderItems != null ? orderItems : new ArrayList<>());

            items.add(vo);
        }

        return new PagedResult<>(items, total, pagedQuery.getSize(), pagedQuery.getCurrent());
    }

    /**
     * 根据订单 ID 获取订单详情（包含订单项）
     * @param id 订单 ID
     * @return 订单及订单项信息
     */
    public OrderWithItemsVO getOrderWithItemsById(String id) {
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
            1, 
            100
        );
        vo.setOrderItems(orderItems != null ? orderItems : new ArrayList<>());

        return vo;
    }

    /**
     * 根据订单号获取订单详情（包含订单项）
     * @param orderId 订单号
     * @return 订单及订单项信息
     */
    public OrderWithItemsVO getOrderWithItemsByOrderId(String orderId) {
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
            1, 
            100
        );
        vo.setOrderItems(orderItems != null ? orderItems : new ArrayList<>());

        return vo;
    }

    /**
     * 添加订单及订单项
     * @param orderInfo 订单信息
     * @param orderItems 订单项列表
     * @return 添加后的订单信息
     */
    public OrderInfo addOrderWithItems(OrderInfo orderInfo, List<OrderItem> orderItems) {
        if (orderInfo == null) {
            throw new IllegalArgumentException("订单信息不能为空");
        }
        if (StringUtils.isBlank(orderInfo.getOrderId())) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("订单项不能为空");
        }

        return domainOrderInfoService.addOrderWithItems(orderInfo, orderItems);
    }
}
