package com.mes.application.command.order;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.delivery.AppDeliveryPkgService;
import com.mes.application.command.delivery.GatherPlatformResolver;
import com.mes.application.command.delivery.JstPrintGuard;
import com.mes.application.command.delivery.JstPrintUnavailableException;
import com.mes.domain.gatherplatform.wdt.entity.WdtConfig;
import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;
import com.mes.domain.gatherplatform.wdt.repository.WdtLabelRecordRepository;
import com.mes.domain.order.preOrderLabelTask.enums.PreOrderLabelTaskStatus;
import com.mes.domain.gatherplatform.wdt.service.WdtService;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.order.enums.OrderChannelType;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.preOrderLabelTask.entity.PreOrderLabelTask;
import com.mes.domain.order.preOrderLabelTask.service.PreOrderLabelTaskService;
import com.mes.infra.mq.LogisticsOrderInfo;
import com.mes.infra.mq.LogisticsOrderProducer;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.SmsCodeRequiredAuthException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsLabel;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import com.piliofpala.craftstudio.shared.domain.logistics.vo.LogisticsCarrierPresetType;
import com.piliofpala.craftstudio.shared.infra.mq.message.Message;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppPreOrderLabelTaskService {
    private static final int ORDER_QUERY_PAGE_SIZE = 100;
    @Autowired
    LogisticsOrderProducer producer;

    private static final long PRE_ORDER_LABEL_TASK_FIXED_RATE_MS = 10 * 60 * 1000L;
    private static final int GATHER_PRINT_MAX_ATTEMPTS = 3;
    private static final long GATHER_PRINT_RETRY_TTL_HOURS = 1L;
    // Keep the historical Redis namespace so in-flight WDT retry counts survive upgrades.
    private static final String GATHER_PRINT_RETRY_KEY_PREFIX = "preOrderLabelTask:wdtPrintRetry:";
    private static final String LOGISTICS_MQ_TOPIC = "mes-logistics";
    private static final int MQ_LOGISTICS_ORDER_ID_MAX_LENGTH = 64;
    private static final String SAME_WAREHOUSE_FAILURE = "换仓失败订单仓库和执行仓库相同，不执行换仓";
    private static final String TEST_KUAIDI100_NUM = "TEST_KUAIDI100_NUM";
    private static final String TASK_LOCK_PREFIX = "preOrderLabelTask:processing:";
    private static final long TASK_LOCK_TTL_HOURS = 2L;
    private static final DefaultRedisScript<Long> RELEASE_TASK_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end", Long.class);

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
    private JstPrintGuard jstPrintGuard;

    @Autowired
    private WdtLabelRecordRepository wdtLabelRecordRepository;

    @Autowired
    private ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${kuaidi100}")
    private String kuaidi100Environment;

    /**
     * 预下快递单批处理开关，默认关闭，避免未显式配置时执行批处理。
     */
    private boolean batchEnabled = true;

    @Scheduled(fixedRate = PRE_ORDER_LABEL_TASK_FIXED_RATE_MS)
    public void processPendingPreOrderLabelTasks() {
        if (!batchEnabled) {
            return;
        }
        // 先分页读取待处理快照，再执行，避免锁住的 JST 占满首页而饿死后续 WDT；
        // 也避免边处理边翻页时状态变化导致 offset 跳过任务。
        List<PreOrderLabelTask> tasks = new ArrayList<>();
        for (long page = 1; ; page++) {
            List<PreOrderLabelTask> batch = preOrderLabelTaskService.findPendingTasks(page, ORDER_QUERY_PAGE_SIZE);
            if (batch == null || batch.isEmpty()) break;
            tasks.addAll(batch);
            if (batch.size() < ORDER_QUERY_PAGE_SIZE) break;
        }
        for (PreOrderLabelTask task : tasks) {
            processTaskWithLock(task);
        }
    }

    /** 客服无须指定订单；从待预打印任务中选择一个尚无面单的 JST 订单。 */
    public WdtLabelRecord retryJstPrintForSmsAuth() {
        if (isKuaidi100TestEnvironment()) {
            throw new IllegalArgumentException("测试环境禁止调用真实聚水潭打印");
        }
        for (long page = 1; ; page++) {
            List<PreOrderLabelTask> tasks = preOrderLabelTaskService.findPendingTasks(page, ORDER_QUERY_PAGE_SIZE);
            if (tasks == null || tasks.isEmpty()) break;
            for (PreOrderLabelTask candidate : tasks) {
                if (candidate == null || StringUtils.isBlank(candidate.getId())) continue;
                String key = TASK_LOCK_PREFIX + candidate.getId();
                String token = UUID.randomUUID().toString();
                if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                        key, token, TASK_LOCK_TTL_HOURS, TimeUnit.HOURS))) continue;
                try {
                    // 取得与定时任务相同的任务锁后重新读取，防止选中刚完成或正在打印的任务。
                    PreOrderLabelTask task = preOrderLabelTaskService.findById(candidate.getId());
                    if (task == null || task.getStatus() != PreOrderLabelTaskStatus.PENDING
                            || StringUtils.isBlank(task.getOrderId()) || StringUtils.isNotBlank(task.getKuaidiNum())) continue;
                    OrderInfo order = orderInfoService.findByOrderId(task.getOrderId());
                    if (!isJstOrder(order) || OrderStatus.RETURNED.equals(order.getStatus())
                            || StringUtils.isNotBlank(order.getKuaidiNum())
                            || StringUtils.isBlank(order.getChannel().getOrderId())
                            || StringUtils.isBlank(order.getManufacturerId()) || order.getLogisticsCarrierInfo() == null) continue;
                    if (wdtLabelRecordRepository.findForPlatform("JST", order.getOrderId(),
                            order.getChannel().getOrderId(), null) != null) continue;
                    String presetType = order.getLogisticsCarrierInfo().getPresetType();
                    if (StringUtils.isBlank(presetType)
                            || wdtService.findConfig("JST", order.getManufacturerId(), presetType) == null) continue;
                    // 只尝试一个订单；上游验证码异常原样交由接口处理，不轮询发送多次短信。
                    return appDeliveryPkgService.retryJstPrintForSmsAuth(order.getOrderId());
                } finally {
                    redisTemplate.execute(RELEASE_TASK_LOCK_SCRIPT, Collections.singletonList(key), token);
                }
            }
            if (tasks.size() < ORDER_QUERY_PAGE_SIZE) break;
        }
        throw new IllegalArgumentException("没有可用于身份校验的待预打印聚水潭订单");
    }

    private boolean isJstOrder(OrderInfo order) {
        return order != null && isGatherPlatform(order)
                && "JST".equalsIgnoreCase(order.getChannel().getCode() == null
                        ? "" : order.getChannel().getCode().trim());
    }

    /**
     * 重新发送指定创建时间范围内、已经预打单成功的订单物流信息。
     *
     * <p>是否需要通知以 {@link OrderInfo#getKuaidiNum()} 为准：预打单成功后运单号会先
     * 持久化到订单，再发送 MQ。因此该接口不会再次请求打单服务，也不会产生新面单。</p>
     */
    public LogisticsMqRetryResult retryLogisticsMqNotifications(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为空");
        }
        if (startTime.after(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }

        LogisticsMqRetryResult result = new LogisticsMqRetryResult();
        int current = 1;
        while (true) {
            List<OrderInfo> orders = orderInfoService.findOrdersByConditions(
                    null, null, startTime, endTime, null, current, ORDER_QUERY_PAGE_SIZE);
            if (orders == null || orders.isEmpty()) {
                break;
            }
            result.scannedCount += orders.size();
            for (OrderInfo orderInfo : orders) {
                if (!requiresLogisticsMqNotification(orderInfo)) {
                    result.skippedCount++;
                    continue;
                }
                String failureReason = notifyLogisticsOrderInfo(orderInfo, orderInfo.getKuaidiNum());
                if (StringUtils.isBlank(failureReason)) {
                    result.successCount++;
                } else {
                    result.failureReasons.put(orderInfo.getOrderId(), failureReason);
                }
            }
            if (orders.size() < ORDER_QUERY_PAGE_SIZE) {
                break;
            }
            current++;
        }
        return result;
    }

    private boolean requiresLogisticsMqNotification(OrderInfo orderInfo) {
        return orderInfo != null
                && !OrderStatus.RETURNED.equals(orderInfo.getStatus())
                && StringUtils.isNotBlank(orderInfo.getKuaidiNum());
    }

    public static class LogisticsMqRetryResult {
        private int scannedCount;
        private int skippedCount;
        private int successCount;
        private final Map<String, String> failureReasons = new LinkedHashMap<>();

        public int getScannedCount() {
            return scannedCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureReasons.size();
        }

        public Map<String, String> getFailureReasons() {
            return failureReasons;
        }
    }

    /** Prevents overlapping scheduler runs or multiple application instances from printing one task twice. */
    private void processTaskWithLock(PreOrderLabelTask task) {
        if (task == null || StringUtils.isBlank(task.getId())) {
            processTask(task);
            return;
        }
        String lockKey = TASK_LOCK_PREFIX + task.getId();
        String token = UUID.randomUUID().toString();
        Boolean acquired;
        try {
            acquired = redisTemplate.opsForValue().setIfAbsent(
                    lockKey, token, TASK_LOCK_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception exception) {
            log.error("预下快递单任务加锁失败，为避免重复打单跳过本轮: taskId={}, orderId={}",
                    task.getId(), task.getOrderId(), exception);
            return;
        }
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("预下快递单任务正在其他实例处理中，跳过本轮: taskId={}, orderId={}",
                    task.getId(), task.getOrderId());
            return;
        }
        try {
            processTask(task);
        } finally {
            try {
                redisTemplate.execute(RELEASE_TASK_LOCK_SCRIPT,
                        Collections.singletonList(lockKey), token);
            } catch (Exception exception) {
                log.warn("预下快递单任务锁释放失败，将等待锁自动过期: taskId={}, orderId={}",
                        task.getId(), task.getOrderId(), exception);
            }
        }
    }

    private void processTask(PreOrderLabelTask task) {
        if (task == null) {
            return;
        }
        if (StringUtils.isBlank(task.getOrderId())) {
            markTaskFailed(task, null, "订单ID为空");
            return;
        }
        String kuaidiNum = null;
        OrderInfo orderInfo = null;
        try {
            orderInfo = orderInfoService.findByOrderId(task.getOrderId());
            if (orderInfo == null) {
                markTaskFailed(task, null, "订单不存在");
                return;
            }
            if (OrderStatus.RETURNED.equals(orderInfo.getStatus())) {
                preOrderLabelTaskService.delete(task);
                log.info("预下快递单批处理任务已删除，订单已退单: taskId={}, orderId={}", task.getId(), task.getOrderId());
                return;
            }

            if (isJstOrder(orderInfo) && !isKuaidi100TestEnvironment()) {
                // 尽早跳过锁定订单，避免查询工件、调用上游或消耗失败次数。
                jstPrintGuard.assertPrintingAllowed(GatherPlatformType.JST);
            }
            List<OrderItem> orderItems = orderItemService.findByOrderId(task.getOrderId(), null, 1, 100);
            List<ProductionPiece> productionPieces = findProductionPieces(orderItems);
            if (productionPieces.isEmpty()) {
                log.info("预下快递单批处理任务跳过，生产工件尚未生成: taskId={}, orderId={}", task.getId(), task.getOrderId());
                return;
            }

            AppDeliveryPkgService.DeliveryPkgPrintResult printResult;
            if (isGatherPlatform(orderInfo)) {
                // 聚单平台也沿用 kuaidi100 环境开关；测试环境只推进本地流程，禁止真实换仓、打单。
                printResult = isKuaidi100TestEnvironment()
                        ? new AppDeliveryPkgService.DeliveryPkgPrintResult(null, TEST_KUAIDI100_NUM)
                        : preOrderGatherPlatformLabel(orderInfo, orderItems);
            } else {
                try {
                    printResult = appDeliveryPkgService.preOrderKuaidi100Label(orderInfo, orderItems);
                } catch (Exception printException) {
                    throw new Kuaidi100LabelPrintException(printException);
                }
            }
            if (isGatherPlatform(orderInfo)) {
                clearGatherPrintRetryCount(task);
            }
            kuaidiNum = printResult == null ? null : printResult.getKuaidiNum();
            syncPreOrderLabelResult(task, orderInfo, productionPieces, kuaidiNum);
            if (StringUtils.isBlank(kuaidiNum)) {
                String failureReason = isGatherPlatform(orderInfo)
                        ? "聚单平台打印未返回物流单号"
                        : "快递100打印未返回快递单号";
                log.warn("预下快递单批处理任务未生成快递单号，任务已标记失败: taskId={}, orderId={}, reason={}",
                        task.getId(), task.getOrderId(), failureReason);
                completePrintFailure(task, orderInfo, failureReason);
                return;
            }
            String mqFailureReason = notifyLogisticsOrderInfo(orderInfo, kuaidiNum);
            if (StringUtils.isBlank(mqFailureReason)) {
                preOrderLabelTaskService.markProcessed(task, kuaidiNum);
            } else {
                preOrderLabelTaskService.markFailed(task, kuaidiNum, mqFailureReason);
            }
        } catch (Exception ex) {
            if (JstPrintGuard.requiresSmsAuth(ex) || containsExceptionType(ex, JstPrintUnavailableException.class)) {
                // 保持 PENDING，不消耗打印失败重试次数，不把身份异常写成订单运单号。
                log.warn("聚水潭预打印暂停，等待客服校验身份或当前操作完成: taskId={}, orderId={}",
                        task.getId(), task.getOrderId());
                return;
            }
            if (containsExceptionType(ex, GatherLabelPrintException.class)) {
                handleGatherPrintFailure(task, orderInfo, resolveExceptionMessage(ex));
                return;
            }
            if (containsExceptionType(ex, Kuaidi100LabelPrintException.class)) {
                completePrintFailure(task, orderInfo, resolveExceptionMessage(ex));
                return;
            }
            markTaskFailed(task, kuaidiNum, resolveExceptionMessage(ex));
            log.warn("预下快递单批处理任务处理失败，已标记失败: taskId={}, orderId={}", task.getId(), task.getOrderId(), ex);
        }
    }

    private void markTaskFailed(PreOrderLabelTask task, String kuaidiNum, String failureReason) {
        try {
            preOrderLabelTaskService.markFailed(task, kuaidiNum, failureReason);
        } catch (Exception markException) {
            log.error("预下快递单批处理任务标记失败状态异常: taskId={}, orderId={}",
                    task.getId(), task.getOrderId(), markException);
        }
    }

    /**
     * 记录 JST/WDT 普通打印失败。失败次数达到三次前保持 PENDING；
     * 第三次失败时发送失败原因 MQ，并将任务更新为失败。
     */
    private void handleGatherPrintFailure(PreOrderLabelTask task, OrderInfo orderInfo, String failureReason) {
        String retryKey = buildGatherPrintRetryKey(task);
        Long attempt;
        try {
            attempt = redisTemplate.opsForValue().increment(retryKey);
            redisTemplate.expire(retryKey, GATHER_PRINT_RETRY_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception redisException) {
            log.error("Redis记录聚单平台面单打印重试次数失败，任务保持待处理: taskId={}, orderId={}",
                    task.getId(), task.getOrderId(), redisException);
            return;
        }

        long currentAttempt = attempt == null ? 1L : attempt;
        if (currentAttempt < GATHER_PRINT_MAX_ATTEMPTS) {
            log.warn("聚单平台面单打印失败，等待下一次定时批次重试: taskId={}, orderId={}, attempt={}/{}, reason={}",
                    task.getId(), task.getOrderId(), currentAttempt, GATHER_PRINT_MAX_ATTEMPTS, failureReason);
            return;
        }

        completePrintFailure(task, orderInfo, failureReason);
    }

    /**
     * 打印最终失败时，将失败原因同时作为订单物流单号通知下游，并保留在失败任务中。
     */
    private void completePrintFailure(PreOrderLabelTask task, OrderInfo orderInfo, String failureReason) {
        if (orderInfo != null) {
            try {
                orderInfo.setKuaidiNum(failureReason);
                orderInfoService.updateOrder(orderInfo);
            } catch (Exception updateException) {
                log.error("面单打印失败后更新订单失败原因异常: taskId={}, orderId={}",
                        task.getId(), task.getOrderId(), updateException);
            }
        }

        String mqFailureReason = notifyLogisticsOrderInfo(orderInfo, failureReason);
        if (StringUtils.isNotBlank(mqFailureReason)) {
            log.error("面单打印失败后发送失败原因MQ通知失败: orderId={}, reason={}, mqReason={}",
                    task.getOrderId(), failureReason, mqFailureReason);
        }
        markTaskFailed(task, null, failureReason);
    }

    private String buildGatherPrintRetryKey(PreOrderLabelTask task) {
        String taskIdentifier = StringUtils.isNotBlank(task.getId()) ? task.getId() : task.getOrderId();
        return GATHER_PRINT_RETRY_KEY_PREFIX + taskIdentifier;
    }

    private void clearGatherPrintRetryCount(PreOrderLabelTask task) {
        try {
            redisTemplate.delete(buildGatherPrintRetryKey(task));
        } catch (Exception redisException) {
            log.warn("清理聚单平台面单打印重试次数失败: taskId={}, orderId={}",
                    task.getId(), task.getOrderId(), redisException);
        }
    }

    private boolean isGatherPlatform(OrderInfo orderInfo) {
        return orderInfo.getChannel() != null && orderInfo.getChannel().getType() == OrderChannelType.GATHER_PLATFORM;
    }

    private boolean isKuaidi100TestEnvironment() {
        return "test".equalsIgnoreCase(kuaidi100Environment);
    }

    /**
     * 为 WDT/JST 聚单平台订单执行预下单面单打印。
     *
     * @param orderInfo 订单信息
     * @param orderItems 订单项列表
     * @return 打印结果；缺少必要数据或未配置快递映射时返回 {@code null}
     */
    private AppDeliveryPkgService.DeliveryPkgPrintResult preOrderGatherPlatformLabel(OrderInfo orderInfo,
                                                                           List<OrderItem> orderItems) {
        if (orderInfo == null || orderInfo.getChannel() == null || orderInfo.getLogisticsCarrierInfo() == null
                || StringUtils.isBlank(orderInfo.getManufacturerId())
                || StringUtils.isBlank(orderInfo.getChannel().getOrderId())) {
            return null;
        }
        String presetType = orderInfo.getLogisticsCarrierInfo().getPresetType();
        GatherPlatformType platformType = GatherPlatformResolver.resolve(orderInfo.getChannel());
        WdtConfig config = wdtService.findConfig(platformType.name(), orderInfo.getManufacturerId(), presetType);
        if (config == null) {
            log.info("聚单平台预下单跳过，未找到快递配置: manufacturerMetaId={}, presetType={}",
                    orderInfo.getManufacturerId(), presetType);
            return null;
        }

        String uniCode = orderInfo.getChannel().getOrderId();
        try {
            return jstPrintGuard.execute(platformType, () -> {
                if (platformType == GatherPlatformType.JST) {
                    WdtLabelRecord existing = wdtLabelRecordRepository.findForPlatform("JST", orderInfo.getOrderId(),
                            orderInfo.getChannel().getOrderId(), null);
                    if (existing != null && StringUtils.isNotBlank(existing.getLogisticsOrderId())) {
                        // 客服恢复操作可能已成功生成面单，继续原预打印结果同步，不再请求上游打印。
                        return new AppDeliveryPkgService.DeliveryPkgPrintResult(null, existing.getLogisticsOrderId());
                    }
                }
                GatherPlatform platform = GatherPlatform.getInstance(platformType);
                try {
                    platform.configLogisticsWarehouse(config.getLogisticsId(), config.getWarehouseId(), uniCode);
                } catch (Exception warehouseException) {
                    log.error("聚单平台换仓异常: orderId={}", uniCode, warehouseException);
                    if (!containsExceptionMessage(warehouseException, SAME_WAREHOUSE_FAILURE)) {
                        throw warehouseException;
                    }
                    log.info("聚单平台订单仓库与执行仓库相同，跳过换仓并直接打印: orderId={}", uniCode);
                }
                LogisticsLabel label;
                try {
                    label = printGatherPlatformLabel(platform, uniCode, presetType);
                    if (label == null || StringUtils.isBlank(label.getLogisticsOrderId())) {
                        throw new IllegalStateException("聚单平台打印未返回物流单号");
                    }
                } catch (SmsCodeRequiredAuthException authException) {
                    log.error("聚单平台面单打印异常: orderId={}", uniCode, authException);
                    throw authException;
                } catch (Exception printException) {
                    log.error("聚单平台面单打印异常: orderId={}", uniCode, printException);
                    throw new GatherLabelPrintException(printException);
                }
                String remark = buildPreOrderGatherRemark(orderInfo, orderItems);
                saveGatherLabelRecord(config, label, remark, orderInfo);
                return new AppDeliveryPkgService.DeliveryPkgPrintResult(null, label.getLogisticsOrderId());
            });
        } catch (Exception ex) {
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    /**
     * 使用默认打印机打印聚单平台面单。
     */
    private LogisticsLabel printGatherPlatformLabel(GatherPlatform platform, String uniCode,String presetType) throws Exception {
        return platform.printLogisticsLabel(uniCode, "default",true, LogisticsCarrierPresetType.valueOf(presetType));
    }

    /**
     * 保存快递配置、完整面单数据及打印备注快照。
     */
    private void saveGatherLabelRecord(WdtConfig config, LogisticsLabel label, String remark, OrderInfo orderInfo) {
        WdtLabelRecord record = new WdtLabelRecord();
        record.setManufacturerMetaId(config.getManufacturerMetaId());
        record.setPlatformType(GatherPlatformResolver.resolve(orderInfo.getChannel()).name());
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
    private String buildPreOrderGatherRemark(OrderInfo orderInfo, List<OrderItem> orderItems) {
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
     * 通知聚单平台物流单号；聚单平台打印重试耗尽时，同一消息字段承载打印失败原因。
     * 通知失败时返回原因，由任务记录持久化，避免重新下单打印。
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

//        Map<String, Object> message = buildBaseMessage(LOGISTICS_MQ_TOPIC, orderInfo.getPlatformCode(), logisticsOrderInfo);
        try {
            LogisticsOrderInfo logisticsOrderInfo = new LogisticsOrderInfo();
            logisticsOrderInfo.setOrderId(Long.valueOf(orderInfo.getOrderId()));
            logisticsOrderInfo.setLogisticsOrderId(truncateLogisticsOrderId(kuaidiNum));
            producer.send(new Message<>(
                    LOGISTICS_MQ_TOPIC,orderInfo.getPlatformCode(), logisticsOrderInfo
            ));
            log.info("预下快递单批处理任务已发送物流订单信息 MQ 通知: tag={}, orderId={}, LogisticsOrderId={}, manufacturerMetaId={}",
                    LOGISTICS_MQ_TOPIC+orderInfo.getPlatformCode(), logisticsOrderInfo.getOrderId(),
                    logisticsOrderInfo.getLogisticsOrderId(), orderInfo.getManufacturerId());
            return null;
        } catch (Exception ex) {
            String failureReason = "MQ通知失败：" + resolveExceptionMessage(ex);
            log.error("预下快递单批处理任务 MQ 通知失败，记录错误并结束任务: orderId={}, kuaidiNum={}",
                    orderInfo.getOrderId(), kuaidiNum, ex);
            return failureReason;
        }
    }

    private static String truncateLogisticsOrderId(String value) {
        if (value == null || value.codePointCount(0, value.length()) <= MQ_LOGISTICS_ORDER_ID_MAX_LENGTH) {
            return value;
        }
        int endIndex = value.offsetByCodePoints(0, MQ_LOGISTICS_ORDER_ID_MAX_LENGTH);
        return value.substring(0, endIndex);
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

    private boolean containsExceptionMessage(Throwable throwable, String expectedMessage) {
        Throwable current = throwable;
        while (current != null) {
            if (StringUtils.isNotBlank(current.getMessage()) && current.getMessage().contains(expectedMessage)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsExceptionType(Throwable throwable, Class<? extends Throwable> expectedType) {
        Throwable current = throwable;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Map<String, Object> buildBaseMessage(String topic, String tag, Object info) {
        Map<String, Object> message = new LinkedHashMap<>();
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
        // productionPieces 在调用外部打单服务之前读取，打单可能耗时较长。这里不能 save 整个旧对象，
        // 否则会覆盖期间由 toLayout 原子更新的待排版/排版中数量。
        productionPieceService.updateChannelByIds(
                productionPieces.stream().map(ProductionPiece::getId)
                        .filter(StringUtils::isNotBlank).toList(),
                orderInfo.getChannel() != null ? orderInfo.getChannel() : task.getChannel());
    }

    private static class GatherLabelPrintException extends RuntimeException {
        private GatherLabelPrintException(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }

    private static class Kuaidi100LabelPrintException extends RuntimeException {
        private Kuaidi100LabelPrintException(Throwable cause) {
            super(cause);
        }
    }



}
