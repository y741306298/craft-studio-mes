package com.mes.interfaces.api.platform.manufacturerSide.order;

import com.mes.domain.shared.utils.JsonLogUtil;
import com.alibaba.fastjson.JSON;
import com.mes.application.command.api.resp.GrayImgToSvgResponse;
import com.mes.application.command.api.resp.ImageMaskResponse;
import com.mes.application.command.order.AppOrderService;
import com.mes.application.command.order.vo.OrderItemVO;
import com.mes.application.command.order.vo.OrderPackagingSyncResult;
import com.mes.application.command.order.vo.OrderQuery;
import com.mes.application.command.order.vo.OrderWithItemsVO;

import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.command.orderPreprocessing.OrderPreprocessTaskQueue;
import com.mes.application.dto.req.order.CancelOrderRequest;
import com.mes.application.dto.req.order.OrderAddRequest;
import com.mes.application.dto.req.order.OrderListRequest;
import com.mes.application.dto.req.order.OrderItemsByOrderIdRequest;
import com.mes.application.dto.req.order.OrderTransferRequest;
import com.mes.application.dto.req.order.OrderTransferRecordListRequest;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.order.OrderItemResponse;
import com.mes.application.dto.resp.order.OrderAddResponse;
import com.mes.application.dto.resp.order.OrderWithItemsResponse;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderTransferRecord.entity.OrderTransferRecord;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Slf4j
@RestController
@RequestMapping("/api/manufacturerSide/order")
public class OrderController {
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private AppOrderService appOrderService;

    @Autowired
    private AppOrderPreprocessingService appOrderPreprocessingService;

    @Autowired
    private OrderPreprocessTaskQueue orderPreprocessTaskQueue;

    Logger logger = Logger.getLogger(OrderController.class.getName());

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
        String customerName = request.getCustomerName();
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
        orderQuery.setCustomerName(customerName);
        orderQuery.setCustomerPhone(customerPhone);
        orderQuery.setRouteId(request.getRouteId());
        orderQuery.setOrgName(request.getOrgName());
        
        // 处理日期字符串转换为 Date 对象
        if (createDateStart != null && !createDateStart.trim().isEmpty()) {
            try {
                LocalDate startDate = LocalDate.parse(createDateStart);
                LocalDateTime startDateTime = startDate.atStartOfDay();
                orderQuery.setStartTime(java.util.Date.from(startDateTime.atZone(BEIJING_ZONE).toInstant()));
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("开始日期格式错误，应为 yyyy-MM-dd");
            }
        }
        if (createDateEnd != null && !createDateEnd.trim().isEmpty()) {
            try {
                LocalDate endDate = LocalDate.parse(createDateEnd);
                LocalDateTime endDateTime = endDate.atTime(LocalTime.of(23, 59, 59));
                orderQuery.setEndTime(java.util.Date.from(endDateTime.atZone(BEIJING_ZONE).toInstant()));
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("结束日期格式错误，应为 yyyy-MM-dd");
            }
        }
        
        orderQuery.setPagedQuery(query);
        
        // 调用应用服务查询数据
        PagedResult<OrderItemVO> ordersWithItems = appOrderService.findOrdersWithItems(orderQuery);
        OrderDailyStatistics statistics = appOrderService.findOrderDailyStatistics(
                request.getManufacturerId(),
                LocalDate.now(BEIJING_ZONE));
        List<OrderItemVO> orderItems = (List<OrderItemVO>) ordersWithItems.items();
        // 返回分页响应及可选统计数据
        return PagedApiResponse.success(
                orderItems,
                query.getCurrent(),
                query.getSize(),
                ordersWithItems.total(),
                statistics == null ? 0L : statistics.getTotalOrderCount(),
                statistics == null ? BigDecimal.ZERO : statistics.getTotalArea(),
                statistics == null ? BigDecimal.ZERO : statistics.getTotalAmount());
    }


    /**
     * 根据订单 ID 全量查询 quantity 不为 0 的订单项列表，返回结构与 listOrders 的 item 一致。
     */
    @PostMapping("/listByOrderId")
    public ApiResponse<List<OrderItemVO>> listOrderItemsByOrderId(@RequestBody OrderItemsByOrderIdRequest request) {
        OrderQuery orderQuery = new OrderQuery();
        orderQuery.setOrderId(request.getOrderId());
        orderQuery.setManufacturerId(request.getManufacturerId());
        return ApiResponse.success(appOrderService.findNonZeroQuantityOrderItemsByOrderId(orderQuery));
    }


    /**
     * 同步生产中订单项的已打包状态。
     * 当订单项关联生产工件的“已打包”节点数量大于等于订单项数量时，将订单项状态更新为已打包。
     *
     * @return 同步结果
     */
    @PostMapping("/item/syncPackagedStatus")
    public ApiResponse<OrderPackagingSyncResult> syncPackagedOrderItems() {
        return ApiResponse.success(appOrderService.syncPackagedOrderItems());
    }

    /**
     * 根据订单项 ID 获取详情（包含生产工件）
     * @param orderItemId 订单项 ID
     * @return 订单项详情及生产工件
     */
    @GetMapping("/item/{orderItemId}")
    public ApiResponse<OrderItemResponse> getOrderItemWithProductionPieces(@PathVariable String orderItemId) {
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
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<?> addOrderWithItems(@RequestBody String requestBody) {
        logger.info("========== addOrderWithItems 入参开始 ==========");
        logger.info("request: " + requestBody);
        logger.info("========== addOrderWithItems 入参结束 ==========");

        if (JSON.isValidArray(requestBody)) {
            List<OrderAddRequest> requests = JSON.parseArray(requestBody, OrderAddRequest.class);
            List<OrderAddResponse> responses = appOrderService.addOrdersWithItems(requests).stream()
                    .map(OrderAddResponse::from)
                    .collect(Collectors.toList());
            return ApiResponse.success(responses);
        } else {
            OrderAddRequest request = JSON.parseObject(requestBody, OrderAddRequest.class);
            OrderAddResponse response = OrderAddResponse.from(appOrderService.addOrderWithItems(request));
            return ApiResponse.success(response);
        }
    }



    /**
     * 重新处理订单项。
     * 根据 orderItemId 删除该订单项已生成的生产工件并重新提交预处理；
     * 根据 orderId 删除该订单下所有订单项已生成的生产工件并重新提交预处理。
     *
     * @param orderItemId 订单项 ID
     * @param orderId 订单 ID
     * @return 操作结果
     */
    @PostMapping("/item/reprocess")
    public ApiResponse<String> reprocessOrderItem(@RequestParam(required = false) String orderItemId,
                                                   @RequestParam(required = false) String orderId) {
        long deletedCount = appOrderService.reprocessOrderItem(orderItemId, orderId);
        return ApiResponse.success("重新处理任务已提交，已删除生产工件数量：" + deletedCount);
    }


    /**
     * 订单转单
     * @param request 转单请求参数
     * @return 操作结果
     */
    @PostMapping("/transfer")
    public ApiResponse<String> transferOrder(@Valid @RequestBody OrderTransferRequest request) {
        return appOrderService.transferOrder(request);
    }


    /**
     * 分页查询转入记录。
     * request.manufacturerMetaId 对应转单记录 targetId。
     * @param request 分页查询参数
     * @return 转入记录分页结果
     */
    @PostMapping("/transfer/in/list")
    public PagedApiResponse<OrderTransferRecord> listTransferInRecords(
            @Valid @RequestBody OrderTransferRecordListRequest request) {
        PagedQuery query = request.toPagedQuery();
        PagedResult<OrderTransferRecord> result = appOrderService.findTransferInRecords(
                request.getManufacturerMetaId(),
                (int) query.getCurrent(),
                query.getSize()
        );
        return PagedApiResponse.success((List<OrderTransferRecord>) result.items(), query.getCurrent(), query.getSize(), result.total());
    }

    /**
     * 分页查询转出记录。
     * request.manufacturerMetaId 对应转单记录 sourceId。
     * @param request 分页查询参数
     * @return 转出记录分页结果
     */
    @PostMapping("/transfer/out/list")
    public PagedApiResponse<OrderTransferRecord> listTransferOutRecords(
            @Valid @RequestBody OrderTransferRecordListRequest request) {
        PagedQuery query = request.toPagedQuery();
        PagedResult<OrderTransferRecord> result = appOrderService.findTransferOutRecords(
                request.getManufacturerMetaId(),
                (int) query.getCurrent(),
                query.getSize()
        );
        return PagedApiResponse.success((List<OrderTransferRecord>) result.items(), query.getCurrent(), query.getSize(), result.total());
    }


    /**
     * 修复历史转单记录 targetId。
     * 将历史记录中保存的 manufacturerUser.account 转换为 manufacturerMetaId。
     * @return 修复结果
     */
    @PostMapping("/transfer/record/target-id/repair")
    public ApiResponse<String> repairTransferRecordTargetIds() {
        return appOrderService.repairTransferRecordTargetIds();
    }

    /**
     * 取消订单
     * @param request 取消订单请求参数
     * @return 操作结果
     */
    @PostMapping("/cancel")
    public ApiResponse<String> cancelOrder(@Valid @RequestBody CancelOrderRequest request) {
        return appOrderService.cancelOrder(request.getPlatformCode(), request.getOrderId());
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

    /**
     * 图像蒙版生成回调接口
     * 供算法服务异步调用，接收图像处理结果并生成生产零件
     * 
     * @param response 算法服务返回的蒙版结果，其中 id 字段为回调 ID（兼容 orderItemId 或 orderItemId#preprocessRequestId）
     * @return 操作结果
     */
    @PostMapping("/callback/generate_mask_files")
    public ApiResponse<String> handleGenerateMaskFilesCallback(@RequestBody ImageMaskResponse response) {
        logger.info("========== handleGenerateMaskFilesCallback 入参开始 ==========");
        logger.info("response: " + JsonLogUtil.toJSONString(response));
        logger.info("========== handleGenerateMaskFilesCallback 入参结束 ==========");
        try {
            // 从 response 中获取回调 ID，新版格式为 orderItemId#preprocessRequestId。
            String callbackId = response.getId();
            if ((callbackId == null || callbackId.isEmpty()) && response.getOrderItemId() != null) {
                callbackId = response.getOrderItemId();
            }
            
            if (callbackId == null || callbackId.isEmpty()) {
                return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "回调ID不能为空");
            }

            // 调用服务层处理回调，由服务层判断是否为当前有效预处理请求。
            appOrderPreprocessingService.handleGenerateMaskFilesCallback(response, callbackId);
            
            return ApiResponse.success("回调处理成功");
            
        } catch (Exception e) {
            System.err.println("处理图像蒙版回调失败：" + e.getMessage());
            return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, "回调处理失败：" + e.getMessage());
        }
    }

    /**
     * 灰度图转 SVG 回调接口。
     *
     * @param response 算法服务返回的 SVG 对象名与订单项 ID
     * @return 操作结果
     */
    @PostMapping("/callback/convert_gray_img_to_svg")
    public ApiResponse<String> handleConvertGrayImgToSvgCallback(@RequestBody GrayImgToSvgResponse response) {
        logger.info("========== handleConvertGrayImgToSvgCallback 入参开始 ==========");
        logger.info("response: " + JsonLogUtil.toJSONString(response));
        logger.info("========== handleConvertGrayImgToSvgCallback 入参结束 ==========");
        try {
            OrderItem orderItem = appOrderPreprocessingService.handleConvertGrayImgToSvgCallback(response);
            orderPreprocessTaskQueue.submit(List.of(orderItem));
            return ApiResponse.success("回调处理成功");
        } catch (Exception e) {
            System.err.println("处理灰度图转SVG回调失败：" + e.getMessage());
            return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, "回调处理失败：" + e.getMessage());
        }
    }

}
