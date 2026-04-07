package com.mes.interfaces.api.platform.manufacturerSide.order;

import com.mes.application.command.order.AppOrderService;
import com.mes.application.command.order.vo.OrderItemVO;
import com.mes.application.command.order.vo.OrderQuery;
import com.mes.application.command.order.vo.OrderWithItemsVO;

import com.mes.application.dto.req.order.OrderAddRequest;
import com.mes.application.dto.req.order.OrderListRequest;
import com.mes.application.dto.resp.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.order.OrderItemResponse;
import com.mes.application.dto.resp.order.OrderWithItemsResponse;
import com.mes.domain.order.enums.OrderStatus;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
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
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<OrderItemVO> listOrders(@RequestBody OrderListRequest request) {
        
        // 转换为领域层查询对象
        PagedQuery query = request.toPagedQuery();
        String orderId = request.getOrderId();
        String status = request.getStatus();
        String customerPhone = request.getCustomerPhone();
        String createDateStart = request.getCreateDateStart();
        String createDateEnd = request.getCreateDateEnd();
        
        // 构建查询条件
        OrderQuery orderQuery = new OrderQuery();
        orderQuery.setOrderId(orderId);
        orderQuery.setManufacturerId(request.getManufacturerId());
        if (status != null && !status.trim().isEmpty()) {
            orderQuery.setStatus(com.mes.domain.order.enums.OrderStatus.valueOf(status));
        }
        orderQuery.setCustomerPhone(customerPhone);
        
        // 处理日期字符串转换为 Date 对象
        if (createDateStart != null && !createDateStart.trim().isEmpty()) {
            try {
                orderQuery.setStartTime(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(createDateStart));
            } catch (java.text.ParseException e) {
                throw new IllegalArgumentException("开始日期格式错误，应为 yyyy-MM-dd");
            }
        }
        if (createDateEnd != null && !createDateEnd.trim().isEmpty()) {
            try {
                orderQuery.setEndTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(createDateEnd + " 23:59:59"));
            } catch (java.text.ParseException e) {
                throw new IllegalArgumentException("结束日期格式错误，应为 yyyy-MM-dd");
            }
        }
        
        orderQuery.setPagedQuery(query);
        
        // 调用应用服务查询数据
        PagedResult<OrderItemVO> ordersWithItems = appOrderService.findOrdersWithItems(orderQuery);
        // 返回分页响应
        return PagedApiResponse.success((List<OrderItemVO>) ordersWithItems.items(), query.getCurrent(), query.getSize(), ordersWithItems.total());
    }


    /**
     * 根据订单项 ID 获取详情（包含生产工件）
     * @param orderItemId 订单项 ID
     * @return 订单项详情及生产工件
     */
    @GetMapping("/item/{orderItemId}")
    public ApiResponse<OrderItemResponse> getOrderItemWithProductionPieces(@RequestBody String orderItemId) {
        var orderItem = appOrderService.getOrderItemWithProductionPieces(orderItemId);
        OrderItemResponse response = OrderItemResponse.from(orderItem);
        return ApiResponse.success(response);
    }

    /**
     * 根据订单号获取订单详情 (包含订单项)
     * @return 订单详情
     */
    @PostMapping("/byOrderId")
    public ApiResponse<OrderWithItemsResponse> getOrderWithItemsByOrderId(@PathVariable OrderListRequest request) {
        OrderQuery orderQuery = new OrderQuery();
        orderQuery.setOrderId(request.getOrderId());
        orderQuery.setPagedQuery(request.toPagedQuery());
        OrderWithItemsVO vo = appOrderService.getOrderWithItemsByOrderId(orderQuery);
        if (vo == null) {
            ApiResponse<OrderWithItemsResponse> response = new ApiResponse<>();
            response.setCode(ApiResponse.RepStatusCode.badParams);
            response.setMessage("订单不存在：" + request.getOrderId());
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
        // 调用应用服务添加订单
        appOrderService.addOrderWithItems(request);
        
        return ApiResponse.success("success");
    }

    /**
     * 切换订单项加急状态（加急/取消加急）
     * @param id 订单项 ID
     * @return 操作结果
     */
    @GetMapping("/toggleUrgent")
    public ApiResponse<String> toggleOrderItemUrgent(@RequestParam String id) {
        appOrderService.toggleOrderItemUrgent(id);
        return ApiResponse.success("success");
    }

    /**
     * 获取所有订单状态枚举
     * @return 订单状态列表
     */
    @GetMapping("/status")
    public ApiResponse<List<OrderStatus>> getOrderStatusList() {
        return ApiResponse.success(List.of(OrderStatus.values()));
    }

}
