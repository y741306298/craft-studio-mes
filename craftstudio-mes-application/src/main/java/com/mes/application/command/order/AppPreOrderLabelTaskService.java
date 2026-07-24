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
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AppPreOrderLabelTaskService {
    private static final long PRE_ORDER_LABEL_TASK_FIXED_DELAY_MS = 10 * 60 * 1000L;
    private static final String LOGISTICS_MQ_TOPIC = "mes-logistics";

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

    @Autowired
    private ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

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
            notifyLogisticsOrderInfo(orderInfo, kuaidiNum);
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

    private void notifyLogisticsOrderInfo(OrderInfo orderInfo, String kuaidiNum) {
        if (orderInfo == null || orderInfo.getChannel() == null || StringUtils.isBlank(orderInfo.getChannel().getOrderId())) {
            log.info("预下快递单批处理任务跳过物流订单信息 MQ 通知，渠道订单 ID 为空: orderId={}",
                    orderInfo == null ? null : orderInfo.getOrderId());
            return;
        }
        if (StringUtils.isBlank(orderInfo.getPlatformCode())) {
            log.info("预下快递单批处理任务跳过物流订单信息 MQ 通知，platformCode 为空: orderId={}", orderInfo.getOrderId());
            return;
        }
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            log.warn("预下快递单批处理任务无法发送物流订单信息 MQ 通知，RocketMQTemplate 未配置: orderId={}", orderInfo.getOrderId());
            return;
        }

        LogisticsOrderInfo logisticsOrderInfo = new LogisticsOrderInfo();
        logisticsOrderInfo.setKuaidiNum(kuaidiNum);
        logisticsOrderInfo.setManufacturerMetaId(orderInfo.getManufacturerId());
        logisticsOrderInfo.setOrderId(orderInfo.getChannel().getOrderId());

        Map<String, Object> message = buildBaseMessage(LOGISTICS_MQ_TOPIC, orderInfo.getPlatformCode(), logisticsOrderInfo);
        rocketMQTemplate.syncSend(LOGISTICS_MQ_TOPIC + ":" + orderInfo.getPlatformCode(), message);
        log.info("预下快递单批处理任务已发送物流订单信息 MQ 通知: topic={}, tag={}, orderId={}, kuaidiNum={}, manufacturerMetaId={}",
                LOGISTICS_MQ_TOPIC, orderInfo.getPlatformCode(), logisticsOrderInfo.getOrderId(),
                logisticsOrderInfo.getKuaidiNum(), logisticsOrderInfo.getManufacturerMetaId());
    }

    private Map<String, Object> buildBaseMessage(String topic, String tag, Object info) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("topic", topic);
        message.put("tag", tag);
        message.put("info", info);
        return message;
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

    @lombok.Data
    private static class LogisticsOrderInfo {
        private String kuaidiNum;
        private String manufacturerMetaId;
        private String orderId;
    }

}
