package com.mes.interfaces.api.platform.manufacturerSide.order;

import com.mes.application.command.order.AppOrderService;
import com.mes.application.command.order.vo.OrderQuery;
import com.mes.application.command.order.vo.OrderWithItemsVO;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.interfaces.api.dto.req.order.OrderAddRequest;
import com.mes.interfaces.api.dto.req.order.OrderListRequest;
import com.mes.interfaces.api.dto.resp.ApiResponse;
import com.mes.interfaces.api.dto.resp.PagedApiResponse;
import com.mes.interfaces.api.dto.resp.order.OrderInfoResponse;
import com.mes.interfaces.api.dto.resp.order.OrderItemResponse;
import com.mes.interfaces.api.dto.resp.order.OrderWithItemsResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manufacturerSide/order")
public class OrderController {

    @Autowired
    private AppOrderService appOrderService;

    /**
     * 分页查询订单列表（包含订单项）
     * @param request 分页请求参数
     * @param orderId 订单号（可选）
     * @param status 订单状态（可选）
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<OrderWithItemsResponse> listOrders(
            @Valid @RequestBody OrderListRequest request) {
        
        // 转换为领域层查询对象
        PagedQuery query = request.toPagedQuery();
        String orderId = request.getOrderId();
        String status = request.getStatus();
        
        // 构建查询条件
        OrderQuery orderQuery = new OrderQuery();
        orderQuery.setOrderId(orderId);
        if (status != null && !status.trim().isEmpty()) {
            orderQuery.setStatus(com.mes.domain.order.enums.OrderStatus.valueOf(status));
        }
        orderQuery.setPagedQuery(query);
        
        // 调用应用服务查询数据
        var result = appOrderService.findOrdersWithItems(orderQuery);
        
        // 转换为响应 DTO
        List<OrderWithItemsResponse> responses = result.items().stream()
                .map(OrderWithItemsResponse::from)
                .toList();
        
        // 返回分页响应
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    /**
     * 根据 ID 获取订单详情（包含订单项）
     * @param id 订单 ID
     * @return 订单详情
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderWithItemsResponse> getOrderWithItemsById(@PathVariable String id) {
        OrderWithItemsVO vo = appOrderService.getOrderWithItemsById(id);
        OrderWithItemsResponse response = OrderWithItemsResponse.from(vo);
        return ApiResponse.success(response);
    }

    /**
     * 根据订单号获取订单详情 (包含订单项)
     * @param orderId 订单号
     * @return 订单详情
     */
    @GetMapping("/byOrderId/{orderId}")
    public ApiResponse<OrderWithItemsResponse> getOrderWithItemsByOrderId(@PathVariable String orderId) {
        OrderWithItemsVO vo = appOrderService.getOrderWithItemsByOrderId(orderId);
        if (vo == null) {
            ApiResponse<OrderWithItemsResponse> response = new ApiResponse<>();
            response.setCode(ApiResponse.RepStatusCode.badParams);
            response.setMessage("订单不存在：" + orderId);
            return response;
        }
        OrderWithItemsResponse response = OrderWithItemsResponse.from(vo);
        return ApiResponse.success(response);
    }

    /**
     * 新增订单及订单项
     * @param request 新增请求参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> addOrderWithItems(@Valid @RequestBody OrderAddRequest request) {
        // 转换订单信息
        OrderInfo orderInfo = request.getOrderInfo();
        List<OrderItem> orderItems = request.getOrderItems();
        
        // 调用应用服务添加订单
        appOrderService.addOrderWithItems(orderInfo, orderItems);
        
        return ApiResponse.success("success");
    }


}
