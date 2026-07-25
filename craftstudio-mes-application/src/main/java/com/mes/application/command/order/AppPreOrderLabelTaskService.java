package com.mes.application.command.order;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.delivery.AppDeliveryPkgService;
import com.mes.domain.gatherplatform.wdt.entity.WdtConfig;
import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;
import com.mes.domain.gatherplatform.wdt.service.WdtService;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.order.enums.OrderChannelType;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.preOrderLabelTask.entity.PreOrderLabelTask;
import com.mes.domain.order.preOrderLabelTask.service.PreOrderLabelTaskService;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsLabel;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private WdtService wdtService;

    @Autowired
    private ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    /**
     * 预下快递单批处理开关，默认关闭，避免未显式配置时执行批处理。
     */
    @Value("${mes.pre-order-label.batch.enabled:false}")
    private boolean batchEnabled;

    @Scheduled(fixedDelay = PRE_ORDER_LABEL_TASK_FIXED_DELAY_MS)
    public void processPendingPreOrderLabelTasks() {
        if (!batchEnabled) {
            return;
        }
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

            AppDeliveryPkgService.DeliveryPkgPrintResult printResult = isGatherPlatform(orderInfo)
                    ? preOrderWdtLabel(orderInfo, orderItems)
                    : appDeliveryPkgService.preOrderKuaidi100Label(orderInfo, orderItems);
            String kuaidiNum = printResult == null ? null : printResult.getKuaidiNum();
            syncPreOrderLabelResult(task, orderInfo, productionPieces, kuaidiNum);
            if (StringUtils.isBlank(kuaidiNum)) {
                String failureReason = isGatherPlatform(orderInfo)
                        ? "聚单平台打印未返回物流单号"
                        : "快递100打印未返回快递单号";
                log.warn("预下快递单批处理任务未生成快递单号，任务按已处理结束: taskId={}, orderId={}, reason={}",
                        task.getId(), task.getOrderId(), failureReason);
                preOrderLabelTaskService.markProcessed(task, null, failureReason);
                return;
            }
            String mqFailureReason = notifyLogisticsOrderInfo(orderInfo, kuaidiNum);
            preOrderLabelTaskService.markProcessed(task, kuaidiNum, mqFailureReason);
        } catch (Exception ex) {
            log.warn("预下快递单批处理任务处理失败，等待下次重试: taskId={}, orderId={}", task.getId(), task.getOrderId(), ex);
        }
    }

    private boolean isGatherPlatform(OrderInfo orderInfo) {
        return orderInfo.getChannel() != null && orderInfo.getChannel().getType() == OrderChannelType.GATHER_PLATFORM;
    }

    /**
     * 为旺店通聚单平台订单执行预下单面单打印。
     *
     * @param orderInfo 订单信息
     * @param orderItems 订单项列表
     * @return 打印结果；缺少必要数据或未配置快递映射时返回 {@code null}
     */
    private AppDeliveryPkgService.DeliveryPkgPrintResult preOrderWdtLabel(OrderInfo orderInfo,
                                                                           List<OrderItem> orderItems) {
        if (orderInfo == null || orderInfo.getChannel() == null || orderInfo.getLogisticsCarrierInfo() == null
                || StringUtils.isBlank(orderInfo.getManufacturerId())
                || StringUtils.isBlank(orderInfo.getChannel().getOrderId())) {
            return null;
        }
        String presetType = orderInfo.getLogisticsCarrierInfo().getPresetType();
        WdtConfig config = wdtService.findConfig(orderInfo.getManufacturerId(), presetType);
        if (config == null) {
            log.info("旺店通预下单跳过，未找到快递配置: manufacturerMetaId={}, presetType={}",
                    orderInfo.getManufacturerId(), presetType);
            return null;
        }

        String uniCode = orderInfo.getChannel().getOrderId();
        try {
            GatherPlatform platform = GatherPlatform.getInstance(GatherPlatformType.WDT);
            platform.configLogisticsWarehouse(config.getLogisticsId(), config.getWarehouseId(), uniCode);
            LogisticsLabel label = printWdtLabel(platform, uniCode);
            if (label == null || StringUtils.isBlank(label.getLogisticsOrderId())) {
                return null;
            }
            String remark = buildPreOrderWdtRemark(orderInfo, orderItems);
            saveWdtLabelRecord(config, label, remark, orderInfo);
            return new AppDeliveryPkgService.DeliveryPkgPrintResult(null, label.getLogisticsOrderId());
        } catch (Exception ex) {
            throw new IllegalStateException("旺店通快递换仓或面单打印失败: " + uniCode, ex);
        }
    }

    /**
     * 使用默认打印机打印旺店通面单。
     */
    private LogisticsLabel printWdtLabel(GatherPlatform platform, String uniCode) throws Exception {
        return platform.printLogisticsLabel(uniCode, "default");
    }

    /**
     * 保存快递配置、完整面单数据及打印备注快照。
     */
    private void saveWdtLabelRecord(WdtConfig config, LogisticsLabel label, String remark, OrderInfo orderInfo) {
        WdtLabelRecord record = new WdtLabelRecord();
        record.setManufacturerMetaId(config.getManufacturerMetaId());
        record.setOrderId(orderInfo.getOrderId());
        record.setChannelOrderId(orderInfo.getChannel().getOrderId());
        record.setConsumeStatus(com.mes.domain.delivery.deliveryPkg.enums.PreOrderLabelConsumeStatus.PRE_ORDERED);
        record.setWarehouseId(config.getWarehouseId());
        record.setLogisticsId(config.getLogisticsId());
        record.setLogisticsName(config.getLogisticsName());
        record.setPresetType(config.getPresetType());
        record.setLogisticsOrderId(label.getLogisticsOrderId());
        record.setConsignee(label.getConsignee());
        record.setLogisticsCloudPrintData(label.getLogisticsCloudPrintData());
        record.setRemark(remark);
        wdtService.saveLabelRecord(record);
    }

    /**
     * 按快递100预下单规则生成订单、文件和订单备注信息。
     */
    private String buildPreOrderWdtRemark(OrderInfo orderInfo, List<OrderItem> orderItems) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(orderInfo.getOrderId())) parts.add("订单:" + orderInfo.getOrderId());
        if (orderItems != null) {
            List<String> files = orderItems.stream().filter(Objects::nonNull).map(OrderItem::getProductionImgFile)
                    .map(this::getImageFileName).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
            if (!files.isEmpty()) parts.add("文件:" + String.join(",", files));
        }
        if (StringUtils.isNotBlank(orderInfo.getRemark())) parts.add(orderInfo.getRemark());
        return String.join("\n", parts);
    }

    /**
     * 从生产图片文件对象读取文件名。
     */
    private String getImageFileName(Object imageFile) {
        if (imageFile == null) return null;
        try {
            Object value = imageFile.getClass().getMethod("getName").invoke(imageFile);
            return value == null ? null : String.valueOf(value);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * 通知聚单平台物流单号。通知失败时返回原因，由任务记录持久化，避免重新下单打印。
     *
     * @return 通知失败原因；通知成功时返回 {@code null}
     */
    private String notifyLogisticsOrderInfo(OrderInfo orderInfo, String kuaidiNum) {
        if (StringUtils.isBlank(kuaidiNum)) {
            return "MQ通知失败：kuaidiNum为空";
        }
        String logisticsOrderId = resolveLogisticsOrderId(orderInfo);
        if (StringUtils.isBlank(logisticsOrderId)) {
            return "MQ通知失败：订单ID为空";
        }
        if (StringUtils.isBlank(orderInfo.getPlatformCode())) {
            return "MQ通知失败：platformCode为空";
        }
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            return "MQ通知失败：RocketMQTemplate未配置";
        }

        LogisticsOrderInfo logisticsOrderInfo = new LogisticsOrderInfo();
        logisticsOrderInfo.setKuaidiNum(kuaidiNum);
        logisticsOrderInfo.setManufacturerMetaId(orderInfo.getManufacturerId());
        logisticsOrderInfo.setOrderId(logisticsOrderId);

        Map<String, Object> message = buildBaseMessage(LOGISTICS_MQ_TOPIC, orderInfo.getPlatformCode(), logisticsOrderInfo);
        try {
            rocketMQTemplate.syncSend(LOGISTICS_MQ_TOPIC + ":" + orderInfo.getPlatformCode(), JSON.toJSONString(message));
            log.info("预下快递单批处理任务已发送物流订单信息 MQ 通知: topic={}, tag={}, orderId={}, kuaidiNum={}, manufacturerMetaId={}",
                    LOGISTICS_MQ_TOPIC, orderInfo.getPlatformCode(), logisticsOrderInfo.getOrderId(),
                    logisticsOrderInfo.getKuaidiNum(), logisticsOrderInfo.getManufacturerMetaId());
            return null;
        } catch (Exception ex) {
            String failureReason = "MQ通知失败：" + resolveExceptionMessage(ex);
            log.error("预下快递单批处理任务 MQ 通知失败，记录错误并结束任务: orderId={}, kuaidiNum={}",
                    orderInfo.getOrderId(), kuaidiNum, ex);
            return failureReason;
        }
    }

    /**
     * 聚单平台订单使用渠道订单 ID，其他订单使用 MES 订单 ID。
     */
    private String resolveLogisticsOrderId(OrderInfo orderInfo) {
        if (orderInfo == null) {
            return null;
        }
        if (orderInfo.getChannel() != null
                && orderInfo.getChannel().getType() == OrderChannelType.GATHER_PLATFORM) {
            return orderInfo.getChannel().getOrderId();
        }
        return orderInfo.getOrderId();
    }

    /**
     * 获取异常链最底层的可读错误信息。
     */
    private String resolveExceptionMessage(Exception ex) {
        if (ex == null) {
            return "未知异常";
        }
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return StringUtils.isBlank(current.getMessage())
                ? current.getClass().getSimpleName()
                : current.getMessage();
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
            productionPieceService.updateProductionPiece(productionPiece);
        }
    }

    @lombok.Data
    private static class LogisticsOrderInfo {
        private String kuaidiNum;
        private String manufacturerMetaId;
        private String orderId;
    }

}
