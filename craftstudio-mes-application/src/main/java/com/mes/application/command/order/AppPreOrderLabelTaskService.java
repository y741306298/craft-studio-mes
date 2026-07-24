package com.mes.application.command.order;

import com.mes.application.command.delivery.AppDeliveryPkgService;
import com.mes.domain.manufacturer.productionPiece.entity.DeliveryPkgInfo;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.order.enums.OrderChannelType;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.preOrderLabelTask.entity.PreOrderLabelTask;
import com.mes.domain.order.preOrderLabelTask.service.PreOrderLabelTaskService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AppPreOrderLabelTaskService {
    private static final long PRE_ORDER_LABEL_TASK_FIXED_DELAY_MS = 10 * 60 * 1000L;

    @Autowired
    private PreOrderLabelTaskService preOrderLabelTaskService;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ProductionPieceService productionPieceService;

    @Autowired
    private AppDeliveryPkgService appDeliveryPkgService;

    @Scheduled(fixedDelay = PRE_ORDER_LABEL_TASK_FIXED_DELAY_MS)
    public void processPendingPreOrderLabelTasks() {
        List<PreOrderLabelTask> tasks = preOrderLabelTaskService.findPendingTasks();
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        for (PreOrderLabelTask task : tasks) {
            processTask(task);
        }
    }

    private void processTask(PreOrderLabelTask task) {
        if (task == null || StringUtils.isBlank(task.getOrderId())) {
            return;
        }
        try {
            List<OrderItem> orderItems = orderItemService.findByOrderId(task.getOrderId(), null, 1, 100);
            List<ProductionPiece> productionPieces = findProductionPieces(orderItems);
            if (productionPieces.isEmpty()) {
                log.info("预下快递单批处理任务跳过，生产工件尚未生成: taskId={}, orderId={}", task.getId(), task.getOrderId());
                return;
            }

            OrderInfo orderInfo = orderInfoService.findByOrderId(task.getOrderId());
            if (orderInfo == null) {
                log.warn("预下快递单批处理任务跳过，订单不存在: taskId={}, orderId={}", task.getId(), task.getOrderId());
                return;
            }

            AppDeliveryPkgService.DeliveryPkgPrintResult printResult = isGatherPlatform(task)
                    ? preOrderWdtLabel(orderInfo, orderItems)
                    : appDeliveryPkgService.preOrderKuaidi100Label(orderInfo, orderItems);
            String kuaidiNum = printResult == null ? null : printResult.getKuaidiNum();
            syncPreOrderLabelResult(task, orderInfo, productionPieces, kuaidiNum);
            preOrderLabelTaskService.markProcessed(task, kuaidiNum);
        } catch (Exception ex) {
            log.warn("预下快递单批处理任务处理失败，等待下次重试: taskId={}, orderId={}", task.getId(), task.getOrderId(), ex);
        }
    }

    private boolean isGatherPlatform(PreOrderLabelTask task) {
        return task.getChannel() != null && task.getChannel().getType() == OrderChannelType.GATHER_PLATFORM;
    }

    /**
     * 旺店通预下单预留入口。
     */
    private AppDeliveryPkgService.DeliveryPkgPrintResult preOrderWdtLabel(OrderInfo orderInfo, List<OrderItem> orderItems) {
        return null;
    }

    private List<ProductionPiece> findProductionPieces(List<OrderItem> orderItems) {
        List<ProductionPiece> productionPieces = new ArrayList<>();
        if (orderItems == null || orderItems.isEmpty()) {
            return productionPieces;
        }
        for (OrderItem orderItem : orderItems) {
            if (orderItem == null || StringUtils.isBlank(orderItem.getOrderItemId())) {
                continue;
            }
            List<ProductionPiece> pieces = productionPieceService.findProductionPiecesByOrderItemId(orderItem.getOrderItemId(), 1, 100);
            if (pieces != null) {
                productionPieces.addAll(pieces);
            }
        }
        return productionPieces;
    }

    private void syncPreOrderLabelResult(PreOrderLabelTask task, OrderInfo orderInfo, List<ProductionPiece> productionPieces, String kuaidiNum) {
        if (StringUtils.isNotBlank(kuaidiNum)) {
            orderInfo.setKuaidiNum(kuaidiNum);
            orderInfoService.updateOrder(orderInfo);
        }
        for (ProductionPiece productionPiece : productionPieces) {
            productionPiece.setChannel(orderInfo.getChannel() != null ? orderInfo.getChannel() : task.getChannel());
            if (StringUtils.isNotBlank(kuaidiNum)) {
                appendDeliveryPkgInfo(productionPiece, orderInfo, kuaidiNum);
            }
            productionPieceService.updateProductionPiece(productionPiece);
        }
    }

    private void appendDeliveryPkgInfo(ProductionPiece productionPiece, OrderInfo orderInfo, String kuaidiNum) {
        List<DeliveryPkgInfo> pkgInfos = productionPiece.getDeliveryPkgInfos();
        if (pkgInfos == null) {
            pkgInfos = new ArrayList<>();
        }
        DeliveryPkgInfo deliveryPkgInfo = new DeliveryPkgInfo();
        deliveryPkgInfo.setKuaidiNum(kuaidiNum);
        if (orderInfo.getLogisticsCarrierInfo() != null) {
            deliveryPkgInfo.setCarrierId(orderInfo.getLogisticsCarrierInfo().getCarrierId());
            deliveryPkgInfo.setCarrierName(orderInfo.getLogisticsCarrierInfo().getCarrierName());
        }
        deliveryPkgInfo.setQuantity(productionPiece.getQuantity());
        pkgInfos.add(deliveryPkgInfo);
        productionPiece.setDeliveryPkgInfos(pkgInfos);
    }
}
