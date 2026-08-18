package com.mes.application.command.typesetting;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.command.api.resp.MaterialDevelopedSizeResponse;
import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.resp.NestingResponse;
import com.mes.application.command.api.resp.FormeGenerationResponse;
import com.mes.application.command.api.vo.CallbackCustomValue;
import com.mes.application.command.api.vo.UploadConfig;
import com.mes.application.command.orderPreprocessing.splice.SpliceProcessStrategies;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.mes.application.command.typesetting.layout.FormeBuildContext;
import com.mes.application.command.typesetting.layout.CaifuOpenBackA30HFilmNestingRuleService;
import com.mes.application.command.typesetting.layout.FormeLayoutBuildResult;
import com.mes.application.command.typesetting.layout.NestingRequestRuleService;
import com.mes.application.command.typesetting.nesting.NestingRequestComposeService;
import com.mes.application.command.typesetting.service.MarkedNestingElementService;
import com.mes.application.command.typesetting.layout.TypesettingLayoutModeBuildService;
import com.mes.application.command.typesetting.layout.TypesettingLayoutModeConfirmService;
import com.mes.application.command.typesetting.strategy.MirrorFormeStrategy;
import com.mes.application.command.typesetting.strategy.NestingManifestStrategy;
import com.mes.application.command.typesetting.strategy.SpecialCraftMarkStrategy;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.application.command.typesetting.enums.FormeGenerationElementType;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.ConfirmPrintResult;
import com.mes.application.command.typesetting.vo.GenerateQrCodeResult;
import com.mes.application.command.typesetting.vo.GenerateTempCodeResult;
import com.mes.application.command.typesetting.vo.LayoutConfirmResult;
import com.mes.application.command.typesetting.vo.TypesettingLayoutModeVO;
import com.mes.application.command.typesetting.vo.ReleaseLayoutResult;
import com.mes.application.command.typesetting.vo.TypesettingLayoutSpecVO;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.command.typesetting.vo.TypesettingPiecesQueryResult;
import com.mes.application.dto.req.typesetting.GenerateQrCodeRequest;
import com.mes.application.dto.req.typesetting.GenerateTempCodeRequest;
import com.mes.application.dto.TypesettingQuery;
import com.mes.application.dto.req.typesetting.ConfirmPrintRequest;
import com.mes.application.dto.req.typesetting.BatchConfirmLayoutRequest;
import com.mes.application.dto.req.typesetting.BatchConfirmPrintRequest;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.domain.shared.utils.JsonLogUtil;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.util.ProcedureFlowNodeMatcher;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingContainerWidthInset;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingPrintTaskStatus;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingSequenceUsageType;
import com.mes.domain.manufacturer.typesetting.service.TypesettingContainerWidthInsetService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingPrintTaskService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingSequencePoolService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingDownloadTaskData;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.piliofpala.craftstudio.shared.infra.cloud.platforms.alicloud.AliCloudAuthService;
import com.piliofpala.craftstudio.shared.infra.cloud.storage.dto.ObjectStorageTempAuthConfig;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.net.URI;
import java.util.Base64;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Comparator;
import java.util.function.Supplier;

@Slf4j
@Service
public class AppTypesettingService {

    private static final int SCOPED_FULL_LIST_SIZE = 999;

    private static final String LAYOUT_CONFIRM_CACHE_PREFIX = "layout:confirm:";
    private static final String TYPESETTING_OPERATION_LOCK_PREFIX = "typesetting:operation:lock:";
    private static final long TYPESETTING_OPERATION_LOCK_EXPIRE_MINUTES = 10;
    private static final long CACHE_EXPIRE_HOURS = 72;
    private static final long NESTING_CALLBACK_RECORD_WAIT_MILLIS = 10_000;
    private static final long NESTING_CALLBACK_RECORD_POLL_MILLIS = 100;
    private static final DateTimeFormatter TYPESETTING_ID_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String TEMP_CODE_QUEUE_KEY_PREFIX = "typesetting:temp-code:queue:";
    private static final String TEMP_CODE_QUEUE_INIT_KEY_PREFIX = "typesetting:temp-code:init:";
    private static final int TEMP_CODE_QUEUE_MAX = 100000;
    private static final Pattern SVG_SOURCE_INDEX_PATTERN = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern SVG_TAG_PATTERN = Pattern.compile("<svg\\b[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SVG_ROOT_SIZE_PATTERN = Pattern.compile(
            "\\b(width|height)\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SVG_VIEW_BOX_PATTERN = Pattern.compile(
            "\\bviewBox\\s*=\\s*[\"']\\s*[-+]?\\d*\\.?\\d+\\s+[-+]?\\d*\\.?\\d+\\s+([-+]?\\d*\\.?\\d+)\\s+([-+]?\\d*\\.?\\d+)\\s*[\"']",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SPLICE_MARK_GROUP_PATTERN = Pattern.compile(
            "<g\\b(?=[^>]*\\bid\\s*=\\s*([\"'])(?:super-width-splice|adhesive-splice|photo-splice|board-cover-splice|inkjet-splice|seamless-splice|panel-splice)-).*?</g\\s*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final int TAG_STRIP_HEIGHT_MM = 20;
    private static final int DEFAULT_CONTAINER_WIDTH_INSET_COVER_BOARD_PARTS_MM = 16;
    private static final int DEFAULT_CONTAINER_HEIGHT_INSET_COVER_BOARD_PARTS_MM = 40;
    private static final int DEFAULT_CONTAINER_WIDTH_INSET_STANDARD_MM = 28;
    private static final TypesettingLayoutSpecVO DEFAULT_DEVELOPED_SIZE_LAYOUT_SPEC =
            new TypesettingLayoutSpecVO("1200*50000", 1200, 50000);
    private static final List<TypesettingLayoutSpecVO> DEFAULT_LAYOUT_SPECS = List.of(
            new TypesettingLayoutSpecVO("900*2440", 900, 2440),
            new TypesettingLayoutSpecVO("1050*2440", 1050, 2440),
            new TypesettingLayoutSpecVO("1200*2400", 1200, 2400),
            new TypesettingLayoutSpecVO("920*2440", 920, 2440),
            new TypesettingLayoutSpecVO("1070*2440", 1070, 2440),
            new TypesettingLayoutSpecVO("1220*2440", 1220, 2440),
            new TypesettingLayoutSpecVO("920*2000", 920, 2000),
            new TypesettingLayoutSpecVO("1070*2000", 1070, 2000),
            new TypesettingLayoutSpecVO("920*1540", 920, 1540),
            new TypesettingLayoutSpecVO("1220*1540", 1220, 1540),
            new TypesettingLayoutSpecVO("1520*1500", 1520, 1500)
    );
    /**
     * layoutMode -> builder 的运行时映射表。
     * 在容器初始化完成后由 initLayoutModeBuilders 填充。
     */
    private final Map<TypesettingLayoutMode, TypesettingLayoutModeBuildService> layoutModeBuildServiceMap = new EnumMap<>(TypesettingLayoutMode.class);
    private final Map<TypesettingLayoutMode, TypesettingLayoutModeConfirmService> layoutModeConfirmServiceMap = new EnumMap<>(TypesettingLayoutMode.class);
    private final Map<TypesettingLayoutMode, NestingRequestRuleService> nestingRequestRuleServiceMap = new EnumMap<>(TypesettingLayoutMode.class);
    private final Map<TypesettingLayoutMode, NestingRequestComposeService> nestingRequestComposeServiceMap = new EnumMap<>(TypesettingLayoutMode.class);
    @Autowired
    private TypesettingService domainTypesettingService;
    @Autowired
    private TypesettingContainerWidthInsetService containerWidthInsetService;
    @Autowired
    private ProductionPieceService productionPieceService;
    @Autowired
    private TypesettingPrintTaskService typesettingPrintTaskService;
    @Autowired
    private TypesettingSequencePoolService typesettingSequencePoolService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private ManufacturerDeviceCfgRepository manufacturerDeviceCfgRepository;
    @Autowired
    private AlgorithmCoreApiService algorithmCoreApiService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private AliCloudAuthService aliCloudAuthService;
    @Autowired
    private NestingManifestStrategy nestingManifestStrategy;
    @Autowired(required = false)
    private List<MirrorFormeStrategy> mirrorFormeStrategies;
    @Autowired(required = false)
    private List<SpecialCraftMarkStrategy> specialCraftMarkStrategies;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ProductCoreApiService productCoreApiService;
    @Autowired
    private List<TypesettingLayoutModeBuildService> layoutModeBuildServices;
    @Autowired
    private OssTagUploadService ossTagUploadService;
    @Autowired
    private MarkedNestingElementService markedNestingElementService;
    @Autowired(required = false)
    private List<TypesettingLayoutModeConfirmService> layoutModeConfirmServices;
    @Autowired(required = false)
    private List<NestingRequestRuleService> nestingRequestRuleServices;
    @Autowired(required = false)
    private List<NestingRequestComposeService> nestingRequestComposeServices;
    @Value("${external.callbackApi.generate_nested_files:}")
    private String generateNestedFilesCallbackUrl;
    @Value("${external.callbackApi.generate_grid_nested_files:}")
    private String generateGridNestedFilesCallbackUrl;
    @Value("${external.callbackApi.generate_forme:}")
    private String generateFormeUrl;
    @Value("${ali-cloud.oss.endpoint:${spring.cloud.alicloud.oss.endpoint:}}")
    private String ossEndpoint;
    @Value("${ali-cloud.oss.raw-bucket:${spring.cloud.alicloud.oss.bucket-name:}}")
    private String ossBucket;

    private static BigDecimal ceilBigDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(0, RoundingMode.CEILING);
    }

    @PostConstruct
    public void initLayoutModeBuilders() {
        // 将所有模式构建器注册到 map，供 confirmLayout 阶段按 mode 分发调用
        if (layoutModeBuildServices == null) {
            return;
        }
        for (TypesettingLayoutModeBuildService buildService : layoutModeBuildServices) {
            layoutModeBuildServiceMap.put(buildService.supportMode(), buildService);
        }
        if (nestingRequestRuleServices != null) {
            for (NestingRequestRuleService ruleService : nestingRequestRuleServices) {
                nestingRequestRuleServiceMap.put(ruleService.supportMode(), ruleService);
                if (ruleService instanceof CaifuOpenBackA30HFilmNestingRuleService) {
                    nestingRequestRuleServiceMap.put(TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_NO_FILM, ruleService);
                }
            }
        }
        if (nestingRequestComposeServices != null) {
            for (NestingRequestComposeService composeService : nestingRequestComposeServices) {
                nestingRequestComposeServiceMap.put(composeService.supportMode(), composeService);
            }
        }
        if (layoutModeConfirmServices == null) {
            return;
        }
        for (TypesettingLayoutModeConfirmService confirmService : layoutModeConfirmServices) {
            layoutModeConfirmServiceMap.put(confirmService.supportMode(), confirmService);
        }
    }

    /**
     * 统一查询接口
     * @param query 查询参数
     * @return 分页结果
     */
    public TypesettingPiecesQueryResult findTypesettingAndProductionPieces(TypesettingQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
        if (StringUtils.isBlank(query.getManufacturerMetaId())) {
            throw new IllegalArgumentException("manufacturerMetaId 不能为空");
        }

        int current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        int size = query.getSize() == null || query.getSize() < 1 ? 50 : Math.min(query.getSize(), 100);

        List<TypesettingProductionPieceVO> allItems = new ArrayList<>();
        boolean queryPartOnly = TypesettingSourceType.PART.getCode().equals(query.getSourceType());
        boolean queryTypesettingOnly = TypesettingSourceType.TYPESETTING.getCode().equals(query.getSourceType());
        boolean queryProductionPiecesByRoute = StringUtils.isNotBlank(query.getRouteId());

        if (!queryTypesettingOnly) {
            List<ProductionPiece> productionPieces = findPendingTypesettingProductionPieces(query);
            Map<String, String> orderGroupIdCache = loadOrderGroupIds(productionPieces, query);
            for (ProductionPiece piece : productionPieces) {
                if (getPendingTypesettingQuantity(piece) > 0) {
                    TypesettingProductionPieceVO vo = TypesettingProductionPieceVO.fromProductionPiece(piece);
                    applyECommerceGroupId(vo, piece, query, orderGroupIdCache);
                    allItems.add(vo);
                }
            }
        }

        if (!queryPartOnly && !queryProductionPiecesByRoute) {
            List<TypesettingInfo> typesettingInfos = timeMongoQuery("findPendingTypesettingInfos", () ->
                    domainTypesettingService.findTypesettingByProcessingConditions(
                            query.getManufacturerMetaId(),
                            TypesettingStatus.PENDING.getCode(),
                            query.getMaterialName(),
                            query.getProcessingName(),
                            query.getStartTime(),
                            query.getEndTime(),
                            null,
                            1,
                            Integer.MAX_VALUE
                    ));
            for (TypesettingInfo info : typesettingInfos) {
                Integer leaveQuantity = info.getLeaveQuantity() == null ? 0 : info.getLeaveQuantity();
                boolean isPending = TypesettingStatus.PENDING.getCode().equals(info.getStatus());
                if (leaveQuantity > 0 && isPending) {
                    allItems.add(TypesettingProductionPieceVO.fromTypesettingInfo(info));
                }
            }
        }

        sortTypesettingProductionPiecesByUrgencyAndCreateTime(allItems);

        long total = allItems.size();
        int fromIndex = Math.min((current - 1) * size, allItems.size());
        int toIndex = Math.min(fromIndex + size, allItems.size());
        List<TypesettingProductionPieceVO> items = new ArrayList<>(allItems.subList(fromIndex, toIndex));

        return new TypesettingPiecesQueryResult(new PagedResult<>(items, total, size, current), allItems);
    }

    /**
     * 查询仍有待排版数量的零件。
     * <p>
     * 生产中的零件可能同时在多个工序节点保有数量。先通过顶层“生产中”状态缩小数据库
     * 查询范围，再由调用方根据“待排版”节点的 pieceQuantity 过滤。
     *
     * @param query 查询条件
     * @return 符合基础条件的生产零件
     */
    private List<ProductionPiece> findPendingTypesettingProductionPieces(TypesettingQuery query) {
        return timeMongoQuery("findPendingTypesettingProductionPieces", () ->
                productionPieceService.findPendingTypesettingPiecesByProcessingConditions(
                        query.getManufacturerMetaId(),
                        query.getMaterialName(),
                        query.getProcessingName(),
                        query.getOrderItemId(),
                        query.getRouteId(),
                        query.getStartTime(),
                        query.getEndTime()
                ));
    }

    /**
     * 按 typesettingId / orderId / orderItemId 之一全量查询待排版对象。
     */
    public List<TypesettingProductionPieceVO> findTypesettingAndProductionPiecesById(TypesettingQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
        if (StringUtils.isBlank(query.getManufacturerMetaId())) {
            throw new IllegalArgumentException("manufacturerMetaId 不能为空");
        }
        validateSingleTypesettingScopedId(query);

        List<TypesettingProductionPieceVO> items;
        if (StringUtils.isNotBlank(query.getTypesettingId())) {
            items = findPendingTypesettingItemsByTypesettingId(query);
        } else {
            items = findPendingProductionPieceItemsByOrderScope(query);
        }
        sortTypesettingProductionPiecesByUrgencyAndCreateTime(items);
        return items;
    }

    private void validateSingleTypesettingScopedId(TypesettingQuery query) {
        int idCount = 0;
        if (StringUtils.isNotBlank(query.getTypesettingId())) {
            idCount++;
        }
        if (StringUtils.isNotBlank(query.getOrderId())) {
            idCount++;
        }
        if (StringUtils.isNotBlank(query.getOrderItemId())) {
            idCount++;
        }
        if (idCount != 1) {
            throw new IllegalArgumentException("typesettingId、orderId、orderItemId 必须且只能传一个");
        }
    }

    private List<TypesettingProductionPieceVO> findPendingTypesettingItemsByTypesettingId(TypesettingQuery query) {
        List<TypesettingInfo> typesettingInfos = domainTypesettingService.findTypesettingByProcessingConditions(
                query.getManufacturerMetaId(),
                null,
                query.getMaterialName(),
                query.getProcessingName(),
                query.getStartTime(),
                query.getEndTime(),
                null,
                1,
                SCOPED_FULL_LIST_SIZE
        );
        List<TypesettingProductionPieceVO> items = new ArrayList<>();
        for (TypesettingInfo info : typesettingInfos) {
            if (info == null) {
                continue;
            }
            Integer leaveQuantity = info.getLeaveQuantity() == null ? 0 : info.getLeaveQuantity();
            boolean isPending = TypesettingStatus.PENDING.getCode().equals(info.getStatus());
            if (leaveQuantity > 0 && isPending && matchesTypesettingId(info.getTypesettingId(), query.getTypesettingId())) {
                items.add(TypesettingProductionPieceVO.fromTypesettingInfo(info));
            }
        }
        return items;
    }

    private List<TypesettingProductionPieceVO> findPendingProductionPieceItemsByOrderScope(TypesettingQuery query) {
        List<String> orderItemIds = StringUtils.isNotBlank(query.getOrderId())
                ? findOrderItemIdsByOrderId(query.getOrderId(), query.getManufacturerMetaId())
                : Collections.singletonList(query.getOrderItemId());

        List<ProductionPiece> matchedPieces = new ArrayList<>();
        for (String orderItemId : orderItemIds) {
            if (StringUtils.isBlank(orderItemId)) {
                continue;
            }
            List<ProductionPiece> productionPieces = productionPieceService.findProductionPiecesByProcessingConditions(
                    query.getManufacturerMetaId(),
                    null,
                    query.getMaterialName(),
                    query.getProcessingName(),
                    orderItemId,
                    query.getRouteId(),
                    query.getStartTime(),
                    query.getEndTime(),
                    1,
                    SCOPED_FULL_LIST_SIZE
            );
            for (ProductionPiece piece : productionPieces) {
                if (getPendingTypesettingQuantity(piece) > 0) {
                    matchedPieces.add(piece);
                }
            }
        }

        List<TypesettingProductionPieceVO> items = new ArrayList<>();
        Map<String, String> orderGroupIdCache = loadOrderGroupIds(matchedPieces, query);
        for (ProductionPiece piece : matchedPieces) {
            TypesettingProductionPieceVO vo = TypesettingProductionPieceVO.fromProductionPiece(piece);
            applyECommerceGroupId(vo, piece, query, orderGroupIdCache);
            items.add(vo);
        }
        return items;
    }

    private List<String> findOrderItemIdsByOrderId(String orderId, String manufacturerMetaId) {
        if (StringUtils.isBlank(orderId)) {
            return Collections.emptyList();
        }
        List<String> orderItemIds = new ArrayList<>();
        int current = 1;
        while (true) {
            List<OrderItem> orderItems = orderItemService.findByOrderId(orderId.trim(), manufacturerMetaId, current, 100);
            if (orderItems == null || orderItems.isEmpty()) {
                break;
            }
            orderItems.stream()
                    .map(OrderItem::getOrderItemId)
                    .filter(StringUtils::isNotBlank)
                    .forEach(orderItemIds::add);
            if (orderItems.size() < 100) {
                break;
            }
            current++;
        }
        return orderItemIds;
    }

    private boolean matchesTypesettingId(String candidate, String requestTypesettingId) {
        if (StringUtils.isBlank(requestTypesettingId)) {
            return true;
        }
        if (StringUtils.isBlank(candidate)) {
            return false;
        }
        return candidate.trim().equals(requestTypesettingId.trim());
    }

    private void applyECommerceGroupId(TypesettingProductionPieceVO vo, ProductionPiece piece, TypesettingQuery query, Map<String, String> orderGroupIdCache) {
        if (vo == null || piece == null || query == null || !Boolean.TRUE.equals(query.getECommerceMmodel())) {
            return;
        }
        String orderItemId = piece.getOrderItemId();
        if (StringUtils.isBlank(orderItemId)) {
            return;
        }
        String orderId = orderGroupIdCache.get(orderItemId.trim());
        if (StringUtils.isNotBlank(orderId)) {
            vo.setGroupId(orderId);
        }
    }

    /**
     * 批量加载电商分组信息，避免为列表中的每一个工件单独查询订单项。
     */
    private Map<String, String> loadOrderGroupIds(List<ProductionPiece> pieces, TypesettingQuery query) {
        if (query == null || !Boolean.TRUE.equals(query.getECommerceMmodel()) || CollectionUtils.isEmpty(pieces)) {
            return Collections.emptyMap();
        }
        Set<String> orderItemIds = pieces.stream()
                .filter(Objects::nonNull)
                .map(ProductionPiece::getOrderItemId)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
        if (orderItemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> orderIdsByItemId = new HashMap<>();
        List<OrderItem> orderItems = timeMongoQuery("findOrderItemsForECommerceGroups",
                () -> orderItemService.findByOrderItemIds(orderItemIds));
        for (OrderItem orderItem : orderItems) {
            if (orderItem == null) {
                continue;
            }
            if (StringUtils.isNotBlank(orderItem.getOrderItemId())) {
                orderIdsByItemId.put(orderItem.getOrderItemId().trim(), orderItem.getOrderId());
            }
            // 兼容历史数据中 productionPiece.orderItemId 保存 MongoDB 主键的情况。
            if (StringUtils.isNotBlank(orderItem.getId())) {
                orderIdsByItemId.put(orderItem.getId().trim(), orderItem.getOrderId());
            }
        }
        return orderIdsByItemId;
    }

    /**
     * 仅对本列表接口显式发起的 MongoDB 查询计时，避免注册影响全局的驱动监听器。
     */
    private <T> T timeMongoQuery(String queryName, Supplier<T> queryAction) {
        long start = System.nanoTime();
        try {
            return queryAction.get();
        } finally {
            log.info("listTypesettingAndProductionPieces MongoDB query completed: query={}, elapsedMs={}",
                    queryName, (System.nanoTime() - start) / 1_000_000.0);
        }
    }

    private void sortTypesettingProductionPiecesByUrgencyAndCreateTime(List<TypesettingProductionPieceVO> items) {
        items.sort(Comparator
                .comparing((TypesettingProductionPieceVO item) -> Boolean.TRUE.equals(item.getIsUrgent()))
                .reversed()
                .thenComparing(TypesettingProductionPieceVO::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())));
    }

    /**
     * 查询排版规格。
     * <p>
     * 入参仅依赖待排版 cell：同批 cell 必须使用同一种材料；存在覆板工艺时，同批 cell 必须都包含覆板工艺。
     * 覆板且全部为零件时使用默认规格，其余场景按材料展开尺寸查询规格。
     */
    public List<TypesettingLayoutSpecVO> listLayoutSpecs(LayoutConfirmRequest request) {
        List<TypesettingProductionPieceVO> typesettingCells = request == null ? null : request.getTypesettingCells();
        if (CollectionUtils.isEmpty(typesettingCells)) {
            throw new IllegalArgumentException("排版对象不能为空");
        }

        String materialId = resolveSameMaterialId(typesettingCells);
        String rmfId = resolveRequestManufacturerMetaId(request);
        boolean hasCoverBoard = typesettingCells.stream().anyMatch(this::hasCoverBoardNode);
        List<TypesettingLayoutSpecVO> layoutSpecs;
        if (hasCoverBoard) {
            for (TypesettingProductionPieceVO cell : typesettingCells) {
                if (!hasCoverBoardNode(cell)) {
                    throw new IllegalArgumentException(buildCoverBoardMissingMessage(cell));
                }
            }
            boolean allParts = typesettingCells.stream()
                    .allMatch(cell -> cell != null && TypesettingSourceType.PART.getCode().equals(cell.getSourceType()));
            layoutSpecs = allParts
                    ? DEFAULT_LAYOUT_SPECS
                    : listLayoutSpecsByMaterialId(materialId, rmfId);
        } else {
            layoutSpecs = listLayoutSpecsByMaterialId(materialId, rmfId);
        }

        // 任意一个 cell 包含“双面对裱”或“覆双面”工艺时，都需要限制规格高度并去重。
        if (hasAnyDoubleSideMountNode(typesettingCells)) {
            return limitLayoutSpecHeightAndDistinct(layoutSpecs, 2400);
        }
        return layoutSpecs;
    }

    private String resolveSameMaterialId(List<TypesettingProductionPieceVO> typesettingCells) {
        String materialId = null;
        for (TypesettingProductionPieceVO cell : typesettingCells) {
            String currentMaterialId = getLayoutSpecMaterialId(cell);
            if (StringUtils.isBlank(currentMaterialId)) {
                throw new IllegalArgumentException("材料ID不能为空");
            }
            if (materialId == null) {
                materialId = currentMaterialId;
                continue;
            }
            if (!Objects.equals(materialId, currentMaterialId)) {
                throw new IllegalArgumentException("材料不同，不能排版");
            }
        }
        return materialId;
    }

    private String getLayoutSpecMaterialId(TypesettingProductionPieceVO cell) {
        if (cell == null || cell.getMaterialConfig() == null) {
            return null;
        }
        if (isMirrorTypesettingCell(cell)) {
            String oriMaterialId = normalizeToNull(cell.getOriMaterialId());
            if (StringUtils.isBlank(oriMaterialId)) {
                oriMaterialId = normalizeToNull(getMaterialConfigField(cell.getMaterialConfig(), "oriMaterialId"));
            }
            if (StringUtils.isNotBlank(oriMaterialId)) {
                return oriMaterialId;
            }
        }
        return normalizeToNull(cell.getMaterialConfig().getMaterialId());
    }

    private boolean isMirrorTypesettingCell(TypesettingProductionPieceVO cell) {
        if (cell == null || !TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
            return false;
        }
        return isMirrorTypesettingId(cell.getGroupId())
                || isMirrorTypesettingId(cell.getId())
                || isMirrorTypesettingId(cell.getSourceId());
    }

    private String normalizeToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Object getMaterialConfigField(MaterialConfig materialConfig, String fieldName) {
        if (materialConfig == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            return materialConfig.getClass().getMethod(getterName).invoke(materialConfig);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveRequestManufacturerMetaId(LayoutConfirmRequest request) {
        if (request == null || StringUtils.isBlank(request.getManufacturerMetaId())) {
            throw new IllegalArgumentException("工厂ID不能为空");
        }
        return request.getManufacturerMetaId().trim();
    }

    private boolean hasCoverBoardNode(TypesettingProductionPieceVO cell) {
        return hasProcedureNode(cell, "覆板");
    }

    private boolean hasAnyDoubleSideMountNode(List<TypesettingProductionPieceVO> typesettingCells) {
        return typesettingCells.stream()
                .anyMatch(this::hasDoubleSideMountNode);
    }

    private boolean hasDoubleSideMountNode(TypesettingProductionPieceVO cell) {
        return cell != null
                && ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(cell.getProcedureFlow());
    }

    private boolean hasProcedureNode(TypesettingProductionPieceVO cell, String... nodeNames) {
        return cell != null && hasProcedureNode(cell.getProcedureFlow(), nodeNames);
    }

    private boolean hasProcedureNode(ProcedureFlow procedureFlow, String... nodeNames) {
        if (procedureFlow == null || CollectionUtils.isEmpty(procedureFlow.getNodes())) {
            return false;
        }
        Set<String> targetNodeNames = Arrays.stream(nodeNames).collect(Collectors.toSet());
        return procedureFlow.getNodes().stream()
                .anyMatch(node -> node != null && targetNodeNames.contains(node.getNodeName()));
    }

    private List<TypesettingLayoutSpecVO> limitLayoutSpecHeightAndDistinct(List<TypesettingLayoutSpecVO> layoutSpecs, int maxHeight) {
        if (CollectionUtils.isEmpty(layoutSpecs)) {
            return Collections.emptyList();
        }
        Map<String, TypesettingLayoutSpecVO> layoutSpecMap = new LinkedHashMap<>();
        for (TypesettingLayoutSpecVO layoutSpec : layoutSpecs) {
            if (layoutSpec == null || layoutSpec.getWidth() == null || layoutSpec.getHeight() == null) {
                continue;
            }
            Integer width = layoutSpec.getWidth();
            Integer height = layoutSpec.getHeight() > maxHeight ? maxHeight : layoutSpec.getHeight();
            String name = width + "*" + height;
            layoutSpecMap.putIfAbsent(name, new TypesettingLayoutSpecVO(name, width, height));
        }
        return new ArrayList<>(layoutSpecMap.values());
    }

    private String buildCoverBoardMissingMessage(TypesettingProductionPieceVO cell) {
        String sourceName = resolveCellDisplayName(cell);
        String sourceTypeName = TypesettingSourceType.TYPESETTING.getCode().equals(cell == null ? null : cell.getSourceType())
                ? "印版"
                : "零件";
        return sourceName + sourceTypeName + "不包含覆板工艺，不能排版";
    }

    private String resolveCellDisplayName(TypesettingProductionPieceVO cell) {
        if (cell == null) {
            return "";
        }
        if (StringUtils.isNotBlank(cell.getSourceId())) {
            return cell.getSourceId();
        }
        if (StringUtils.isNotBlank(cell.getId())) {
            return cell.getId();
        }
        if (StringUtils.isNotBlank(cell.getGroupId())) {
            return cell.getGroupId();
        }
        return "";
    }

    private List<TypesettingLayoutSpecVO> listLayoutSpecsByMaterialId(String materialId, String rmfId) {
        Map<String, MaterialDevelopedSizeResponse> developedSizeMap = findDevelopedSizeMapByMaterialIdOrEmpty(materialId, rmfId);
        if (CollectionUtils.isEmpty(developedSizeMap)) {
            return List.of(DEFAULT_DEVELOPED_SIZE_LAYOUT_SPEC);
        }

        Map<String, TypesettingLayoutSpecVO> layoutSpecMap = new LinkedHashMap<>();
        for (MaterialDevelopedSizeResponse developedSize : developedSizeMap.values()) {
            if (developedSize == null || developedSize.getWidth() == null || developedSize.getHeight() == null) {
                continue;
            }
            Integer width = toLayoutSpecSize(developedSize.getWidth());
            Integer height = toLayoutSpecSize(developedSize.getHeight());
            String name = width + "*" + height;
            layoutSpecMap.putIfAbsent(name, new TypesettingLayoutSpecVO(name, width, height));
        }

        if (layoutSpecMap.isEmpty()) {
            return List.of(DEFAULT_DEVELOPED_SIZE_LAYOUT_SPEC);
        }
        return new ArrayList<>(layoutSpecMap.values());
    }

    private Map<String, MaterialDevelopedSizeResponse> findDevelopedSizeMapByMaterialIdOrEmpty(String materialId, String rmfId) {
        try {
            return productCoreApiService.findDevelopedSizeMapByMaterialId(materialId, rmfId);
        } catch (RuntimeException e) {
            if (isDevelopedSizeNotFoundException(e)) {
                log.warn("材料展开尺寸不存在，使用默认排版规格，materialId={}, rmfId={}", materialId, rmfId, e);
                return Collections.emptyMap();
            }
            throw e;
        }
    }

    private boolean isDevelopedSizeNotFoundException(RuntimeException e) {
        String message = e.getMessage();
        return message != null && message.contains("获取材料展开尺寸失败") && message.contains("不存在");
    }

    /**
     * 查询默认排版规格
     */
    public List<TypesettingLayoutSpecVO> listDefaultLayoutSpecs() {
        return DEFAULT_LAYOUT_SPECS;
    }

    private Integer toLayoutSpecSize(Double size) {
        return Math.toIntExact(Math.round(size));
    }

    /**
     * 查询所有排版方式（完整对象）
     */
    public List<TypesettingLayoutModeVO> listLayoutModes() {
        return Arrays.stream(TypesettingLayoutMode.values())
                .filter(TypesettingLayoutMode::isQueryable)
                .map(TypesettingLayoutModeVO::from)
                .collect(Collectors.toList());
    }

    /**
     * 查询状态为待确认（confirming）的排版信息列表（分页）
     * @param manufacturerMetaId 厂商元数据ID
     * @param current 当前页码
     * @param size 每页大小
     * @return 分页结果
     */
    public PagedResult<TypesettingInfo> findConfirmingTypesetting(String manufacturerMetaId, String typesettingId, int current, int size) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("manufacturerMetaId 不能为空");
        }
        if (current < 1) {
            current = 1;
        }
        if (size < 1 || size > 100) {
            size = 20;
        }

        List<TypesettingInfo> confirmingTypesettingInfos = domainTypesettingService.findTypesettingByConditions(
                manufacturerMetaId,
                TypesettingStatus.CONFIRMING.getCode(),
                null,
                null,
                1,
                Integer.MAX_VALUE
        );
        List<TypesettingInfo> inProgressTypesettingInfos = domainTypesettingService.findTypesettingByConditions(
                manufacturerMetaId,
                TypesettingStatus.IN_PROGRESS.getCode(),
                null,
                null,
                1,
                Integer.MAX_VALUE
        );
        List<TypesettingInfo> failedTypesettingInfos = domainTypesettingService.findTypesettingByConditions(
                manufacturerMetaId,
                TypesettingStatus.FAILED.getCode(),
                null,
                null,
                1,
                Integer.MAX_VALUE
        );

        List<TypesettingInfo> allTypesettingInfos = new ArrayList<>();
        if (confirmingTypesettingInfos != null) {
            allTypesettingInfos.addAll(confirmingTypesettingInfos);
        }
        if (inProgressTypesettingInfos != null) {
            allTypesettingInfos.addAll(inProgressTypesettingInfos);
        }
        if (failedTypesettingInfos != null) {
            allTypesettingInfos.addAll(failedTypesettingInfos);
        }

        if (StringUtils.isNotBlank(typesettingId)) {
            String keyword = typesettingId.trim().toLowerCase();
            allTypesettingInfos = allTypesettingInfos.stream()
                    .filter(info -> StringUtils.isNotBlank(info.getTypesettingId())
                            && info.getTypesettingId().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }

        allTypesettingInfos.sort(Comparator
                .comparing((TypesettingInfo info) -> Boolean.TRUE.equals(info.getIsUrgent()))
                .reversed()
                .thenComparing(TypesettingInfo::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        int fromIndex = Math.min((current - 1) * size, allTypesettingInfos.size());
        int toIndex = Math.min(fromIndex + size, allTypesettingInfos.size());
        List<TypesettingInfo> pagedTypesettingInfos = allTypesettingInfos.subList(fromIndex, toIndex);
        for (TypesettingInfo typesettingInfo : pagedTypesettingInfos) {
            if (typesettingInfo == null || typesettingInfo.getElement() == null) {
                continue;
            }
            typesettingInfo.getElement().setWidth(ceilBigDecimal(typesettingInfo.getElement().getWidth()));
            typesettingInfo.getElement().setHeight(ceilBigDecimal(typesettingInfo.getElement().getHeight()));
        }
        fillTypesettingSourceCellPreviewUrls(pagedTypesettingInfos);
        fillLayoutModeDescription(pagedTypesettingInfos);

        long total = allTypesettingInfos.size();

        return new PagedResult<>(pagedTypesettingInfos, total, size, current);
    }

    /**
     * 为待确认排版来源单元补充预览地址。
     * <ul>
     *   <li>生产工件：读取 productionPiece.productImageFile.filePreview.preview；</li>
     *   <li>印版：读取 source typesetting.element.nestedSvg。</li>
     * </ul>
     */
    private void fillTypesettingSourceCellPreviewUrls(List<TypesettingInfo> typesettingInfos) {
        if (typesettingInfos == null || typesettingInfos.isEmpty()) {
            return;
        }
        Map<String, String> productionPiecePreviewUrlCache = new HashMap<>();
        Map<String, String> typesettingPreviewUrlCache = new HashMap<>();
        for (TypesettingInfo typesettingInfo : typesettingInfos) {
            if (typesettingInfo == null || typesettingInfo.getTypesettingCells() == null) {
                continue;
            }
            for (TypesettingSourceCell cell : typesettingInfo.getTypesettingCells()) {
                if (cell == null || StringUtils.isBlank(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                    continue;
                }
                if (TypesettingSourceType.PART.getCode().equals(cell.getSourceType())) {
                    cell.setPreviewUrl(productionPiecePreviewUrlCache.computeIfAbsent(
                            cell.getSourceId(), this::resolveProductionPiecePreviewUrl));
                } else if (TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                    cell.setPreviewUrl(typesettingPreviewUrlCache.computeIfAbsent(
                            cell.getSourceId(), this::resolveTypesettingPreviewUrl));
                }
            }
        }
    }

    private String resolveProductionPiecePreviewUrl(String productionPieceId) {
        if (StringUtils.isBlank(productionPieceId)) {
            return null;
        }
        ProductionPiece productionPiece = productionPieceService.findById(productionPieceId);
        if (productionPiece == null
                || productionPiece.getProductImageFile() == null
                || productionPiece.getProductImageFile().getFilePreview() == null) {
            return null;
        }
        return productionPiece.getProductImageFile().getFilePreview().getPreview();
    }

    private String resolveTypesettingPreviewUrl(String typesettingInfoId) {
        if (StringUtils.isBlank(typesettingInfoId)) {
            return null;
        }
        TypesettingInfo sourceTypesetting = domainTypesettingService.findById(typesettingInfoId);
        if (sourceTypesetting == null || sourceTypesetting.getElement() == null) {
            return null;
        }
        return sourceTypesetting.getElement().getNestedSvg();
    }

    /**
     * 根据 typesettingId 全量查询待确认排版数据，包含对应的 -Mirror 数据。
     */
    public List<TypesettingInfo> findConfirmingTypesettingByTypesettingId(String manufacturerMetaId, String typesettingId) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("manufacturerMetaId 不能为空");
        }
        if (StringUtils.isBlank(typesettingId)) {
            throw new IllegalArgumentException("typesettingId 不能为空");
        }
        List<TypesettingInfo> confirmingTypesettingInfos = domainTypesettingService.findTypesettingByConditions(
                manufacturerMetaId,
                TypesettingStatus.CONFIRMING.getCode(),
                null,
                null,
                1,
                Integer.MAX_VALUE
        );
        List<TypesettingInfo> result = confirmingTypesettingInfos == null
                ? new ArrayList<>()
                : confirmingTypesettingInfos.stream()
                .filter(info -> info != null && matchesTypesettingId(info.getTypesettingId(), typesettingId))
                .collect(Collectors.toList());
        result.sort(Comparator
                .comparing((TypesettingInfo info) -> Boolean.TRUE.equals(info.getIsUrgent()))
                .reversed()
                .thenComparing(TypesettingInfo::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        for (TypesettingInfo typesettingInfo : result) {
            if (typesettingInfo == null || typesettingInfo.getElement() == null) {
                continue;
            }
            typesettingInfo.getElement().setWidth(ceilBigDecimal(typesettingInfo.getElement().getWidth()));
            typesettingInfo.getElement().setHeight(ceilBigDecimal(typesettingInfo.getElement().getHeight()));
        }
        fillLayoutModeDescription(result);
        return result;
    }

    /**
     * 为待确认列表响应补充排版方式业务描述，同时保留 layoutMode 编码。
     */
    private void fillLayoutModeDescription(List<TypesettingInfo> typesettingInfos) {
        if (typesettingInfos == null || typesettingInfos.isEmpty()) {
            return;
        }
        for (TypesettingInfo typesettingInfo : typesettingInfos) {
            if (typesettingInfo == null || StringUtils.isBlank(typesettingInfo.getLayoutMode())) {
                continue;
            }
            typesettingInfo.setDescription(TypesettingLayoutMode.fromCode(typesettingInfo.getLayoutMode()).getDescription());
        }
    }

    /**
     * 查询全部（零件 + 排版）
     */
    private List<TypesettingProductionPieceVO> queryBoth(TypesettingQuery query) {
        List<TypesettingProductionPieceVO> result = new ArrayList<>();

        // 查询零件（只查询待排版状态）
        result.addAll(queryPartsOnly(query));

        // 查询排版
        result.addAll(queryTypesettingOnly(query));

        return result;
    }

    /**
     * 统计全部数量
     */
    private long countBoth(TypesettingQuery query) {
        return countPartsOnly(query) + countTypesettingOnly(query);
    }

    /**
     * 只查询生产中且仍有待排版节点数量的零件。
     */
    private List<TypesettingProductionPieceVO> queryPartsOnly(TypesettingQuery query) {
        // 不走分页查询：先按 manufacturerId 查询符合基础条件的全部零件，再在内存中过滤“待排版数量>0”
        List<ProductionPiece> parts = productionPieceService.findPendingTypesettingPiecesByProcessingConditions(
                query.getManufacturerMetaId(),
                query.getMaterialName(),
                query.getProcessingName(),
                query.getOrderItemId(),
                query.getRouteId(),
                query.getStartTime(),
                query.getEndTime()
        );

        // 转换为 VO
        List<TypesettingProductionPieceVO> voList = new ArrayList<>();
        Map<String, String> orderGroupIdCache = loadOrderGroupIds(parts, query);
        for (ProductionPiece piece : parts) {
            if (getPendingTypesettingQuantity(piece) <= 0) {
                continue;
            }
            TypesettingProductionPieceVO vo = TypesettingProductionPieceVO.fromProductionPiece(piece);
            applyECommerceGroupId(vo, piece, query, orderGroupIdCache);
            voList.add(vo);
        }

        return voList;
    }

    /**
     * 统计零件数量
     */
    private long countPartsOnly(TypesettingQuery query) {
        return queryPartsOnly(query).size();
    }

    /**
     * 只查询排版
     */
    private List<TypesettingProductionPieceVO> queryTypesettingOnly(TypesettingQuery query) {
        // 不走分页查询：先查全量排版记录，再在内存中过滤 leaveQuantity > 0
        List<TypesettingInfo> typesettings = domainTypesettingService.findTypesettingByProcessingConditions(
                query.getManufacturerMetaId(),
                query.getStatus(),
                query.getMaterialName(),
                query.getProcessingName(),
                query.getStartTime(),
                query.getEndTime(),
                null,
                1,
                Integer.MAX_VALUE
        );

        // 转换为 VO
        List<TypesettingProductionPieceVO> voList = new ArrayList<>();
        for (TypesettingInfo info : typesettings) {
            Integer leaveQuantity = info.getLeaveQuantity() == null ? 0 : info.getLeaveQuantity();
            if (leaveQuantity <= 0) {
                continue;
            }
            voList.add(TypesettingProductionPieceVO.fromTypesettingInfo(info));
        }

        return voList;
    }

    /**
     * 统计排版数量
     */
    private long countTypesettingOnly(TypesettingQuery query) {
        return queryTypesettingOnly(query).size();
    }

    private int getPendingTypesettingQuantity(ProductionPiece piece) {
        if (piece == null || piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
            return 0;
        }
        for (ProcedureFlowNode node : piece.getProcedureFlow().getNodes()) {
            if ("待排版".equals(node.getNodeName())) {
                return node.getPieceQuantity() == null ? 0 : node.getPieceQuantity();
            }
        }
        return 0;
    }

    /**
     * 并发备注：
     * toLayout 会扣减来源零件的“待排版”数量或来源印版的剩余数量。
     * 同一个来源对象同一时间只允许一个请求进入，避免两个人同时排版同一个零件/印版导致重复扣减或重复提交算法任务。
     */
    private List<String> buildToLayoutOperationLockKeys(LayoutConfirmRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getTypesettingCells())) {
            return Collections.emptyList();
        }
        return request.getTypesettingCells().stream()
                .filter(Objects::nonNull)
                .filter(cell -> !Integer.valueOf(0).equals(cell.getQuantity()))
                .filter(cell -> StringUtils.isNotBlank(cell.getSourceType()) && StringUtils.isNotBlank(cell.getSourceId()))
                .map(cell -> TYPESETTING_OPERATION_LOCK_PREFIX + "toLayout:" + cell.getSourceType() + ":" + cell.getSourceId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 并发备注：
     * confirmLayout 与 confirmPrint 都会把同一条排版记录从“待确认”推进到后续生成印版任务，
     * 因此二者使用同一个 confirm 锁，避免一个人确认排版、另一个人确认打印时重复确认同一条记录。
     */
    private String buildConfirmLayoutOperationLockKey(String id) {
        return TYPESETTING_OPERATION_LOCK_PREFIX + "confirm:" + id;
    }

    private String buildConfirmPrintOperationLockKey(String id) {
        return TYPESETTING_OPERATION_LOCK_PREFIX + "confirm:" + id;
    }

    private String acquireOperationLocks(List<String> lockKeys, String failureMessage) {
        if (CollectionUtils.isEmpty(lockKeys)) {
            return null;
        }
        String token = UUID.randomUUID().toString();
        List<String> acquiredKeys = new ArrayList<>();
        try {
            for (String lockKey : lockKeys) {
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                        lockKey,
                        token,
                        TYPESETTING_OPERATION_LOCK_EXPIRE_MINUTES,
                        TimeUnit.MINUTES
                );
                if (!Boolean.TRUE.equals(acquired)) {
                    throw new IllegalStateException(failureMessage);
                }
                acquiredKeys.add(lockKey);
            }
            return token;
        } catch (RuntimeException ex) {
            releaseOperationLocks(acquiredKeys, token);
            throw ex;
        }
    }

    private void releaseOperationLocks(List<String> lockKeys, String token) {
        if (CollectionUtils.isEmpty(lockKeys) || StringUtils.isBlank(token)) {
            return;
        }
        // 当前 MongoDB 部署为 standalone，不支持 Spring Mongo 事务；这里不注册事务同步，
        // 避免触发 “Transaction numbers are only allowed on a replica set member or mongos”。
        // 方法内数据库写入完成后再进入 finally 释放锁，仍可保证同一来源/同一排版记录的并发请求串行执行。
        releaseOperationLocksImmediately(lockKeys, token);
    }

    private void releaseOperationLocksImmediately(List<String> lockKeys, String token) {
        DefaultRedisScript<Long> releaseScript = new DefaultRedisScript<>();
        releaseScript.setResultType(Long.class);
        releaseScript.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else return 0 end"
        );
        for (String lockKey : lockKeys) {
            try {
                redisTemplate.execute(releaseScript, Collections.singletonList(lockKey), token);
            } catch (Exception ex) {
                log.warn("释放排版操作锁失败，lockKey={}", lockKey, ex);
            }
        }
    }

    private void ensureTypesettingStatus(TypesettingInfo typesettingInfo, TypesettingStatus expectedStatus, String message) {
        if (typesettingInfo == null || expectedStatus == null) {
            return;
        }
        if (!expectedStatus.getCode().equals(typesettingInfo.getStatus())) {
            throw new IllegalStateException(message + "，当前状态：" + typesettingInfo.getStatus());
        }
    }

    /**
     * 确认排版：校验材料和工艺，调用 API 生成排版结果，并更新生产工件状态
     * @return 排版结果
     */
    public LayoutConfirmResult toLayout(LayoutConfirmRequest request) {
        List<String> operationLockKeys = buildToLayoutOperationLockKeys(request);
        String operationLockToken = acquireOperationLocks(operationLockKeys, "排版对象正在处理中，请稍后重试");
        try {
            return doToLayout(request);
        } finally {
            releaseOperationLocks(operationLockKeys, operationLockToken);
        }
    }

    private LayoutConfirmResult doToLayout(LayoutConfirmRequest request) {
        List<ProductionPiece> productionPieces = new ArrayList<>();
        List<TypesettingInfo> typesettingInfos = new ArrayList<>();
        List<TypesettingProductionPieceVO> typesettingCells = request.getTypesettingCells();
        if (typesettingCells == null) {
            typesettingCells = new ArrayList<>();
        } else {
            typesettingCells = typesettingCells.stream()
                    .filter(cell -> !Integer.valueOf(0).equals(cell.getQuantity()))
                    .collect(Collectors.toList());
            request.setTypesettingCells(typesettingCells);
        }

        for (TypesettingProductionPieceVO cell : typesettingCells) {
            if (cell == null || StringUtils.isBlank(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            if (TypesettingSourceType.PART.getCode().equals(cell.getSourceType())) {
                ProductionPiece productionPiece = cell.toProductionPiece();
                ProductionPiece dbPiece = productionPieceService.findById(productionPiece.getId());
                Integer quantity = productionPiece.getQuantity();
                if (dbPiece == null) {
                    throw new IllegalArgumentException("生产工件不存在：" + productionPiece.getProductionPieceId());
                }
                dbPiece.setQuantity(quantity);
                cell.setQuantity(quantity);
                cell.setIsRedo(dbPiece.getIsRedo());
                cell.setHaveBlood(isBloodPieceByCoordinates(dbPiece));
                productionPieces.add(dbPiece);
            } else if (TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                TypesettingInfo typesettingInfo = cell.toTypesettingInfo();
                TypesettingInfo dbTypesettingInfo = domainTypesettingService.findById(typesettingInfo.getId());
                if (dbTypesettingInfo == null) {
                    throw new IllegalArgumentException("排版信息不存在：" + typesettingInfo.getId());
                }
                if (typesettingInfo.getQuantity() != null) {
                    dbTypesettingInfo.setQuantity(typesettingInfo.getQuantity());
                }
                cell.setHaveBlood(dbTypesettingInfo.getHaveBlood());
                typesettingInfos.add(dbTypesettingInfo);
            }
        }

        for (TypesettingInfo typesettingInfo : typesettingInfos) {
            Integer quantity = typesettingInfo.getQuantity() == null ? 0 : typesettingInfo.getQuantity();
            Integer leaveQuantity = typesettingInfo.getLeaveQuantity() == null ? 0 : typesettingInfo.getLeaveQuantity();
            if (quantity > leaveQuantity) {
                throw new IllegalArgumentException(typesettingInfo.getId() + "排版数量超出");
            }
        }

        for (ProductionPiece productionPiece : productionPieces) {
            Integer quantity = productionPiece.getQuantity();
            int pendingQuantity = getPendingTypesettingQuantity(productionPiece);
            if (quantity != null && pendingQuantity < quantity) {
                return LayoutConfirmResult.failed(productionPiece.getProductionPieceId() + "可排版数量不足");
            }
        }

        String filmConsistencyResult = validateFilmConsistency(productionPieces, typesettingInfos);
        if (!filmConsistencyResult.equals("PASS")) {
            return LayoutConfirmResult.failed(filmConsistencyResult);
        }
        // 步骤备注1：聚合本次确认印版是否含血位特征（零件当前/历史 + 来源印版haveBlood）
        boolean haveBlood = productionPieces.stream().anyMatch(this::isBloodPieceByCoordinates)
                || typesettingInfos.stream().anyMatch(info -> info != null && Boolean.TRUE.equals(info.getHaveBlood()));
        boolean isUrgent = productionPieces.stream().anyMatch(piece -> piece != null && Boolean.TRUE.equals(piece.getIsUrgent()))
                || typesettingInfos.stream().anyMatch(info -> info != null && Boolean.TRUE.equals(info.getIsUrgent()));

        ProcedureFlow commonProcedureFlow;
        try {
            commonProcedureFlow = validateAndBuildCommonProcedureFlow(productionPieces, typesettingInfos);
        } catch (IllegalArgumentException ex) {
            return LayoutConfirmResult.failed(ex.getMessage());
        }

        // 3. 校验材料是否一致
        String validateMaterialResult = validateMaterials(productionPieces);
        if (!validateMaterialResult.equals("PASS")) {
            return LayoutConfirmResult.failed(validateMaterialResult);
        }

        // 4. 校验特殊工艺的材料一致性
        String validateProcedureResult = validateSpecialProcedureMaterials(productionPieces);
        if (!validateProcedureResult.equals("PASS")) {
            return LayoutConfirmResult.failed(validateProcedureResult);
        }
        try {
            applyToLayoutContainerWidthInset(request, productionPieces, typesettingInfos);
            validateCellSizeAgainstContainers(request, productionPieces, typesettingInfos, TypesettingLayoutMode.fromCode(request.getLayoutMode()));
        } catch (IllegalArgumentException ex) {
            return LayoutConfirmResult.failed(ex.getMessage());
        }
        //记录id，供callback使用
        String cacheKey = generateTypesettingId(request.getManufacturerMetaId());
        try {
            String requestJson = JSON.toJSONString(request);
            redisTemplate.opsForValue().set(cacheKey, requestJson, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            return LayoutConfirmResult.failed("缓存请求数据失败：" + e.getMessage());
        }
        // 5. 调用排版 API
        NestingRequest nestingRequest;
        try {
            nestingRequest = buildNestingRequest(request, cacheKey);
        } catch (Exception e) {
            return LayoutConfirmResult.failed(e.getMessage());
        }
        TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(request.getLayoutMode());
        NestingResponse nestingResponse;
        switch (layoutMode.getLayoutCategory()) {
            case "grid_typesetting":
                nestingResponse = algorithmCoreApiService.generateGridNestedFilesAsync(nestingRequest);
                break;
            case "vertical_typesetting":
                nestingResponse = algorithmCoreApiService.generateVerticalNestedFilesAsync(nestingRequest);
                break;
            case "rect_typesetting":
                nestingResponse = algorithmCoreApiService.generateRectNestedFilesAsync(nestingRequest);
                break;
            default:
                nestingResponse = algorithmCoreApiService.generateNestedFilesAsync(nestingRequest);
                break;
        }
//        if (nestingResponse == null || StringUtils.isBlank(nestingResponse.getStatus())) {
//            return LayoutConfirmResult.failed("排版算法调用失败：返回为空");
//        }

        // 6. 构建返回结果
        LayoutConfirmResult result = new LayoutConfirmResult();
        result.setSuccess(true);
        result.setMessage("排版开始,耐心请等待");

        // 7. 异步排版任务受理后，按本次数量更新零件/模板的剩余数量与工序流转
        for (ProductionPiece piece : productionPieces) {
            try {
                Integer quantity = piece.getQuantity();
                if (quantity == null || quantity <= 0) {
                    continue;
                }
                productionPieceService.transferPieceQuantityBetweenNodes(
                        piece.getId(),
                        "NODE_TYPESETTING",
                        "NODE_TYPESETTING_IN_PROGRESS",
                        quantity
                );
            } catch (Exception e) {
                throw new IllegalStateException("更新生产工件 " + piece.getId() + " 节点数量失败：" + e.getMessage(), e);
            }
        }
        for (TypesettingInfo info : typesettingInfos) {
            Integer quantity = info.getQuantity();
            if (quantity == null || quantity <= 0) {
                continue;
            }
            Integer leaveQuantity = info.getLeaveQuantity() == null ? 0 : info.getLeaveQuantity();
            info.setLeaveQuantity(Math.max(leaveQuantity - quantity, 0));
            domainTypesettingService.updateTypesetting(info);
        }
        //添加排版信息
        TypesettingInfo typesettingInfo = new TypesettingInfo();
        typesettingInfo.setTypesettingId(cacheKey);
        typesettingInfo.setElement(null);
        List<String> materialConfigs = productionPieces.stream()
                .map(ProductionPiece::getMaterialConfig)
                .filter(Objects::nonNull)
                .map(MaterialConfig::getMaterialId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        MaterialConfig unifiedMaterialConfig = productionPieces.stream()
                .map(ProductionPiece::getMaterialConfig)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> typesettingInfos.stream()
                        .map(TypesettingInfo::getMaterialConfig)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
        String commonProcessingFlow = resolveProcessingFlowFromOrderItem(productionPieces, typesettingInfos);
        if (materialConfigs.isEmpty() && unifiedMaterialConfig != null && StringUtils.isNotBlank(unifiedMaterialConfig.getMaterialId())) {
            materialConfigs = Collections.singletonList(unifiedMaterialConfig.getMaterialId());
        }
        typesettingInfo.setMaterialConfig(unifiedMaterialConfig);
        typesettingInfo.setMaterialConfigs(materialConfigs);
        typesettingInfo.setProcessingFlow(commonProcessingFlow);
        typesettingInfo.setProcedureFlow(commonProcedureFlow);
        typesettingInfo.setManufacturerMetaId(request.getManufacturerMetaId());
        typesettingInfo.setStatus(TypesettingStatus.IN_PROGRESS.getCode());
        typesettingInfo.setQuantity(1);
        typesettingInfo.setLeaveQuantity(1);
        typesettingInfo.setIsUrgent(isUrgent);
        typesettingInfo.setHaveBlood(haveBlood);
        if (StringUtils.isNotBlank(request.getLayoutMode())) {
            typesettingInfo.setLayoutMode(request.getLayoutMode());
        } else if (!typesettingInfos.isEmpty()) {
            typesettingInfo.setLayoutMode(typesettingInfos.get(0).getLayoutMode());
        }
        typesettingInfo.setTypesettingCells(toSourceCells(request.getTypesettingCells()));
        // 生产工件只要携带 marks，就会按特殊 element 参与排版；这里同步把来源 marks 汇总到新建的 typesettingInfo。
        LinkedHashMap<String, String> mergedMarks = new LinkedHashMap<>();
        mergeSourceProductionPieceMarks(productionPieces, mergedMarks);
        mergeSourceTypesettingMarks(typesettingInfos, mergedMarks);
        Map<String, String> markerMarks = extractCaifuOpenBackMarkerMarks(nestingRequest, layoutMode);
        if (!markerMarks.isEmpty()) {
            mergeMarkMapIncrementally(markerMarks, mergedMarks, "marker");
        }
        if (!mergedMarks.isEmpty()) {
            typesettingInfo.setMarks(mergedMarks);
        }
        domainTypesettingService.addTypesetting(typesettingInfo);
        return result;
    }


    private void mergeSourceProductionPieceMarks(List<ProductionPiece> productionPieces, LinkedHashMap<String, String> target) {
        if (productionPieces == null || target == null) {
            return;
        }
        for (ProductionPiece piece : productionPieces) {
            if (piece == null || piece.getMarks() == null || piece.getMarks().isEmpty()) {
                continue;
            }
            mergeMarkMapIncrementally(piece.getMarks(), target, resolveProductionPieceMarkSourceKey(piece));
        }
    }

    private void mergeSourceTypesettingMarks(List<TypesettingInfo> typesettingInfos, LinkedHashMap<String, String> target) {
        if (typesettingInfos == null || target == null) {
            return;
        }
        for (TypesettingInfo info : typesettingInfos) {
            if (info == null || info.getMarks() == null || info.getMarks().isEmpty()) {
                continue;
            }
            mergeMarkMapIncrementally(info.getMarks(), target, resolveTypesettingMarkSourceKey(info));
        }
    }

    private void mergeMarkMapIncrementally(Map<String, String> sourceMarks, LinkedHashMap<String, String> target, String sourceKey) {
        if (sourceMarks == null || sourceMarks.isEmpty() || target == null) {
            return;
        }
        for (Map.Entry<String, String> entry : sourceMarks.entrySet()) {
            if (entry == null || StringUtils.isBlank(entry.getValue())) {
                continue;
            }
            String baseKey = StringUtils.isNotBlank(entry.getKey()) ? entry.getKey() : ("sourceMark_" + target.size());
            target.put(resolveIncrementalMarkKey(target, baseKey, sourceKey), entry.getValue());
        }
    }

    private String resolveIncrementalMarkKey(LinkedHashMap<String, String> target, String baseKey, String sourceKey) {
        if (!target.containsKey(baseKey)) {
            return baseKey;
        }
        String sourcePrefix = StringUtils.isNotBlank(sourceKey) ? sourceKey : "source";
        String candidate = sourcePrefix + ":" + baseKey;
        int index = 1;
        while (target.containsKey(candidate)) {
            candidate = sourcePrefix + ":" + baseKey + "_" + index;
            index++;
        }
        return candidate;
    }

    private String resolveProductionPieceMarkSourceKey(ProductionPiece piece) {
        if (piece == null) {
            return "productionPiece";
        }
        if (StringUtils.isNotBlank(piece.getProductionPieceId())) {
            return piece.getProductionPieceId();
        }
        if (StringUtils.isNotBlank(piece.getId())) {
            return piece.getId();
        }
        return "productionPiece";
    }

    private String resolveTypesettingMarkSourceKey(TypesettingInfo info) {
        if (info == null) {
            return "typesetting";
        }
        if (StringUtils.isNotBlank(info.getTypesettingId())) {
            return info.getTypesettingId();
        }
        if (StringUtils.isNotBlank(info.getId())) {
            return info.getId();
        }
        return "typesetting";
    }

    private Map<String, String> extractCaifuOpenBackMarkerMarks(NestingRequest nestingRequest, TypesettingLayoutMode layoutMode) {
        if (layoutMode != TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_FILM
                && layoutMode != TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_NO_FILM) {
            return Collections.emptyMap();
        }
        if (nestingRequest == null || nestingRequest.getNestManifest() == null
                || nestingRequest.getNestManifest().getElements() == null) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, String> markMap = new LinkedHashMap<>();
        int idx = 1;
        for (NestingRequest.Element element : nestingRequest.getNestManifest().getElements()) {
            if (element == null || StringUtils.isBlank(element.getId()) || StringUtils.isBlank(element.getImg())) {
                continue;
            }
            String id = element.getId();
            String lowerImg = element.getImg().toLowerCase(Locale.ROOT);
            if (!lowerImg.endsWith(".png")) {
                continue;
            }
            boolean caifuMarkerByPath = lowerImg.contains("/mark/") && lowerImg.contains("/caifu/");
            boolean caifuMarkerByName = id.matches("[0-9a-fA-F-]{36}") && lowerImg.endsWith(id.toLowerCase(Locale.ROOT) + ".png");
            if (!caifuMarkerByPath && !caifuMarkerByName) {
                continue;
            }
            markMap.put("caifuMarker_" + idx, element.getImg());
            idx++;
        }
        return markMap;
    }

    /**
     * 确认排版主流程。
     *
     * <p>业务目标：把“待确认”的排版记录转换为一条可提交给算法服务的印版生成任务。
     * 当前关键步骤如下：
     * <ol>
     *   <li>参数校验：必须传入排版记录 ID；</li>
     *   <li>数据库读取：按 ID 查询最新 TypesettingInfo；</li>
     *   <li>业务校验：要求存在 nestedSvg；</li>
     *   <li>模式确定：优先使用本次请求 layoutMode，否则回退到数据库记录；</li>
     *   <li>模式派生：调用 applyLayoutModeConfig 回填 requireJson/requirePlt/anchor 等派生字段；</li>
     *   <li>构建 FormeGenerationRequest 并异步提交给算法服务。</li>
     * </ol>
     *
     * <p>说明：plt 二维码模式依赖 manufacturerMetaId 生成队列码与二维码。
     */
    public LayoutConfirmResult confirmLayout(TypesettingInfo request) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            throw new IllegalArgumentException("确认排版参数不能为空，且必须包含排版ID");
        }
        List<String> operationLockKeys = Collections.singletonList(buildConfirmLayoutOperationLockKey(request.getId()));
        String operationLockToken = acquireOperationLocks(operationLockKeys, "排版记录正在确认中，请勿重复确认");
        try {
            return doConfirmLayout(request);
        } finally {
            releaseOperationLocks(operationLockKeys, operationLockToken);
        }
    }

    private LayoutConfirmResult doConfirmLayout(TypesettingInfo request) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            throw new IllegalArgumentException("确认排版参数不能为空，且必须包含排版ID");
        }
        TypesettingInfo typesettingInfo = domainTypesettingService.findById(request.getId());
        if (typesettingInfo == null) {
            throw new IllegalArgumentException("排版信息不存在：" + request.getId());
        }
        try {
            validateConfirmLayoutPreconditions(typesettingInfo);

            TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(
                    StringUtils.isNotBlank(request.getLayoutMode()) ? request.getLayoutMode() : typesettingInfo.getLayoutMode()
            );
            if (typesettingInfo.getElement() == null || StringUtils.isBlank(typesettingInfo.getElement().getNestedSvg())) {
                throw new TypesettingPreAlgorithmValidationException("排版信息缺少 nestedSvg，无法确认排版");
            }
            if (requireManufacturerMetaId(layoutMode) && StringUtils.isBlank(typesettingInfo.getManufacturerMetaId())) {
                throw new TypesettingPreAlgorithmValidationException("plt二维码排版缺少 manufacturerMetaId，无法生成队列编号与二维码");
            }
            typesettingInfo.setLayoutMode(layoutMode.getCode());
            typesettingInfo.applyLayoutModeConfig();

            String businessId = resolveFormeBusinessId(typesettingInfo, layoutMode);
            FormeGenerationRequest formeRequest = buildFormeGenerationRequest(typesettingInfo, layoutMode, businessId);
            mergeAnchorPointMarks(typesettingInfo, formeRequest);
            String formeOpRemark = "FORME_OP:LAYOUT";
            // 先落库为确认中状态，再提交异步任务，避免算法服务快速回调时读不到 FORME_OP 标记而跳过回调落库。
            typesettingInfo.setStatus(TypesettingStatus.CONFIRMED.getCode());
            typesettingInfo.setRemark(formeOpRemark);
            mergeExistingMarksBeforeUpdate(typesettingInfo);
            domainTypesettingService.updateTypesetting(typesettingInfo);
            String formeRequestJson = JSON.toJSONString(formeRequest);
            log.info("formeRequest========:{}", formeRequestJson);
            algorithmCoreApiService.generateFormeAsync(formeRequestJson, formeRequest.getCallbackConfig().getCallbackUrl());

            TypesettingInfo mirrorTypesettingInfo = resolveMirrorTypesettingInfo(typesettingInfo);
            if (mirrorTypesettingInfo != null) {
                if (mirrorTypesettingInfo.getElement() != null && StringUtils.isNotBlank(mirrorTypesettingInfo.getElement().getNestedMirrorSvg())) {
                    mirrorTypesettingInfo.getElement().setNestedSvg(mirrorTypesettingInfo.getElement().getNestedMirrorSvg());
                }
                mirrorTypesettingInfo.setRemark(formeOpRemark);
                mirrorTypesettingInfo.setStatus(TypesettingStatus.CONFIRMED.getCode());
                ensureMirrorTypesettingExists(mirrorTypesettingInfo);
                FormeGenerationRequest mirrorFormeRequest = buildFormeGenerationRequest(
                        mirrorTypesettingInfo,
                        TypesettingLayoutMode.DOUBLE_SIDE_MOUNTING_LAYOUT,
                        resolveMirrorFormeBusinessId(mirrorTypesettingInfo, businessId)
                );
                mergeAnchorPointMarks(mirrorTypesettingInfo, mirrorFormeRequest);
                // 镜像印版由 DoubleSideMountingLayoutBuildService 回填了 marks，这里同步落库
                mergeExistingMarksBeforeUpdate(mirrorTypesettingInfo);
                domainTypesettingService.updateTypesetting(mirrorTypesettingInfo);
                String mirrorFormeRequestJson = JSON.toJSONString(mirrorFormeRequest);
                log.info("mirrorFormeRequest========:{}", mirrorFormeRequestJson);
                algorithmCoreApiService.generateFormeAsync(mirrorFormeRequestJson, mirrorFormeRequest.getCallbackConfig().getCallbackUrl());
            }

            LayoutConfirmResult result = new LayoutConfirmResult();
            result.setSuccess(true);
            result.setMessage("确认排版任务已提交，等待回调");
            return result;
        } catch (TypesettingPreAlgorithmValidationException e) {
            return LayoutConfirmResult.failed(e.getMessage());
        } catch (Exception e) {
            String failureReason = "确认排版处理失败：" + resolveExceptionMessage(e);
            log.error(failureReason, e);
            markTypesettingFailed(typesettingInfo, failureReason);
            return LayoutConfirmResult.failed(failureReason);
        }
    }

    private void validateConfirmLayoutPreconditions(TypesettingInfo typesettingInfo) {
        try {
            ensureTypesettingStatus(typesettingInfo, TypesettingStatus.CONFIRMING, "只有待确认的排版记录才能确认排版");
            validateNoSecondaryTypesettingCells(typesettingInfo);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new TypesettingPreAlgorithmValidationException(e.getMessage(), e);
        }
    }

    /**
     * 组装印版生成请求（FormeGenerationRequest）。
     *
     * <p>请求包含四部分：
     * <ul>
     *   <li>forme：输入的 nestedSvg、margin、marks、anchorPoints；</li>
     *   <li>outputs：算法需要输出的 json/plt/svg 文件配置；</li>
     *   <li>uploadConfig：上传 OSS 的 STS 与目录；</li>
     *   <li>callbackConfig：异步回调地址与业务透传 ID。</li>
     * </ul>
     */
    private FormeGenerationRequest buildFormeGenerationRequest(TypesettingInfo typesettingInfo,
                                                               TypesettingLayoutMode layoutMode,
                                                               String businessId) {
        FormeGenerationRequest request = new FormeGenerationRequest();
        // element 原始宽高（单位 mm），必须由入参提供
        BigDecimal nestedWidth = typesettingInfo.getElement() != null ? typesettingInfo.getElement().getWidth() : null;
        BigDecimal nestedHeight = typesettingInfo.getElement() != null ? typesettingInfo.getElement().getHeight() : null;
        if (nestedWidth == null || nestedHeight == null) {
            throw new IllegalArgumentException("buildFormeGenerationRequest 缺少必要参数：nestedWidth 和 nestedHeight 必须有入参");
        }
        SvgRootSize rawNestedSize = resolveRawNestedSize(typesettingInfo);
        if (rawNestedSize != null) {
            nestedWidth = rawNestedSize.width;
            nestedHeight = rawNestedSize.height;
        }
        BigDecimal marginHeight = BigDecimal.valueOf(TAG_STRIP_HEIGHT_MM);

        // 1) 选择当前 mode 对应的独立构建 service
        TypesettingLayoutModeBuildService modeBuildService = layoutModeBuildServiceMap.get(layoutMode);
        if (modeBuildService == null) {
            throw new IllegalArgumentException("未找到排版模式构建服务: " + layoutMode.getCode());
        }
        // 2) 组装构建上下文（统一单位：mm）
        FormeBuildContext buildContext = new FormeBuildContext();
        buildContext.setTypesettingInfo(typesettingInfo);
        buildContext.setBusinessId(businessId);
        buildContext.setNestedWidth(nestedWidth);
        buildContext.setNestedHeight(nestedHeight);
        buildContext.setMarginHeight(marginHeight);
        buildContext.setElementAResolver(this::extractElementA);
        buildContext.setPlateNameSupplier(() -> generatePrintingPlateName(typesettingInfo.getManufacturerMetaId()));
        buildContext.setPlateNameBBSupplier(() -> generatePrintingPlateName(typesettingInfo.getManufacturerMetaId()));
        buildContext.setQrDataUriGenerator(content -> buildQrCodeDataUri(typesettingInfo.getManufacturerMetaId(), content));
        // 3) 获取模式构建结果（margin/marks/anchors/outputs/uploadPath）
        FormeLayoutBuildResult modeResult = modeBuildService.build(buildContext);

        // 4) 回填 forme 基础输入
        FormeGenerationRequest.FormeInfo formeInfo = new FormeGenerationRequest.FormeInfo();
        formeInfo.setKnife(resolveFormeKnife(typesettingInfo));
        formeInfo.setSvgUrl(buildCompleteOssUrl(typesettingInfo.getElement().getNestedSvg()));
        formeInfo.setMargin(modeResult.getMargin());
        formeInfo.setMarks(modeResult.getMarks());
        formeInfo.setAnchorPoints(modeResult.getAnchorPoints());
        request.setForme(formeInfo);
        request.setOutputs(modeResult.getOutputs());

        applySpecialCraftMarkStrategies(typesettingInfo, request);
        mergeFormeMarkResources(typesettingInfo, request);
        syncTypesettingElementSizeAfterFormeExpand(typesettingInfo, request, rawNestedSize);

        // 5) 注入上传配置（STS + mode 专属上传路径）
        ObjectStorageTempAuthConfig objectStorageTempAuthConfig = aliCloudAuthService.getObjectStorageTempAuthConfig(businessId);
        UploadConfig uploadConfig = new UploadConfig();
        uploadConfig.setUploadPath(appendManufacturerMetaIdToUploadPath(modeResult.getUploadPath(), typesettingInfo));
        uploadConfig.setOssConfig(objectStorageTempAuthConfig);
        request.setUploadConfig(uploadConfig);

        // 6) 配置异步回调
        CallbackConfig callbackConfig = new CallbackConfig();
        callbackConfig.setCallbackUrl(generateFormeUrl);
        CallbackCustomValue callbackCustomValue = new CallbackCustomValue();
        callbackCustomValue.setId(typesettingInfo.getId());
        callbackConfig.setCallbackCustomValue(callbackCustomValue);
        request.setCallbackConfig(callbackConfig);
        return request;
    }


    private String resolveFormeKnife(TypesettingInfo typesettingInfo) {
        return isCoverBoardPartOnlyTypesetting(typesettingInfo)
                ? FormeGenerationElementType.VIBRATION_KNIFE.getCode()
                : FormeGenerationElementType.DRAG_KNIFE.getCode();
    }

    private boolean isCoverBoardPartOnlyTypesetting(TypesettingInfo typesettingInfo) {
        return typesettingInfo != null
                && !CollectionUtils.isEmpty(typesettingInfo.getTypesettingCells())
                && typesettingInfo.getTypesettingCells().stream()
                .allMatch(cell -> cell != null && TypesettingSourceType.PART.getCode().equals(cell.getSourceType()))
                && hasProcedureNode(typesettingInfo.getProcedureFlow(), "覆板");
    }


    private void applySpecialCraftMarkStrategies(TypesettingInfo typesettingInfo, FormeGenerationRequest formeRequest) {
        if (specialCraftMarkStrategies == null || specialCraftMarkStrategies.isEmpty()) {
            return;
        }
        for (SpecialCraftMarkStrategy strategy : specialCraftMarkStrategies) {
            if (strategy == null) {
                continue;
            }
            strategy.apply(typesettingInfo, formeRequest);
        }
    }

    /**
     * 将印版生成阶段的最终外扩尺寸同步回排版元素。
     *
     * <p>confirmLayout / confirmPrint 会通过 forme.margin 在原 nestedSvg 四周追加标签条、定位点或辅助线区域。
     * 算法请求提交后、回调返回前，列表和后续打印任务仍读取 typesetting.element.width/height，
     * 因此需要在所有外扩参数处理完成后，把展示/落库尺寸同步为原印版尺寸 + 四边 margin。
     * 同步时同样优先使用 nestedSvg 原始尺寸，避免反面印版的 element.width/height 被重复外扩。</p>
     */
    private void syncTypesettingElementSizeAfterFormeExpand(TypesettingInfo typesettingInfo,
                                                            FormeGenerationRequest formeRequest,
                                                            SvgRootSize rawNestedSize) {
        if (typesettingInfo == null || typesettingInfo.getElement() == null || formeRequest == null
                || formeRequest.getForme() == null || formeRequest.getForme().getMargin() == null) {
            return;
        }
        TypesettingElement element = typesettingInfo.getElement();
        if (element.getWidth() == null || element.getHeight() == null) {
            return;
        }
        BigDecimal baseWidth = rawNestedSize != null ? rawNestedSize.width : element.getWidth();
        BigDecimal baseHeight = rawNestedSize != null ? rawNestedSize.height : element.getHeight();
        FormeGenerationRequest.Margin margin = formeRequest.getForme().getMargin();
        BigDecimal expandedWidth = baseWidth
                .add(BigDecimal.valueOf(defaultMarginValue(margin.getLeft())))
                .add(BigDecimal.valueOf(defaultMarginValue(margin.getRight())));
        BigDecimal expandedHeight = baseHeight
                .add(BigDecimal.valueOf(defaultMarginValue(margin.getTop())))
                .add(BigDecimal.valueOf(defaultMarginValue(margin.getBottom())));
        element.setWidth(expandedWidth);
        element.setHeight(expandedHeight);
    }

    private int defaultMarginValue(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 从 nestedSvg 根节点读取原始排版宽高。
     *
     * <p>confirmLayout / confirmPrint 为了列表展示会把 element.width/height 同步为扩边后的印版尺寸。
     * 如果后续重复确认或生成 mirror 印版时继续使用已扩边的 element.height，底部标签条坐标会被重复推高
     * （例如 double-side mirror 的 bottom mark 会多出上下 20mm 标签条）。因此构建算法请求时优先以
     * nestedSvg 根节点尺寸作为真正的原始 nested 尺寸，下载或解析失败时再退回 element.width/height。</p>
     */
    private SvgRootSize resolveRawNestedSize(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null || typesettingInfo.getElement() == null
                || StringUtils.isBlank(typesettingInfo.getElement().getNestedSvg())) {
            return null;
        }
        Path tempSvgPath = null;
        try {
            tempSvgPath = downloadNestedSvgToTempFile(typesettingInfo.getElement().getNestedSvg());
            if (tempSvgPath == null) {
                return null;
            }
            String svg = Files.readString(tempSvgPath, StandardCharsets.UTF_8);
            return parseSvgRootSize(svg);
        } catch (Exception e) {
            log.warn("解析 nestedSvg 原始尺寸失败，回退使用 element.width/height: typesettingId={}, nestedSvg={}, error={}",
                    typesettingInfo.getTypesettingId(), typesettingInfo.getElement().getNestedSvg(), e.getMessage());
            return null;
        } finally {
            if (tempSvgPath != null) {
                try {
                    Files.deleteIfExists(tempSvgPath);
                } catch (IOException e) {
                    log.warn("删除 nestedSvg 临时文件失败: {}", tempSvgPath, e);
                }
            }
        }
    }

    private SvgRootSize parseSvgRootSize(String svg) {
        if (StringUtils.isBlank(svg)) {
            return null;
        }
        Matcher svgTagMatcher = SVG_TAG_PATTERN.matcher(svg);
        if (!svgTagMatcher.find()) {
            return null;
        }
        String openTag = svgTagMatcher.group();
        BigDecimal width = null;
        BigDecimal height = null;
        Matcher sizeMatcher = SVG_ROOT_SIZE_PATTERN.matcher(openTag);
        while (sizeMatcher.find()) {
            String attribute = sizeMatcher.group(1);
            BigDecimal value = new BigDecimal(sizeMatcher.group(2));
            if ("width".equalsIgnoreCase(attribute)) {
                width = value;
            } else if ("height".equalsIgnoreCase(attribute)) {
                height = value;
            }
        }
        if (width == null || height == null) {
            Matcher viewBoxMatcher = SVG_VIEW_BOX_PATTERN.matcher(openTag);
            if (viewBoxMatcher.find()) {
                if (width == null) {
                    width = new BigDecimal(viewBoxMatcher.group(1));
                }
                if (height == null) {
                    height = new BigDecimal(viewBoxMatcher.group(2));
                }
            }
        }
        if (width == null || height == null || width.compareTo(BigDecimal.ZERO) <= 0 || height.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return new SvgRootSize(width, height);
    }

    private static class SvgRootSize {
        private final BigDecimal width;
        private final BigDecimal height;

        private SvgRootSize(BigDecimal width, BigDecimal height) {
            this.width = width;
            this.height = height;
        }
    }

    private void mergeFormeMarkResources(TypesettingInfo typesettingInfo, FormeGenerationRequest formeRequest) {
        if (typesettingInfo == null || formeRequest == null || formeRequest.getForme() == null
                || formeRequest.getForme().getMarks() == null || formeRequest.getForme().getMarks().isEmpty()) {
            return;
        }
        LinkedHashMap<String, String> markMap = new LinkedHashMap<>();
        if (typesettingInfo.getMarks() != null && !typesettingInfo.getMarks().isEmpty()) {
            markMap.putAll(typesettingInfo.getMarks());
        }
        LinkedHashSet<String> existingValues = new LinkedHashSet<>(markMap.values());
        int index = resolveNextFormeMarkIndex(markMap);
        for (FormeGenerationRequest.Mark mark : formeRequest.getForme().getMarks()) {
            if (mark == null || StringUtils.isBlank(mark.getImg())) {
                continue;
            }
            if (existingValues.add(mark.getImg())) {
                markMap.put("formeMarkImg_" + index, mark.getImg());
                index++;
            }
        }
        if (!markMap.isEmpty()) {
            typesettingInfo.setMarks(markMap);
        }
    }

    private int resolveNextFormeMarkIndex(Map<String, String> markMap) {
        if (markMap == null || markMap.isEmpty()) {
            return 0;
        }
        int maxIndex = -1;
        for (String key : markMap.keySet()) {
            if (StringUtils.isBlank(key) || !key.startsWith("formeMarkImg_")) {
                continue;
            }
            String suffix = key.substring("formeMarkImg_".length());
            if (StringUtils.isBlank(suffix)) {
                continue;
            }
            try {
                maxIndex = Math.max(maxIndex, Integer.parseInt(suffix));
            } catch (Exception ignored) {
                // ignore malformed key and continue
            }
        }
        return maxIndex + 1;
    }

    private TypesettingInfo resolveMirrorTypesettingInfo(TypesettingInfo origin) {
        if (mirrorFormeStrategies == null || mirrorFormeStrategies.isEmpty()) {
            return null;
        }
        return mirrorFormeStrategies.stream()
                .filter(strategy -> strategy != null && strategy.supports(origin))
                .findFirst()
                .map(strategy -> strategy.buildMirrorTypesettingInfo(origin))
                .orElse(null);
    }


    private String resolveMirrorFormeBusinessId(TypesettingInfo mirrorTypesettingInfo, String originBusinessId) {
        if (mirrorTypesettingInfo != null && StringUtils.isNotBlank(mirrorTypesettingInfo.getTypesettingId())) {
            return mirrorTypesettingInfo.getTypesettingId() + buildMirrorTemplateSuffix(mirrorTypesettingInfo.getTemplateCode());
        }
        return originBusinessId + "-Mirror";
    }

    private TypesettingInfo findMirrorTypesettingInfo(TypesettingInfo origin) {
        if (origin == null) {
            return null;
        }
        for (String mirrorTypesettingId : buildMirrorTypesettingIdCandidates(origin)) {
            TypesettingInfo mirror = findMirrorTypesettingInfoByTemplate(mirrorTypesettingId, origin.getTemplateCode());
            if (mirror != null && StringUtils.isNotBlank(mirror.getId())) {
                return mirror;
            }
        }
        return null;
    }

    private TypesettingInfo findMirrorTypesettingInfoByTemplate(String mirrorTypesettingId, String templateCode) {
        TypesettingInfo mirror = domainTypesettingService.findTypesettingByTypesettingIdAndTemplateCode(mirrorTypesettingId, templateCode);
        if (mirror != null && StringUtils.isNotBlank(mirror.getId())) {
            return mirror;
        }
        if (StringUtils.isBlank(templateCode) || "1/1".equals(templateCode)) {
            return domainTypesettingService.findTypesettingByTypesettingId(mirrorTypesettingId);
        }
        return null;
    }

    private List<String> buildMirrorTypesettingIdCandidates(TypesettingInfo origin) {
        List<String> candidates = new ArrayList<>();
        if (StringUtils.isNotBlank(origin.getTypesettingId())) {
            candidates.add(origin.getTypesettingId() + "-Mirror");
        }
        if (StringUtils.isNotBlank(origin.getId())) {
            candidates.add(origin.getId() + "-Mirror");
        }
        return candidates.stream().filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
    }

    private String buildMirrorTemplateSuffix(String templateCode) {
        if (StringUtils.isBlank(templateCode) || "1/1".equals(templateCode)) {
            return "";
        }
        return "-" + templateCode.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private void ensureMirrorTypesettingExists(TypesettingInfo mirrorTypesettingInfo) {
        if (mirrorTypesettingInfo == null || StringUtils.isBlank(mirrorTypesettingInfo.getTypesettingId())) {
            return;
        }
        TypesettingInfo existing = domainTypesettingService.findTypesettingByTypesettingIdAndTemplateCode(
                mirrorTypesettingInfo.getTypesettingId(),
                mirrorTypesettingInfo.getTemplateCode()
        );
        if (existing == null) {
            mirrorTypesettingInfo.setId(null);
            TypesettingInfo created = domainTypesettingService.addTypesetting(mirrorTypesettingInfo);
            if (created != null && StringUtils.isNotBlank(created.getId())) {
                mirrorTypesettingInfo.setId(created.getId());
            } else {
                TypesettingInfo persisted = domainTypesettingService.findTypesettingByTypesettingIdAndTemplateCode(
                        mirrorTypesettingInfo.getTypesettingId(),
                        mirrorTypesettingInfo.getTemplateCode()
                );
                if (persisted != null && StringUtils.isNotBlank(persisted.getId())) {
                    mirrorTypesettingInfo.setId(persisted.getId());
                }
            }
            return;
        }
        mirrorTypesettingInfo.setId(existing.getId());
        mirrorTypesettingInfo.setCreateTime(existing.getCreateTime());
        domainTypesettingService.updateTypesetting(mirrorTypesettingInfo);
    }

    private void mergeAnchorPointMarks(TypesettingInfo typesettingInfo, FormeGenerationRequest formeRequest) {
        if (typesettingInfo == null || formeRequest == null || formeRequest.getForme() == null
                || formeRequest.getForme().getAnchorPoints() == null || formeRequest.getForme().getAnchorPoints().isEmpty()) {
            return;
        }
        LinkedHashMap<String, String> markMap = new LinkedHashMap<>();
        if (typesettingInfo.getMarks() != null && !typesettingInfo.getMarks().isEmpty()) {
            markMap.putAll(typesettingInfo.getMarks());
        }
        int anchorIndex = 0;
        for (FormeGenerationRequest.AnchorPoint anchorPoint : formeRequest.getForme().getAnchorPoints()) {
            if (anchorPoint == null || StringUtils.isBlank(anchorPoint.getSvg())) {
                continue;
            }
            markMap.put("anchorPointSvg_" + anchorIndex, anchorPoint.getSvg());
            anchorIndex++;
        }
        if (!markMap.isEmpty()) {
            typesettingInfo.setMarks(markMap);
        }
    }

    /**
     * 提取元素 A（typesettingId 标识）。
     *
     * <p>confirmLayout / confirmPrint 生成 elementF 标签时始终使用当前排版记录的
     * 完整 typesettingId（镜像印版保留 -Mirror 后缀），缺失时再兜底当前记录 id。
     */
    private String extractElementA(TypesettingInfo typesettingInfo) {
        String typesettingId = typesettingInfo.getTypesettingId();
        return StringUtils.isNotBlank(typesettingId) ? typesettingId : typesettingInfo.getId();
    }

    /**
     * 镜像印版与正面印版属于同一次排版业务，印版标签上的 typesettingId 保持原值。
     */
    private String normalizeMirrorTypesettingId(String typesettingId) {
        if (StringUtils.isBlank(typesettingId)) {
            return typesettingId;
        }
        String mirrorSuffix = "-Mirror";
        return typesettingId.toLowerCase(Locale.ROOT).endsWith(mirrorSuffix.toLowerCase(Locale.ROOT))
                ? typesettingId.substring(0, typesettingId.length() - mirrorSuffix.length())
                : typesettingId;
    }

    /**
     * 生成元素 B（xxx.plt 文件名）。
     *
     * <p>号码来源：排版序号池（每工厂按 usageType 维护 1~10000 循环数组）。
     */
    private String generatePrintingPlateName(String manufacturerMetaId) {
        int nextSeq = typesettingSequencePoolService.nextSequence(manufacturerMetaId, TypesettingSequenceUsageType.PLT_FILE_NAME);
        return nextSeq + ".plt";
    }

    /**
     * 生成元素 C（二位码 data URI）。
     *
     * <p>这里不直接落 OSS，先返回 data URI 参与标签条 SVG 组装。
     */
    private String buildQrCodeDataUri(String manufacturerMetaId, String content) {
        GenerateQrCodeRequest qrCodeRequest = new GenerateQrCodeRequest();
        qrCodeRequest.setManufacturerMetaId(manufacturerMetaId);
        qrCodeRequest.setContent(content);
        GenerateQrCodeResult qrCodeResult = generateQrCode(qrCodeRequest);
        return "data:image/png;base64," + qrCodeResult.getQrCodeBase64();
    }


    private String generateTypesettingId(String manufacturerMetaId) {
        int nextSeq = typesettingSequencePoolService.nextSequence(manufacturerMetaId, TypesettingSequenceUsageType.LAYOUT_ID);
        return "LAYOUT" + LocalDateTime.now().format(TYPESETTING_ID_TIME_FORMATTER) + nextSeq;
    }

    /**
     * 在提交算法前扣减 container 规格尺寸。
     * <p>兼容仅传前端 cell 的老调用；toLayout 主流程会在按 sourceId 补全 DB 零件/印版后，
     * 调用 {@link #applyToLayoutContainerWidthInset(LayoutConfirmRequest, List, List)}，避免前端未传
     * procedureFlow/materialConfig 时误走默认扣减。</p>
     */
    public void applyToLayoutContainerWidthInset(LayoutConfirmRequest request) {
        if (request == null) {
            return;
        }
        TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(request.getLayoutMode());
        if (shouldSkipToLayoutContainerWidthInset(layoutMode)) {
            return;
        }
        boolean coverBoardProductionPieceOnlyLayout = isCoverBoardProductionPieceOnlyLayout(request.getTypesettingCells());
        applyContainerSizeInset(
                request,
                resolveContainerWidthInset(request.getTypesettingCells(), layoutMode),
                coverBoardProductionPieceOnlyLayout ? DEFAULT_CONTAINER_HEIGHT_INSET_COVER_BOARD_PARTS_MM : 0
        );
    }

    /**
     * 在提交算法前，基于已从 DB 补全的零件/印版数据扣减 container 规格尺寸。
     * <p>全部实际来源为零件且存在“覆板”工艺时固定扣减宽度 16mm、高度 40mm；其他场景优先按实际来源的
     * materialId + layoutMode 查询 width 内缩配置，未配置时默认扣减 28mm。</p>
     */
    public void applyToLayoutContainerWidthInset(LayoutConfirmRequest request,
                                                 List<ProductionPiece> productionPieces,
                                                 List<TypesettingInfo> typesettingInfos) {
        if (request == null) {
            return;
        }
        TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(request.getLayoutMode());
        if (shouldSkipToLayoutContainerWidthInset(layoutMode)) {
            return;
        }
        boolean coverBoardProductionPieceOnlyLayout = isCoverBoardProductionPieceOnlyLayout(productionPieces, typesettingInfos);
        applyContainerSizeInset(
                request,
                resolveContainerWidthInset(productionPieces, typesettingInfos, layoutMode),
                coverBoardProductionPieceOnlyLayout ? DEFAULT_CONTAINER_HEIGHT_INSET_COVER_BOARD_PARTS_MM : 0
        );
    }

    private boolean shouldSkipToLayoutContainerWidthInset(TypesettingLayoutMode layoutMode) {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A30_LARGE_BOARD == layoutMode
                || TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_FILM == layoutMode
                || TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_NO_FILM == layoutMode
                || TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A30_SMALL_GRAPH == layoutMode;
    }

    private void applyContainerSizeInset(LayoutConfirmRequest request, Integer widthInset, Integer heightInset) {
        if (request == null) {
            return;
        }
        if (CollectionUtils.isEmpty(request.getContainers())) {
            request.setContainers(new ArrayList<>(List.of(new LayoutConfirmRequest.ContainerInfo(1500, 1000))));
        }
        if (widthInset == null || widthInset <= 0) {
            return;
        }
        for (LayoutConfirmRequest.ContainerInfo container : request.getContainers()) {
            if (container == null || container.getWidth() == null) {
                continue;
            }
            int adjustedWidth = container.getWidth() - widthInset;
            if (adjustedWidth <= 0) {
                throw new IllegalArgumentException("containers.width 扣减内缩值后必须大于0");
            }
            Integer adjustedHeight = null;
            if (heightInset != null && heightInset > 0 && container.getHeight() != null) {
                adjustedHeight = container.getHeight() - heightInset;
                if (adjustedHeight <= 0) {
                    throw new IllegalArgumentException("containers.height 扣减内缩值后必须大于0");
                }
            }
            container.setWidth(adjustedWidth);
            if (adjustedHeight != null) {
                container.setHeight(adjustedHeight);
            }
        }
    }

    private Integer resolveContainerWidthInset(List<TypesettingProductionPieceVO> typesettingCells, TypesettingLayoutMode layoutMode) {
        if (isCoverBoardProductionPieceOnlyLayout(typesettingCells)) {
            return DEFAULT_CONTAINER_WIDTH_INSET_COVER_BOARD_PARTS_MM;
        }
        String materialId = resolveSingleMaterialId(typesettingCells);
        if (StringUtils.isNotBlank(materialId) && layoutMode != null && containerWidthInsetService != null) {
            TypesettingContainerWidthInset inset = containerWidthInsetService
                    .findByMaterialIdAndLayoutMode(materialId, layoutMode.getCode());
            if (inset != null && inset.getWidthInset() != null) {
                return inset.getWidthInset();
            }
        }
        return DEFAULT_CONTAINER_WIDTH_INSET_STANDARD_MM;
    }

    private Integer resolveContainerWidthInset(List<ProductionPiece> productionPieces,
                                               List<TypesettingInfo> typesettingInfos,
                                               TypesettingLayoutMode layoutMode) {
        if (isCoverBoardProductionPieceOnlyLayout(productionPieces, typesettingInfos)) {
            return DEFAULT_CONTAINER_WIDTH_INSET_COVER_BOARD_PARTS_MM;
        }
        String materialId = resolveSingleMaterialId(productionPieces, typesettingInfos);
        if (StringUtils.isNotBlank(materialId) && layoutMode != null && containerWidthInsetService != null) {
            TypesettingContainerWidthInset inset = containerWidthInsetService
                    .findByMaterialIdAndLayoutMode(materialId, layoutMode.getCode());
            if (inset != null && inset.getWidthInset() != null) {
                return inset.getWidthInset();
            }
        }
        return DEFAULT_CONTAINER_WIDTH_INSET_STANDARD_MM;
    }

    private boolean isCoverBoardProductionPieceOnlyLayout(List<TypesettingProductionPieceVO> typesettingCells) {
        return isAllProductionPieceCells(typesettingCells) && hasAnyCoverBoardNode(typesettingCells);
    }

    private boolean isCoverBoardProductionPieceOnlyLayout(List<ProductionPiece> productionPieces, List<TypesettingInfo> typesettingInfos) {
        return !CollectionUtils.isEmpty(productionPieces)
                && CollectionUtils.isEmpty(typesettingInfos)
                && productionPieces.stream().allMatch(Objects::nonNull)
                && productionPieces.stream().anyMatch(piece -> hasProcedureNode(piece.getProcedureFlow(), "覆板"));
    }

    private boolean isAllProductionPieceCells(List<TypesettingProductionPieceVO> typesettingCells) {
        if (CollectionUtils.isEmpty(typesettingCells)) {
            return false;
        }
        return typesettingCells.stream()
                .allMatch(cell -> cell != null && TypesettingSourceType.PART.getCode().equals(cell.getSourceType()));
    }

    private boolean hasAnyCoverBoardNode(List<TypesettingProductionPieceVO> typesettingCells) {
        if (CollectionUtils.isEmpty(typesettingCells)) {
            return false;
        }
        return typesettingCells.stream()
                .anyMatch(this::hasCoverBoardNode);
    }

    private String resolveSingleMaterialId(List<TypesettingProductionPieceVO> typesettingCells) {
        if (typesettingCells == null) {
            return null;
        }
        return typesettingCells.stream()
                .filter(Objects::nonNull)
                .map(TypesettingProductionPieceVO::getMaterialConfig)
                .filter(Objects::nonNull)
                .map(MaterialConfig::getMaterialId)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .limit(2)
                .reduce((first, second) -> {
                    throw new IllegalArgumentException("同一次排版只能根据一组 materialId + layoutMode 匹配 containers.width 内缩值");
                })
                .orElse(null);
    }

    private String resolveSingleMaterialId(List<ProductionPiece> productionPieces, List<TypesettingInfo> typesettingInfos) {
        Stream<String> productionPieceMaterialIds = productionPieces == null
                ? Stream.empty()
                : productionPieces.stream()
                .filter(Objects::nonNull)
                .map(ProductionPiece::getMaterialConfig)
                .filter(Objects::nonNull)
                .map(MaterialConfig::getMaterialId);
        Stream<String> typesettingMaterialIds = typesettingInfos == null
                ? Stream.empty()
                : typesettingInfos.stream()
                .filter(Objects::nonNull)
                .map(TypesettingInfo::getMaterialConfig)
                .filter(Objects::nonNull)
                .map(MaterialConfig::getMaterialId);
        return Stream.concat(productionPieceMaterialIds, typesettingMaterialIds)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                // toLayout 已按材料名称校验同材料；同名材料可能来自不同订单并携带不同 ID。
                // 此处只需要一个代表 ID 查询容器内缩配置，不应再将 ID 不同判定为材料不同。
                .findFirst()
                .orElse(null);
    }

    private NestingRequest buildNestingRequest(LayoutConfirmRequest request, String cacheKey) {
        if (StringUtils.isBlank(generateNestedFilesCallbackUrl)) {
            throw new IllegalArgumentException("排版回调地址未配置");
        }
        TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(request.getLayoutMode());
        boolean isVerticalTypesetting = "vertical_typesetting".equals(layoutMode.getLayoutCategory());
        List<ProductionPiece> productionPieces = new ArrayList<>();
        List<TypesettingInfo> typesettingInfos = new ArrayList<>();
        List<TypesettingProductionPieceVO> typesettingCells = request.getTypesettingCells();
        if (typesettingCells != null) {
            for (TypesettingProductionPieceVO cell : typesettingCells) {
                if (cell == null || StringUtils.isBlank(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                    continue;
                }
                if (TypesettingSourceType.PART.getCode().equals(cell.getSourceType())) {
                    ProductionPiece piece = cell.toProductionPiece();
                    ProductionPiece dbPiece = productionPieceService.findById(piece.getId());
                    if (dbPiece == null) {
                        throw new IllegalArgumentException("生产工件不存在：" + piece.getId());
                    }
                    Integer quantity = piece.getQuantity();
                    dbPiece.setQuantity(quantity);
                    cell.setQuantity(quantity);
                    productionPieces.add(dbPiece);
                } else if (TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                    TypesettingInfo info = cell.toTypesettingInfo();
                    TypesettingInfo dbInfo = domainTypesettingService.findById(info.getId());
                    if (dbInfo == null) {
                        throw new IllegalArgumentException("排版信息不存在：" + info.getId());
                    }
                    if (info.getQuantity() != null) {
                        dbInfo.setQuantity(info.getQuantity());
                    }
                    typesettingInfos.add(dbInfo);
                }
            }
        }
        boolean mirrorTypesettingTask = typesettingInfos.stream()
                .filter(Objects::nonNull)
                .map(TypesettingInfo::getTypesettingId)
                .anyMatch(typesettingId -> StringUtils.isNotBlank(typesettingId) && typesettingId.endsWith("-Mirror"));
        // 步骤备注2：计算本次排版全局血位标记（用于align/safeDistance）
        boolean hasBloodPiece = productionPieces.stream().anyMatch(this::isBloodPieceByCoordinates)
                || typesettingInfos.stream().anyMatch(info -> info != null && Boolean.TRUE.equals(info.getHaveBlood()));
        NestingRequestRuleService elementArrangementRule = nestingRequestRuleServiceMap.get(layoutMode);
        if (elementArrangementRule != null) {
            elementArrangementRule.arrangeElementSources(productionPieces, typesettingInfos);
        }
        List<NestingRequest.Element> elements = new ArrayList<>();
        if (productionPieces != null) {
            for (ProductionPiece piece : productionPieces) {
                if (piece == null || StringUtils.isBlank(piece.getProductionPieceId())) {
                    continue;
                }
                NestingRequest.Element element = markedNestingElementService.buildMarkedElement(piece);
                if (element == null) {
                    if (StringUtils.isBlank(piece.getTemplateCode())) {
                        throw new IllegalArgumentException("生产工件缺少排版SVG地址：" + piece.getProductionPieceId());
                    }
                    element = new NestingRequest.Element();
                    element.setId(piece.getId());
                    if (StringUtils.isNotBlank(piece.getRouteSvg())) {
                        element.setSvg(piece.getRouteSvg());
                    } else if (piece.getMaskImageFile() != null && StringUtils.isNotBlank(piece.getMaskImageFile().getRawFile())) {
                        element.setSvg(piece.getMaskImageFile().getRawFile());
                    }
                    element.setCounts(piece.getQuantity() != null && piece.getQuantity() > 0 ? piece.getQuantity() : 1);
                    element.setForme(Boolean.FALSE);
                    String pieceImg = resolvePieceNestingImg(piece, mirrorTypesettingTask);
                    if (StringUtils.isNotBlank(pieceImg)) {
                        element.setImg(pieceImg);
                    }
                }
                if (isVerticalTypesetting) {
                    element.setVMargin(0);
                    element.setHGravity("left");
                    element.setHMargin(0);
                }
                boolean spliceBleedPiece = isSpliceBleedPiece(piece);
                boolean spliceBleedHasVerticalCut = spliceBleedPiece && hasVerticalCut(piece);
                if (spliceBleedPiece) {
                    // 拼接算法当前把主动出血边回写在左边（竖切）或上边（横切）。
                    // 排版时只记录旋转角度，让算法把主动出血边转到右侧后贴紧排放：
                    // 竖切旋转 180°，横切顺时针旋转 90°。
                    element.setRotation(spliceBleedHasVerticalCut ? 180 : 90);
                } else if (isBloodBasedRotationCandidate(piece)) {
                    element.setRotation(-90);
                }
                boolean currentPieceNeedRightAlign = isBloodPieceByCoordinates(piece);
                NestingRequestRuleService nestingRequestRuleService = nestingRequestRuleServiceMap.get(layoutMode);
                if (nestingRequestRuleService != null) {
                    nestingRequestRuleService.applyElementStyle(
                            element, currentPieceNeedRightAlign, hasBloodPiece, !typesettingInfos.isEmpty());
                }
                if (spliceBleedPiece) {
                    element.setAlign("right");
                    element.setSafeDistance(30D);
                } else {
                    applyElementAlignAndSafeDistance(element, hasBloodPiece, currentPieceNeedRightAlign);
                }
                elements.add(element);
            }
        }
        if (typesettingInfos != null) {
            for (TypesettingInfo info : typesettingInfos) {
                if (info == null) {
                    continue;
                }
                if (StringUtils.isBlank(info.getElement().getFormeSvg())) {
                    throw new IllegalArgumentException("排版信息缺少参与排版的maskSvg：" + info.getTypesettingId());
                }
                NestingRequest.Element element = new NestingRequest.Element();
                element.setId(info.getId());
                element.setImg(info.getElement().getFormeSvg());
                element.setSvg(info.getElement().getFormeSvg());
                element.setCounts(info.getQuantity() != null && info.getQuantity() > 0 ? info.getQuantity() : 1);
                element.setForme(Boolean.TRUE);
                if (isVerticalTypesetting) {
                    element.setVMargin(0);
                    element.setHGravity("left");
                    element.setHMargin(0);
                }
                NestingRequestRuleService nestingRequestRuleService = nestingRequestRuleServiceMap.get(layoutMode);
                if (nestingRequestRuleService != null) {
                    nestingRequestRuleService.applyElementStyle(
                            element, Boolean.TRUE.equals(info.getHaveBlood()), hasBloodPiece, !typesettingInfos.isEmpty());
                }
                applyElementAlignAndSafeDistance(element, hasBloodPiece, Boolean.TRUE.equals(info.getHaveBlood()));
                elements.add(element);
            }
        }

        if (elements.isEmpty()) {
            throw new IllegalArgumentException("生产工件和排版信息均无可用于排版的有效元素");
        }

        List<NestingRequest.Container> containers = new ArrayList<>();
        if (request.getContainers() != null && !request.getContainers().isEmpty()) {
            for (LayoutConfirmRequest.ContainerInfo requestContainer : request.getContainers()) {
                if (requestContainer == null || requestContainer.getWidth() == null || requestContainer.getHeight() == null) {
                    continue;
                }
                NestingRequest.Container container = new NestingRequest.Container();
                container.setWidth(requestContainer.getWidth());
                container.setHeight(requestContainer.getHeight());
                containers.add(container);
            }
        }
        if (containers.isEmpty()) {
            NestingRequest.Container defaultContainer = new NestingRequest.Container();
            defaultContainer.setWidth(1500);
            defaultContainer.setHeight(1000);
            containers.add(defaultContainer);
        }
        NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
        NestingRequestComposeService composeService = nestingRequestComposeServiceMap.get(layoutMode);
        manifest.setSpacing(composeService == null ? layoutMode.getNestingSpacingMm() : composeService.resolveSpacing(layoutMode));
        manifest.setRequirePlt(Boolean.TRUE);
        manifest.setMirrorAppend(Boolean.FALSE);
        manifest.setMirrorRequirePlt(Boolean.FALSE);
        manifest.setContainers(containers);
        manifest.setElements(composeService == null
                ? elements
                : composeService.composeElements(request.getManufacturerMetaId(), cacheKey, elements, containers));
        nestingManifestStrategy.apply(manifest, productionPieces, typesettingInfos);

        CallbackConfig callbackConfig = new CallbackConfig();
        if ("grid_typesetting".equals(layoutMode.getLayoutCategory())
                || "vertical_typesetting".equals(layoutMode.getLayoutCategory())) {
            callbackConfig.setCallbackUrl(generateGridNestedFilesCallbackUrl);
        } else {
            callbackConfig.setCallbackUrl(generateNestedFilesCallbackUrl);
        }
        CallbackCustomValue callbackCustomValue = new CallbackCustomValue();
        callbackCustomValue.setId(cacheKey);
        callbackConfig.setCallbackCustomValue(callbackCustomValue);

        ObjectStorageTempAuthConfig objectStorageTempAuthConfig = aliCloudAuthService.getObjectStorageTempAuthConfig(cacheKey);
        UploadConfig uploadConfig = new UploadConfig();
        uploadConfig.setUploadPath(buildLayoutUploadPath(request.getManufacturerMetaId(), cacheKey));
        uploadConfig.setOssConfig(objectStorageTempAuthConfig);
        //配置callback信息

        NestingRequest nestingRequest = new NestingRequest();
        nestingRequest.setNestManifest(manifest);
        nestingRequest.setUploadConfig(uploadConfig);
        nestingRequest.setCallbackConfig(callbackConfig);
        return nestingRequest;
    }

    /**
     * 解析生产工件本次提交给排版的数量。
     *
     * <p>留白零件在算法请求中按一张已生成的 forme / 印版 SVG 参与排版，
     * 因此无论前端带入多少待排版数量，本次 toLayout 都固定提交 1，
     * 同时回写 request.typesettingCells，保证缓存与后续 typesettingCells 持久化数量一致。</p>
     */
    private Integer resolveProductionPieceLayoutQuantity(ProductionPiece piece, Integer requestedQuantity) {
        if (hasLiubaiProcedure(piece)) {
            return 1;
        }
        return requestedQuantity;
    }

    /**
     * 判断生产工件是否带有“留白xxx”工艺。
     *
     * <p>留白预处理会把矩形 mask 外扩并生成新的 mask SVG；toLayout 组装算法元素时，
     * 这类零件需要按 forme 元素参与排版，确保算法侧按已处理后的外框 SVG 处理。</p>
     */
    private boolean hasLiubaiProcedure(ProductionPiece piece) {
        if (piece == null) {
            return false;
        }
        if (piece.getMarks() != null && piece.getMarks().keySet().stream()
                .filter(Objects::nonNull)
                .anyMatch(key -> key.startsWith("liubai-") || key.contains("留白"))) {
            return true;
        }
        if (StringUtils.isNotBlank(piece.getProcessingFlow()) && piece.getProcessingFlow().contains("留白")) {
            return true;
        }
        ProcedureFlow procedureFlow = piece.getProcedureFlow();
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node == null) {
                continue;
            }
            if (StringUtils.isNotBlank(node.getNodeName()) && node.getNodeName().contains("留白")) {
                return true;
            }
            if (node.getParamConfigs() != null && JSON.toJSONString(node.getParamConfigs()).contains("留白")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasVerticalCut(ProductionPiece piece) {
        if (piece == null || StringUtils.isBlank(piece.getOrderItemId())) {
            return false;
        }
        OrderItem orderItem = orderItemService.findByOrderItemId(piece.getOrderItemId());
        if (orderItem == null || orderItem.getProcedureFlow() == null || orderItem.getProcedureFlow().getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : orderItem.getProcedureFlow().getNodes()) {
            if (node == null || SpliceProcessStrategies.findByNode(node).isEmpty()
                    || node.getParamConfigs() == null || node.getParamConfigs().isEmpty()) {
                continue;
            }
            MTOProductSpecDTO.ProcessParamConfigDTO config = node.getParamConfigs().get(0);
            if (config == null || config.getParam() == null) {
                continue;
            }
            Object param = config.getParam();
            if (param instanceof Map) {
                Object xs = ((Map<?, ?>) param).get("xs");
                if (xs instanceof List && !((List<?>) xs).isEmpty()) {
                    return true;
                }
            } else {
                Object xs = invokeGetter(param, "getXs");
                if (xs instanceof List && !((List<?>) xs).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Object invokeGetter(Object target, String methodName) {
        if (target == null || StringUtils.isBlank(methodName)) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void applyElementAlignAndSafeDistance(NestingRequest.Element element, boolean hasBloodPiece, boolean currentPieceHasBlood) {
        if (!hasBloodPiece) {
            element.setAlign(null);
            element.setSafeDistance(null);
            return;
        }
        if (currentPieceHasBlood) {
            element.setAlign("right");
            element.setSafeDistance(30.00D);
            return;
        }
        element.setAlign(null);
        element.setSafeDistance(null);
    }

    private void validateCellSizeAgainstContainers(LayoutConfirmRequest request,
                                                   List<ProductionPiece> productionPieces,
                                                   List<TypesettingInfo> typesettingInfos,
                                                   TypesettingLayoutMode layoutMode) {
        List<LayoutConfirmRequest.ContainerInfo> configuredContainers = new ArrayList<>();
        if (request.getContainers() != null) {
            for (LayoutConfirmRequest.ContainerInfo containerInfo : request.getContainers()) {
                if (containerInfo == null || containerInfo.getWidth() == null || containerInfo.getHeight() == null) {
                    continue;
                }
                configuredContainers.add(containerInfo);
            }
        }

        if (configuredContainers.isEmpty()) {
            configuredContainers.add(new LayoutConfirmRequest.ContainerInfo(1500, 1000));
        }

        List<LayoutConfirmRequest.ContainerInfo> availableContainers = configuredContainers.stream()
                .filter(containerInfo -> canContainerFitAllCells(containerInfo, productionPieces, typesettingInfos))
                .collect(Collectors.toList());
        if (availableContainers.isEmpty()) {
            String oversizedCellId = resolveFirstOversizedCellId(configuredContainers, productionPieces, typesettingInfos);
            throw new IllegalArgumentException(oversizedCellId + "零件的尺寸大于所选规格，不能排版");
        }
        if (request.getContainers() != null) {
            request.setContainers(availableContainers);
        }

        NestingRequestRuleService nestingRequestRuleService = nestingRequestRuleServiceMap.get(layoutMode);
        if (nestingRequestRuleService != null) {
            nestingRequestRuleService.validateBeforeBuild(request, productionPieces, typesettingInfos);
        }
    }

    private boolean canContainerFitAllCells(LayoutConfirmRequest.ContainerInfo containerInfo,
                                            List<ProductionPiece> productionPieces,
                                            List<TypesettingInfo> typesettingInfos) {
        double containerWidth = containerInfo.getWidth().doubleValue();
        double containerHeight = containerInfo.getHeight().doubleValue();
        double containerShortSide = Math.min(containerWidth, containerHeight);
        double containerLongSide = Math.max(containerWidth, containerHeight);
        return canAllProductionPiecesFitContainer(productionPieces, containerShortSide, containerLongSide)
                && canAllTypesettingInfosFitContainer(typesettingInfos, containerShortSide, containerLongSide);
    }

    private boolean canAllProductionPiecesFitContainer(List<ProductionPiece> productionPieces,
                                                       double containerShortSide,
                                                       double containerLongSide) {
        if (productionPieces == null) {
            return true;
        }
        for (ProductionPiece piece : productionPieces) {
            if (piece == null || piece.getWidth() == null || piece.getHeight() == null) {
                continue;
            }
            double pieceShortSideMm = Math.min(piece.getWidth(), piece.getHeight());
            double pieceLongSideMm = Math.max(piece.getWidth(), piece.getHeight());
            if (pieceShortSideMm > containerShortSide || pieceLongSideMm > containerLongSide) {
                return false;
            }
        }
        return true;
    }

    private boolean canAllTypesettingInfosFitContainer(List<TypesettingInfo> typesettingInfos,
                                                       double containerShortSide,
                                                       double containerLongSide) {
        if (typesettingInfos == null) {
            return true;
        }
        for (TypesettingInfo info : typesettingInfos) {
            if (info == null || info.getElement() == null
                    || info.getElement().getWidth() == null || info.getElement().getHeight() == null) {
                continue;
            }
            double typesettingShortSideMm = info.getElement().getWidth().min(info.getElement().getHeight()).doubleValue();
            double typesettingLongSideMm = info.getElement().getWidth().max(info.getElement().getHeight()).doubleValue();
            if (typesettingShortSideMm > containerShortSide || typesettingLongSideMm > containerLongSide) {
                return false;
            }
        }
        return true;
    }

    private String resolveFirstOversizedCellId(List<LayoutConfirmRequest.ContainerInfo> containers,
                                               List<ProductionPiece> productionPieces,
                                               List<TypesettingInfo> typesettingInfos) {
        if (productionPieces != null) {
            for (ProductionPiece piece : productionPieces) {
                if (piece == null || piece.getWidth() == null || piece.getHeight() == null) {
                    continue;
                }
                if (containers.stream().noneMatch(container -> canProductionPieceFitContainer(piece, container))) {
                    return StringUtils.isNotBlank(piece.getProductionPieceId()) ? piece.getProductionPieceId() : piece.getId();
                }
            }
        }
        if (typesettingInfos != null) {
            for (TypesettingInfo info : typesettingInfos) {
                if (info == null || info.getElement() == null
                        || info.getElement().getWidth() == null || info.getElement().getHeight() == null) {
                    continue;
                }
                if (containers.stream().noneMatch(container -> canTypesettingInfoFitContainer(info, container))) {
                    return StringUtils.isNotBlank(info.getTypesettingId()) ? info.getTypesettingId() : info.getId();
                }
            }
        }
        return "";
    }

    private boolean canProductionPieceFitContainer(ProductionPiece piece, LayoutConfirmRequest.ContainerInfo container) {
        double containerShortSide = Math.min(container.getWidth().doubleValue(), container.getHeight().doubleValue());
        double containerLongSide = Math.max(container.getWidth().doubleValue(), container.getHeight().doubleValue());
        double pieceShortSideMm = Math.min(piece.getWidth(), piece.getHeight());
        double pieceLongSideMm = Math.max(piece.getWidth(), piece.getHeight());
        return pieceShortSideMm <= containerShortSide && pieceLongSideMm <= containerLongSide;
    }

    private boolean canTypesettingInfoFitContainer(TypesettingInfo info, LayoutConfirmRequest.ContainerInfo container) {
        double containerShortSide = Math.min(container.getWidth().doubleValue(), container.getHeight().doubleValue());
        double containerLongSide = Math.max(container.getWidth().doubleValue(), container.getHeight().doubleValue());
        double typesettingShortSideMm = info.getElement().getWidth().min(info.getElement().getHeight()).doubleValue();
        double typesettingLongSideMm = info.getElement().getWidth().max(info.getElement().getHeight()).doubleValue();
        return typesettingShortSideMm <= containerShortSide && typesettingLongSideMm <= containerLongSide;
    }


    /**
     * 判断当前零件是否为拼接出血分片。
     *
     * <p>分片序号只用于识别拼接位置，是否出血仍以 blood 坐标为准，避免 blood.x / blood.y
     * 均为 0 的后续分片被误当作出血零件。</p>
     */
    private boolean isSpliceBleedPiece(ProductionPiece piece) {
        if (piece == null || piece.getSeq() == null || StringUtils.isBlank(piece.getGroup()) || !hasSupportedSpliceNode(piece)) {
            return false;
        }
        return piece.getSeq() > 1 && isBloodPieceByCoordinates(piece);
    }

    private boolean hasSupportedSpliceNode(ProductionPiece piece) {
        if (piece == null) {
            return false;
        }
        if (SpliceProcessStrategies.hasSpliceNode(piece.getProcedureFlow())) {
            return true;
        }
        if (StringUtils.isBlank(piece.getOrderItemId())) {
            return false;
        }
        OrderItem orderItem = orderItemService.findByOrderItemId(piece.getOrderItemId());
        return orderItem != null && SpliceProcessStrategies.hasSpliceNode(orderItem.getProcedureFlow());
    }

    private boolean isBloodBasedRotationCandidate(ProductionPiece piece) {
        if (piece == null || piece.getBlood() == null) {
            return false;
        }
        if (piece.getGroup() == null || piece.getSeq() == null) {
            return false;
        }
        Integer bloodX = piece.getBlood().getX();
        Integer bloodY = piece.getBlood().getY();
        return bloodX != null && bloodY != null && bloodX == 0 && bloodY != 0;
    }

    /**
     * 是否为血位件：只要 blood.x / blood.y 至少一个非 0 即判定为血位件。
     * 说明：
     * - blood.x == 0 && blood.y == 0 不是血位件；
     * - blood.x == 0 && blood.y != 0 仅表示“需要旋转”的血位件子集。
     */
    private boolean isBloodPieceByCoordinates(ProductionPiece piece) {
        if (piece == null || piece.getBlood() == null) {
            return false;
        }
        Integer bloodX = piece.getBlood().getX();
        Integer bloodY = piece.getBlood().getY();
        if (bloodX == null || bloodY == null) {
            return false;
        }
        return bloodX != 0 || bloodY != 0;
    }

    /**
     * 确认打印：将排版数据根据状态机改为待打印状态
     *
     * @param request 确认打印请求（排版ID、可选layoutMode、deviceCode）
     * @return 操作结果
     */
    public ConfirmPrintResult confirmPrint(ConfirmPrintRequest request) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            throw new RuntimeException("排版ID不能为空");
        }
        List<String> operationLockKeys = Collections.singletonList(buildConfirmPrintOperationLockKey(request.getId()));
        String operationLockToken = acquireOperationLocks(operationLockKeys, "排版记录正在确认打印中，请勿重复确认");
        try {
            return doConfirmPrint(request);
        } finally {
            releaseOperationLocks(operationLockKeys, operationLockToken);
        }
    }

    private ConfirmPrintResult doConfirmPrint(ConfirmPrintRequest request) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            throw new RuntimeException("排版ID不能为空");
        }
        if (StringUtils.isBlank(request.getDeviceCode())) {
            throw new RuntimeException("设备编号不能为空");
        }
        TypesettingInfo typesettingInfo = domainTypesettingService.findById(request.getId());
        if (typesettingInfo == null) {
            throw new RuntimeException("排版信息不存在：" + request.getId());
        }
        try {
            validateConfirmPrintPreconditions(typesettingInfo);
            if (typesettingInfo.getElement() == null || StringUtils.isBlank(typesettingInfo.getElement().getNestedSvg())) {
                throw new TypesettingPreAlgorithmValidationException("排版信息缺少 nestedSvg，无法确认打印");
            }

            TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(
                    StringUtils.isNotBlank(request.getLayoutMode()) ? request.getLayoutMode() : typesettingInfo.getLayoutMode()
            );
            if (requireManufacturerMetaId(layoutMode) && StringUtils.isBlank(typesettingInfo.getManufacturerMetaId())) {
                throw new TypesettingPreAlgorithmValidationException("plt二维码排版缺少 manufacturerMetaId，无法生成队列编号与二维码");
            }
            typesettingInfo.setLayoutMode(layoutMode.getCode());
            typesettingInfo.applyLayoutModeConfig();

            String businessId = resolveFormeBusinessId(typesettingInfo, layoutMode);
            FormeGenerationRequest formeRequest = buildFormeGenerationRequest(typesettingInfo, layoutMode, businessId);
            mergeAnchorPointMarks(typesettingInfo, formeRequest);
            String formeOpRemark = "FORME_OP:PRINT:" + request.getDeviceCode();
            TypesettingInfo mirrorTypesettingInfo = resolveMirrorTypesettingInfo(typesettingInfo);
            if (mirrorTypesettingInfo != null) {
                if (mirrorTypesettingInfo.getElement() != null && StringUtils.isNotBlank(mirrorTypesettingInfo.getElement().getNestedMirrorSvg())) {
                    mirrorTypesettingInfo.getElement().setNestedSvg(mirrorTypesettingInfo.getElement().getNestedMirrorSvg());
                }
                mirrorTypesettingInfo.setRemark(formeOpRemark);
                mirrorTypesettingInfo.setDeviceCode(request.getDeviceCode());
                mirrorTypesettingInfo.setStatus(TypesettingStatus.CONFIRMED.getCode());
                ManufacturerDeviceCfg mirrorDeviceCfg = findDeviceCfgByDeviceCode(typesettingInfo.getManufacturerMetaId(), request.getDeviceCode());
                mirrorTypesettingInfo.setDeviceName(mirrorDeviceCfg.getDeviceName());
                ensureMirrorTypesettingExists(mirrorTypesettingInfo);
                FormeGenerationRequest mirrorFormeRequest = buildFormeGenerationRequest(
                        mirrorTypesettingInfo,
                        TypesettingLayoutMode.DOUBLE_SIDE_MOUNTING_LAYOUT,
                        resolveMirrorFormeBusinessId(mirrorTypesettingInfo, businessId)
                );
                mergeAnchorPointMarks(mirrorTypesettingInfo, mirrorFormeRequest);
                // 镜像印版由 DoubleSideMountingLayoutBuildService 回填了 marks，这里同步落库
                mergeExistingMarksBeforeUpdate(mirrorTypesettingInfo);
                domainTypesettingService.updateTypesetting(mirrorTypesettingInfo);
                String mirrorFormeRequestJson = JSON.toJSONString(mirrorFormeRequest);
                log.info("formeRequest-print-mirror========:{}", mirrorFormeRequestJson);
                algorithmCoreApiService.generateFormeAsync(mirrorFormeRequestJson, mirrorFormeRequest.getCallbackConfig().getCallbackUrl());
            }

            ManufacturerDeviceCfg deviceCfg = findDeviceCfgByDeviceCode(typesettingInfo.getManufacturerMetaId(), request.getDeviceCode());
            typesettingInfo.setStatus(TypesettingStatus.CONFIRMED.getCode());
            typesettingInfo.setRemark(formeOpRemark);
            typesettingInfo.setDeviceCode(request.getDeviceCode());
            typesettingInfo.setDeviceName(deviceCfg.getDeviceName());
            mergeExistingMarksBeforeUpdate(typesettingInfo);
            domainTypesettingService.updateTypesetting(typesettingInfo);

            String formeRequestJson = JSON.toJSONString(formeRequest);
            log.info("formeRequest-print========:{}", formeRequestJson);
            algorithmCoreApiService.generateFormeAsync(formeRequestJson, formeRequest.getCallbackConfig().getCallbackUrl());

            ConfirmPrintResult result = new ConfirmPrintResult();
            result.setSuccess(true);
            result.setMessage("确认打印任务已提交，等待回调");
            return result;
        } catch (TypesettingPreAlgorithmValidationException e) {
            ConfirmPrintResult result = new ConfirmPrintResult();
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        } catch (Exception e) {
            String failureReason = "确认打印处理失败：" + resolveExceptionMessage(e);
            log.error(failureReason, e);
            markTypesettingFailed(typesettingInfo, failureReason);
            ConfirmPrintResult result = new ConfirmPrintResult();
            result.setSuccess(false);
            result.setMessage(failureReason);
            return result;
        }
    }

    private void validateConfirmPrintPreconditions(TypesettingInfo typesettingInfo) {
        try {
            ensureTypesettingStatus(typesettingInfo, TypesettingStatus.CONFIRMING, "只有待确认的排版记录才能确认打印");
            validateConfirmPrintForDoubleSideMirror(typesettingInfo);
        } catch (RuntimeException e) {
            throw new TypesettingPreAlgorithmValidationException(e.getMessage(), e);
        }
    }

    private void validateConfirmPrintForDoubleSideMirror(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null) {
            return;
        }
        boolean hasDoubleSideMirrorProcedure = ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(typesettingInfo.getProcedureFlow())
                || containsDoubleSideMirrorKeyword(typesettingInfo.getProcessingFlow());
        if (!hasDoubleSideMirrorProcedure || !isAllPartCompositionTypesetting(typesettingInfo)) {
            return;
        }
        throw new RuntimeException("该印版存在反面文件，需要先确认，不能直接下发");
    }

    private boolean containsDoubleSideMirrorKeyword(String processingFlow) {
        return StringUtils.isNotBlank(processingFlow)
                && ProcedureFlowNodeMatcher.containsDoubleSideMountingKeyword(processingFlow);
    }

    private boolean isAllPartCompositionTypesetting(TypesettingInfo typesettingInfo) {
        if (CollectionUtils.isEmpty(typesettingInfo.getTypesettingCells())) {
            return true;
        }
        return typesettingInfo.getTypesettingCells().stream()
                .filter(Objects::nonNull)
                .noneMatch(cell -> TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType()));
    }

    public LayoutConfirmResult batchConfirmLayout(BatchConfirmLayoutRequest request) {
        if (request == null || request.getTypesettingInfos() == null || request.getTypesettingInfos().isEmpty()) {
            throw new IllegalArgumentException("批量确认排版参数不能为空");
        }
        for (TypesettingInfo info : request.getTypesettingInfos()) {
            confirmLayout(info);
        }
        LayoutConfirmResult ok = new LayoutConfirmResult();
        ok.setSuccess(true);
        ok.setMessage("批量确认排版任务已提交");
        return ok;
    }

    public ConfirmPrintResult batchConfirmPrint(BatchConfirmPrintRequest request) {
        if (request == null || request.getRequests() == null || request.getRequests().isEmpty()) {
            throw new RuntimeException("批量确认打印参数不能为空");
        }
        for (ConfirmPrintRequest item : request.getRequests()) {
            confirmPrint(item);
        }
        ConfirmPrintResult result = new ConfirmPrintResult();
        result.setSuccess(true);
        result.setMessage("批量确认打印任务已提交");
        return result;
    }

    public void handleGenerateFormeCallback(FormeGenerationResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("印版生成回调参数不能为空");
        }
        String recordId = null;
        if (response.getCallbackConfig() != null && response.getCallbackConfig().getCallbackCustomValue() != null) {
            recordId = response.getCallbackConfig().getCallbackCustomValue().getId();
        }
        if (StringUtils.isBlank(recordId)) {
            recordId = response.getId();
        }
        if (StringUtils.isBlank(recordId)) {
            throw new IllegalArgumentException("印版生成回调缺少排版记录ID");
        }
        List<String> callbackLockKeys = Collections.singletonList(
                TYPESETTING_OPERATION_LOCK_PREFIX + "formeCallback:" + recordId);
        String callbackLockToken = acquireOperationLocks(callbackLockKeys, "印版生成回调正在处理中，请稍后重试");
        try {
            doHandleGenerateFormeCallback(response, recordId);
        } finally {
            releaseOperationLocks(callbackLockKeys, callbackLockToken);
        }
    }

    private void doHandleGenerateFormeCallback(FormeGenerationResponse response, String recordId) {
        TypesettingInfo typesettingInfo = domainTypesettingService.findById(recordId);
        if (typesettingInfo == null) {
            throw new IllegalArgumentException("印版生成回调对应的排版记录不存在：" + recordId);
        }
        try {
            if (!"success".equalsIgnoreCase(response.getStatus())) {
                markTypesettingFailed(typesettingInfo,
                        StringUtils.isNotBlank(response.getError()) ? response.getError() : "印版异步生成失败");
                return;
            }
            applyFormeGenerationResult(typesettingInfo, response.getResult());
            if (StringUtils.isBlank(typesettingInfo.getTemplateCode())) {
                typesettingInfo.setTemplateCode(buildTemplateCode(1, 1));
            }
            String remark = typesettingInfo.getRemark() == null ? "" : typesettingInfo.getRemark().trim();
            if ("FORME_OP:LAYOUT".equals(remark)) {
                typesettingInfo.setStatus(TypesettingStatus.PENDING.getCode());
                typesettingInfo.setRemark(null);
                mergeExistingMarksBeforeUpdate(typesettingInfo);
                domainTypesettingService.updateTypesetting(typesettingInfo);
                return;
            }
            log.info("开始进行打印印版回调参数remark,{}", JsonLogUtil.toJSONString(remark));
            if (remark != null && remark.startsWith("FORME_OP:PRINT:")) {
                log.info("开始进行打印印版回调参数,{}", buildTypesettingInfoLogSummary(typesettingInfo));
                String deviceCode = remark.substring("FORME_OP:PRINT:".length());
                boolean repeatedPrintCallback = TypesettingStatus.PRINTING.getCode().equals(typesettingInfo.getStatus());
                typesettingInfo.setStatus(TypesettingStatus.PRINTING.getCode());
                typesettingInfo.setDeviceCode(deviceCode);
                typesettingInfo.setLeaveQuantity(1);
                boolean skipQuantityTransferForMirror = isMirrorTypesettingInfo(typesettingInfo);
                Set<String> visitedTypesettingKeys = new HashSet<>();
                Map<String, Integer> productionPieceUsage = new LinkedHashMap<>();
                collectProductionPieceUsageForQuantityTransfer(typesettingInfo, 1, visitedTypesettingKeys, productionPieceUsage);
                int plateUseCount = typesettingInfo.getLeaveQuantity() != null && typesettingInfo.getLeaveQuantity() > 0
                        ? typesettingInfo.getLeaveQuantity() : 1;
                String callbackTypesettingId = StringUtils.isNotBlank(typesettingInfo.getTypesettingId())
                        ? typesettingInfo.getTypesettingId() : typesettingInfo.getId();
                if (!repeatedPrintCallback && !skipQuantityTransferForMirror) {
                    transferTypesettingQuantityToPrinting(productionPieceUsage, plateUseCount);
                }
                Set<String> productionPieceIds = collectProductionPieceIdsForDownload(typesettingInfo);
                productionPieceIds.addAll(productionPieceUsage.keySet());
                String printTaskTypesettingId = callbackTypesettingId;
                String printTaskTypesettingCode = typesettingInfo.getId();
                String deviceInfoId = resolveDeviceInfoIdByDeviceCode(typesettingInfo.getManufacturerMetaId(), deviceCode);
                Map<String, String> allMarks = collectTypesettingMarks(typesettingInfo);
                TypesettingDownloadTaskData downloadTaskData = buildDownloadTaskData(
                        printTaskTypesettingId,
                        deviceInfoId,
                        deviceCode,
                        typesettingInfo.getElement(),
                        allMarks,
                        productionPieceIds,
                        typesettingInfo);
                typesettingInfo.setRemark(null);
                mergeExistingMarksBeforeUpdate(typesettingInfo);
                domainTypesettingService.updateTypesetting(typesettingInfo);
                TypesettingDownloadTaskData nonPltData = copyDownloadTaskDataWithoutPlts(downloadTaskData);
                savePrintTaskByDeviceCode(printTaskTypesettingId, printTaskTypesettingCode, typesettingInfo.getManufacturerMetaId(), deviceCode, nonPltData);
                savePltBroadcastPrintTask(printTaskTypesettingId, printTaskTypesettingCode, typesettingInfo.getManufacturerMetaId(), downloadTaskData, typesettingInfo);
                return;
            }
            throw new IllegalStateException("印版生成回调无法识别排版操作类型，recordId=" + recordId
                    + "，status=" + typesettingInfo.getStatus()
                    + "，remark=" + typesettingInfo.getRemark());
        }catch (Exception e) {
            log.error("处理印版生成回调异常", e);
            markTypesettingFailed(typesettingInfo, "印版生成回调处理异常：" + resolveExceptionMessage(e));
            throw new IllegalStateException("处理印版生成回调异常：" + resolveExceptionMessage(e), e);
        }

    }

    private String buildTypesettingInfoLogSummary(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null) {
            return "null";
        }
        return "id=" + typesettingInfo.getId()
                + ", typesettingId=" + typesettingInfo.getTypesettingId()
                + ", status=" + typesettingInfo.getStatus()
                + ", remark=" + typesettingInfo.getRemark()
                + ", manufacturerMetaId=" + typesettingInfo.getManufacturerMetaId()
                + ", layoutMode=" + typesettingInfo.getLayoutMode()
                + ", templateCode=" + typesettingInfo.getTemplateCode()
                + ", cells=" + (typesettingInfo.getTypesettingCells() == null ? 0 : typesettingInfo.getTypesettingCells().size())
                + ", procedureNodes=" + (typesettingInfo.getProcedureFlow() == null
                || typesettingInfo.getProcedureFlow().getNodes() == null ? 0 : typesettingInfo.getProcedureFlow().getNodes().size())
                + ", marks=" + (typesettingInfo.getMarks() == null ? 0 : typesettingInfo.getMarks().size());
    }

    private void markTypesettingFailed(TypesettingInfo typesettingInfo, String reason) {
        if (typesettingInfo == null) {
            return;
        }
        typesettingInfo.setStatus(TypesettingStatus.FAILED.getCode());
        typesettingInfo.setRemark(StringUtils.isNotBlank(reason) ? reason : "印版处理失败");
        mergeExistingMarksBeforeUpdate(typesettingInfo);
        domainTypesettingService.updateTypesetting(typesettingInfo);
    }

    private void markTypesettingsFailed(Collection<TypesettingInfo> typesettingInfos, String reason) {
        if (typesettingInfos == null || typesettingInfos.isEmpty()) {
            return;
        }
        domainTypesettingService.batchUpdateCallbackFailure(typesettingInfos, reason);
    }

    private String resolveExceptionMessage(Exception e) {
        if (e == null) {
            return "未知错误";
        }
        return StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
    }

    private static class TypesettingPreAlgorithmValidationException extends RuntimeException {
        private TypesettingPreAlgorithmValidationException(String message) {
            super(message);
        }

        private TypesettingPreAlgorithmValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private boolean requireManufacturerMetaId(TypesettingLayoutMode layoutMode) {
        return TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CIRCLE == layoutMode
                || TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CROSS == layoutMode
                || TypesettingLayoutMode.RECT_TYPESETTING_PLT_QR_CIRCLE == layoutMode
                || TypesettingLayoutMode.RECT_TYPESETTING_PLT_QR_CROSS == layoutMode
                || TypesettingLayoutMode.GRID_TYPESETTING_PLT_QR_CIRCLE == layoutMode
                || TypesettingLayoutMode.GRID_TYPESETTING_PLT_QR_CROSS == layoutMode;
    }

    private String resolveDeviceInfoIdByDeviceCode(String manufacturerMetaId, String deviceCode) {
        ManufacturerDeviceCfg deviceCfg = findDeviceCfgByDeviceCode(manufacturerMetaId, deviceCode);
        if (StringUtils.isBlank(deviceCfg.getDeviceInfoId())) {
            throw new RuntimeException("设备编号未绑定设备信息：" + deviceCode);
        }
        return deviceCfg.getDeviceInfoId();
    }

    private ManufacturerDeviceCfg findDeviceCfgByDeviceCode(String manufacturerMetaId, String deviceCode) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("deviceCode", deviceCode);
        if (StringUtils.isNotBlank(manufacturerMetaId)) {
            filters.put("manufacturerMetaId", manufacturerMetaId);
        }
        List<ManufacturerDeviceCfg> deviceCfgs = manufacturerDeviceCfgRepository.filterList(1, 1, filters);
        if (deviceCfgs == null || deviceCfgs.isEmpty()) {
            throw new RuntimeException("设备编号不存在：" + deviceCode);
        }
        return deviceCfgs.get(0);
    }


    private void collectProductionPieceUsageForQuantityTransfer(TypesettingInfo typesettingInfo,
                                                                 int multiplier,
                                                                 Set<String> visitedTypesettingKeys,
                                                                 Map<String, Integer> productionPieceUsage) {
        collectProductionPieceUsage(typesettingInfo, multiplier, visitedTypesettingKeys, productionPieceUsage, isMirrorTypesettingInfo(typesettingInfo));
    }

    private void collectProductionPieceUsage(TypesettingInfo typesettingInfo,
                                             int multiplier,
                                             Set<String> visitedTypesettingKeys,
                                             Map<String, Integer> productionPieceUsage) {
        collectProductionPieceUsage(typesettingInfo, multiplier, visitedTypesettingKeys, productionPieceUsage, false);
    }


    private boolean isMirrorTypesettingInfo(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null) {
            return false;
        }
        return isMirrorTypesettingId(typesettingInfo.getTypesettingId()) || isMirrorTypesettingId(typesettingInfo.getId());
    }

    private boolean isMirrorTypesettingId(String typesettingId) {
        return StringUtils.isNotBlank(typesettingId) && typesettingId.endsWith("-Mirror");
    }

    private void collectProductionPieceUsage(TypesettingInfo typesettingInfo,
                                             int multiplier,
                                             Set<String> visitedTypesettingKeys,
                                             Map<String, Integer> productionPieceUsage,
                                             boolean mirrorBranch) {
        if (typesettingInfo == null || StringUtils.isBlank(typesettingInfo.getId())) {
            return;
        }
        String currentKey = "id:" + typesettingInfo.getId();
        if (visitedTypesettingKeys.contains(currentKey)) {
            return;
        }
        visitedTypesettingKeys.add(currentKey);
        if (typesettingInfo.getTypesettingCells() == null) {
            visitedTypesettingKeys.remove(currentKey);
            return;
        }
        for (TypesettingSourceCell cell : typesettingInfo.getTypesettingCells()) {
            if (cell == null || StringUtils.isBlank(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            int cellQuantity = cell.getQuantity() != null && cell.getQuantity() > 0 ? cell.getQuantity() : 1;
            int currentMultiplier = multiplier * cellQuantity;
            if (TypesettingSourceType.PART.getCode().equals(cell.getSourceType())) {
                if (!mirrorBranch) {
                    productionPieceUsage.merge(cell.getSourceId(), currentMultiplier, Integer::sum);
                }
                continue;
            }
            if (!TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                continue;
            }
            TypesettingInfo nestedInfo = domainTypesettingService.findById(cell.getSourceId());
            if (nestedInfo == null || StringUtils.isBlank(nestedInfo.getId())) {
                continue;
            }
            boolean nestedMirrorBranch = mirrorBranch || isMirrorTypesettingInfo(nestedInfo);
            collectProductionPieceUsage(nestedInfo, currentMultiplier, visitedTypesettingKeys, productionPieceUsage, nestedMirrorBranch);

            // 需要查看对应 -Mirror 印版，但其 productionPiece 用量不参与扣减统计
            if (!isMirrorTypesettingInfo(nestedInfo)) {
                TypesettingInfo mirrorNestedInfo = findMirrorTypesettingInfo(nestedInfo);
                if (mirrorNestedInfo != null && StringUtils.isNotBlank(mirrorNestedInfo.getId())) {
                    collectProductionPieceUsage(mirrorNestedInfo, currentMultiplier, visitedTypesettingKeys, productionPieceUsage, true);
                }
            }
        }
        visitedTypesettingKeys.remove(currentKey);
    }

    private Set<String> collectProductionPieceIdsForDownload(TypesettingInfo typesettingInfo) {
        LinkedHashSet<String> productionPieceIds = new LinkedHashSet<>();
        collectProductionPieceIdsForDownloadRecursive(typesettingInfo, productionPieceIds, new HashSet<>());
        return productionPieceIds;
    }

    private void collectProductionPieceIdsForDownloadRecursive(TypesettingInfo typesettingInfo,
                                                                Set<String> productionPieceIds,
                                                                Set<String> visitedTypesettingKeys) {
        if (typesettingInfo == null || StringUtils.isBlank(typesettingInfo.getId())) {
            return;
        }
        String currentKey = "id:" + typesettingInfo.getId();
        if (!visitedTypesettingKeys.add(currentKey)) {
            return;
        }
        try {
            if (typesettingInfo.getTypesettingCells() == null) {
                return;
            }
            for (TypesettingSourceCell cell : typesettingInfo.getTypesettingCells()) {
                if (cell == null || StringUtils.isBlank(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                    continue;
                }
                if (TypesettingSourceType.PART.getCode().equals(cell.getSourceType())) {
                    productionPieceIds.add(cell.getSourceId());
                    continue;
                }
                if (!TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                    continue;
                }
                TypesettingInfo nestedInfo = domainTypesettingService.findById(cell.getSourceId());
                collectProductionPieceIdsForDownloadRecursive(nestedInfo, productionPieceIds, visitedTypesettingKeys);
            }
        } finally {
            visitedTypesettingKeys.remove(currentKey);
        }
    }

    private void transferTypesettingQuantityToPrinting(Map<String, Integer> productionPieceUsage, int plateUseCount) {
        if (productionPieceUsage == null || productionPieceUsage.isEmpty() || plateUseCount <= 0) {
            return;
        }
        Map<String, ProductionPiece> piecesById = productionPieceService.findByIds(productionPieceUsage.keySet());
        Map<String, Integer> requiredQuantities = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : productionPieceUsage.entrySet()) {
            String productionPieceRecordId = entry.getKey();
            int requiredQuantity = entry.getValue() * plateUseCount;
            if (requiredQuantity <= 0) {
                continue;
            }
            ProductionPiece piece = piecesById.get(productionPieceRecordId);
            if (piece == null || piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
                continue;
            }
            ProcedureFlowNode typesettingNode = null;
            ProcedureFlowNode printingNode = null;
            for (ProcedureFlowNode node : piece.getProcedureFlow().getNodes()) {
                if (node == null || StringUtils.isBlank(node.getNodeName())) {
                    continue;
                }
                if ("排版中".equals(node.getNodeName())) {
                    typesettingNode = node;
                } else if ("打印中".equals(node.getNodeName())) {
                    printingNode = node;
                }
            }
            if (typesettingNode == null || printingNode == null) {
                continue;
            }
            int typesettingQuantity = typesettingNode.getPieceQuantity() == null ? 0 : typesettingNode.getPieceQuantity();
            if (typesettingQuantity < requiredQuantity) {
                throw new RuntimeException("零件 " + productionPieceRecordId + " 的“排版中”数量不足，需求="
                        + requiredQuantity + "，当前=" + typesettingQuantity);
            }
            requiredQuantities.put(productionPieceRecordId, requiredQuantity);
        }
        long transferred = productionPieceService.transferTypesettingQuantitiesToPrinting(requiredQuantities);
        if (transferred != requiredQuantities.size()) {
            throw new RuntimeException("生产工件数量并发变更，期望转移=" + requiredQuantities.size() + "，实际转移=" + transferred);
        }
    }

    private Map<String, String> collectTypesettingMarks(TypesettingInfo rootTypesettingInfo) {
        LinkedHashMap<String, String> marks = new LinkedHashMap<>();
        collectTypesettingMarksRecursive(rootTypesettingInfo, marks, new HashSet<>());
        return marks;
    }

    private void collectTypesettingMarksRecursive(TypesettingInfo typesettingInfo,
                                                  LinkedHashMap<String, String> marks,
                                                  Set<String> visitedIds) {
        if (typesettingInfo == null || StringUtils.isBlank(typesettingInfo.getId()) || visitedIds.contains(typesettingInfo.getId())) {
            return;
        }
        visitedIds.add(typesettingInfo.getId());
        if (typesettingInfo.getMarks() != null && !typesettingInfo.getMarks().isEmpty()) {
            for (Map.Entry<String, String> entry : typesettingInfo.getMarks().entrySet()) {
                if (StringUtils.isNotBlank(entry.getValue())) {
                    marks.put(typesettingInfo.getId() + ":" + entry.getKey(), entry.getValue());
                }
            }
        }
        if (typesettingInfo.getTypesettingCells() == null) {
            return;
        }
        for (TypesettingSourceCell cell : typesettingInfo.getTypesettingCells()) {
            if (cell == null || !TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            TypesettingInfo childTypesetting = domainTypesettingService.findById(cell.getSourceId());
            if (childTypesetting != null) {
                collectTypesettingMarksRecursive(childTypesetting, marks, visitedIds);
            }
        }
    }


    private TypesettingDownloadTaskData buildDownloadTaskData(String typesettingInfoId,
                                                              String deviceInfoId,
                                                              String deviceCode,
                                                              TypesettingElement typesettingElement,
                                                              Map<String, String> marks,
                                                              Set<String> productionPieceIds,
                                                              TypesettingInfo rootTypesettingInfo) {
        LinkedHashSet<String> imageSet = new LinkedHashSet<>();
        List<ProductionPiece> sourcePieces = new ArrayList<>();
        for (String productionPieceId : productionPieceIds) {
            ProductionPiece piece = productionPieceService.findById(productionPieceId);
            if (piece == null) {
                continue;
            }
            sourcePieces.add(piece);
            String baseImage = resolveProductionPieceImageForDownload(piece);
            appendRawFile(imageSet, baseImage);
            appendMirrorConfigImages(imageSet, piece);
        }
        LinkedHashSet<String> pltSet = new LinkedHashSet<>();
        LinkedHashSet<String> jsonSet = new LinkedHashSet<>();
        LinkedHashSet<String> markSet = new LinkedHashSet<>();
        if (typesettingElement != null) {
            appendRawFile(jsonSet, typesettingElement.getJson());
            appendFormeSvgImgFiles(imageSet, typesettingElement.getFormeSvg());
        }
        collectRequiredPltsRecursive(rootTypesettingInfo, pltSet, new HashSet<>());
        if (marks != null && !marks.isEmpty()) {
            for (String markFile : marks.values()) {
                appendMarkFiles(markSet, markFile);
            }
        }
        // 不能只依赖排版记录上的汇总 marks：历史排版记录或回调前创建的记录可能尚未回填该字段。
        // 打印任务以实际参与排版的零件为准，直接补齐零件自身的 mark 资源，保证下载端拿到完整文件集。
        for (ProductionPiece sourcePiece : sourcePieces) {
            if (sourcePiece.getMarks() == null || sourcePiece.getMarks().isEmpty()) {
                continue;
            }
            for (String markFile : sourcePiece.getMarks().values()) {
                appendMarkFiles(markSet, markFile);
            }
        }
        TypesettingDownloadTaskData data = new TypesettingDownloadTaskData();
        data.setId(typesettingInfoId);
        data.setDeviceInfoId(deviceInfoId);
        data.setDeviceInfoIds(Collections.singletonList(deviceInfoId));
        data.setDeviceCodes(Collections.singletonList(deviceCode));
        data.setImamges(new ArrayList<>(imageSet));
        data.setPlts(new ArrayList<>(pltSet));
        data.setJsons(new ArrayList<>(jsonSet));
        data.setMarks(new ArrayList<>(markSet));
        return data;
    }


    private void collectRequiredPltsRecursive(TypesettingInfo typesettingInfo,
                                              Set<String> pltSet,
                                              Set<String> visitedIds) {
        if (typesettingInfo == null) {
            return;
        }
        String visitedKey = resolveTypesettingVisitedKey(typesettingInfo);
        if (StringUtils.isNotBlank(visitedKey) && !visitedIds.add(visitedKey)) {
            return;
        }
        if (isRequirePltFile(typesettingInfo)) {
            TypesettingElement element = typesettingInfo.getElement();
            if (element != null && element.getPlt() != null) {
                appendRawFile(pltSet, element.getPlt().getNormal());
                appendRawFile(pltSet, element.getPlt().getReverse());
            }
        }
        if (typesettingInfo.getTypesettingCells() == null || typesettingInfo.getTypesettingCells().isEmpty()) {
            return;
        }
        for (TypesettingSourceCell cell : typesettingInfo.getTypesettingCells()) {
            if (cell == null || !TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            if (visitedIds.contains(cell.getSourceId())) {
                continue;
            }
            TypesettingInfo cellTypesetting = domainTypesettingService == null ? null : domainTypesettingService.findById(cell.getSourceId());
            collectRequiredPltsRecursive(cellTypesetting, pltSet, visitedIds);
        }
    }

    private String resolveTypesettingVisitedKey(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null) {
            return null;
        }
        if (StringUtils.isNotBlank(typesettingInfo.getId())) {
            return typesettingInfo.getId();
        }
        return typesettingInfo.getTypesettingId();
    }

    private boolean isRequirePltFile(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null) {
            return false;
        }
        if (StringUtils.isNotBlank(typesettingInfo.getLayoutMode())) {
            TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(typesettingInfo.getLayoutMode());
            return layoutMode != null && layoutMode.isRequirePltFile();
        }
        return Boolean.TRUE.equals(typesettingInfo.getRequirePltFile());
    }


    private void appendMirrorConfigImages(Set<String> imageSet, ProductionPiece piece) {
        if (piece == null || piece.getMirrorConfigs() == null || piece.getMirrorConfigs().isEmpty()) {
            return;
        }
        for (MirrorConfig mirrorConfig : piece.getMirrorConfigs()) {
            if (mirrorConfig == null) {
                continue;
            }
            appendRawFile(imageSet, mirrorConfig.getImg());
        }
    }

    private String resolveProductionPieceImageForDownload(ProductionPiece piece) {
        if (piece == null || piece.getProductImageFile() == null) {
            return null;
        }
        if (StringUtils.isNotBlank(piece.getRouteImg())) {
            return piece.getRouteImg();
        }
        if (StringUtils.isNotBlank(piece.getRouteImg())) {
            return piece.getRouteImg();
        }
        return piece.getProductImageFile().getRawFile();
    }

    private void appendRawFile(Set<String> container, String fileUrl) {
        if (StringUtils.isNotBlank(fileUrl)) {
            container.add(fileUrl);
        }
    }

    private void appendFormeSvgImgFiles(Set<String> imageSet, String formeSvg) {
        if (imageSet == null || StringUtils.isBlank(formeSvg)) {
            return;
        }
        try {
            String svgContent = readFormeSvgContent(formeSvg);
            if (StringUtils.isBlank(svgContent)) {
                return;
            }
            Matcher imgTagMatcher = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(svgContent);
            while (imgTagMatcher.find()) {
                String imgTag = imgTagMatcher.group();
                appendRawFile(imageSet, extractImgTagAttribute(imgTag, "src"));
                appendRawFile(imageSet, extractImgTagAttribute(imgTag, "href"));
                appendRawFile(imageSet, extractImgTagAttribute(imgTag, "xlink:href"));
            }
        } catch (Exception ex) {
            log.warn("读取 formeSvg 中 img 标签失败, formeSvg={}, error={}", formeSvg, ex.getMessage());
        }
    }

    private String readFormeSvgContent(String formeSvg) throws IOException {
        String completeUrl = buildCompleteOssUrl(formeSvg);
        if (StringUtils.isNotBlank(completeUrl) && (completeUrl.startsWith("http://") || completeUrl.startsWith("https://"))) {
            return restTemplate.getForObject(URI.create(completeUrl), String.class);
        }
        Path localPath = Path.of(formeSvg);
        if (Files.exists(localPath)) {
            return Files.readString(localPath, StandardCharsets.UTF_8);
        }
        return null;
    }

    private String extractImgTagAttribute(String imgTag, String attributeName) {
        if (StringUtils.isBlank(imgTag) || StringUtils.isBlank(attributeName)) {
            return null;
        }
        Matcher attributeMatcher = Pattern.compile("(?i)(?<![\\w:-])" + Pattern.quote(attributeName)
                + "\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))").matcher(imgTag);
        if (!attributeMatcher.find()) {
            return null;
        }
        if (attributeMatcher.group(2) != null) {
            return attributeMatcher.group(2);
        }
        if (attributeMatcher.group(3) != null) {
            return attributeMatcher.group(3);
        }
        return attributeMatcher.group(4);
    }

    private void appendMarkFiles(Set<String> container, String markFileUrl) {
        appendRawFile(container, markFileUrl);
        if (StringUtils.isBlank(markFileUrl)) {
            return;
        }
        String lower = markFileUrl.toLowerCase(Locale.ROOT);
        if (lower.contains("/basetag/") && lower.contains(".svg")) {
            String pngUrl = markFileUrl.substring(0, markFileUrl.length() - 4) + ".png";
            appendRawFile(container, pngUrl);
        }
    }


    private void savePrintTaskByDeviceCode(String typesettingInfoId,
                                           String typesettingCode,
                                           String manufacturerMetaId,
                                           String deviceCode,
                                           TypesettingDownloadTaskData data) {
        ManufacturerDeviceCfg deviceCfg = findDeviceCfgByDeviceCode(manufacturerMetaId, deviceCode);
        if (StringUtils.isBlank(deviceCfg.getDeviceInfoId())) {
            throw new RuntimeException("设备编号未绑定设备信息：" + deviceCode);
        }
        String deviceInfoId = deviceCfg.getDeviceInfoId();
        String resolvedDeviceCode = StringUtils.isNotBlank(deviceCfg.getDeviceCode()) ? deviceCfg.getDeviceCode() : deviceCode;
        if (data != null) {
            data.setDeviceInfoId(deviceInfoId);
            data.setDeviceInfoIds(Collections.singletonList(deviceInfoId));
            data.setDeviceCodes(Collections.singletonList(resolvedDeviceCode));
        }
        savePrintTask(typesettingInfoId, typesettingCode, manufacturerMetaId, Collections.singletonList(deviceInfoId), Collections.singletonList(resolvedDeviceCode), data);
    }

    private void savePrintTask(String typesettingInfoId, String typesettingCode, String deviceInfoId, TypesettingDownloadTaskData data) {
        savePrintTask(typesettingInfoId, typesettingCode, null, Collections.singletonList(deviceInfoId), Collections.emptyList(), data);
    }

    private void savePrintTask(String typesettingInfoId,
                               String typesettingCode,
                               String manufacturerMetaId,
                               List<String> deviceInfoIds,
                               List<String> deviceCodes,
                               TypesettingDownloadTaskData data) {
        List<String> normalizedDeviceInfoIds = normalizeStringList(deviceInfoIds);
        List<String> normalizedDeviceCodes = normalizeStringList(deviceCodes);
        if ((normalizedDeviceInfoIds.isEmpty() || normalizedDeviceCodes.isEmpty()) && data != null) {
            if (normalizedDeviceInfoIds.isEmpty()) {
                normalizedDeviceInfoIds = normalizeStringList(data.getDeviceInfoIds());
                if (normalizedDeviceInfoIds.isEmpty() && StringUtils.isNotBlank(data.getDeviceInfoId())) {
                    normalizedDeviceInfoIds = Collections.singletonList(data.getDeviceInfoId());
                }
            }
            if (normalizedDeviceCodes.isEmpty()) {
                normalizedDeviceCodes = normalizeStringList(data.getDeviceCodes());
            }
        }
        if (data != null) {
            if (StringUtils.isNotBlank(typesettingCode)) {
                data.setId(typesettingCode);
            }
            data.setDeviceInfoIds(normalizedDeviceInfoIds);
            data.setDeviceCodes(normalizedDeviceCodes);
            if (StringUtils.isBlank(data.getDeviceInfoId()) && !normalizedDeviceInfoIds.isEmpty()) {
                data.setDeviceInfoId(normalizedDeviceInfoIds.get(0));
            }
        }
        TypesettingPrintTask task = new TypesettingPrintTask();
        task.setTypesettingInfoId(typesettingInfoId);
        task.setTypesettingCode(typesettingCode);
        task.setManufacturerMetaId(manufacturerMetaId);
        task.setDeviceInfoId(normalizedDeviceInfoIds);
        task.setDeviceCode(normalizedDeviceCodes);
        task.setStatus(TypesettingPrintTaskStatus.PENDING.getCode());
        task.setData(data);
        typesettingPrintTaskService.saveOrUpdate(task);
    }

    private List<String> normalizeStringList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : source) {
            if (StringUtils.isNotBlank(item)) {
                values.add(item);
            }
        }
        return values.isEmpty() ? Collections.emptyList() : new ArrayList<>(values);
    }

    private void savePltBroadcastPrintTask(String typesettingInfoId,
                                           String typesettingCode,
                                           String manufacturerMetaId,
                                           TypesettingDownloadTaskData originalData) {
        savePltBroadcastPrintTask(typesettingInfoId, typesettingCode, manufacturerMetaId, originalData, null);
    }

    private void savePltBroadcastPrintTask(String typesettingInfoId,
                                           String typesettingCode,
                                           String manufacturerMetaId,
                                           TypesettingDownloadTaskData originalData,
                                           TypesettingInfo rootTypesettingInfo) {
        LinkedHashSet<String> pltSet = new LinkedHashSet<>();
        if (originalData != null) {
            pltSet.addAll(normalizeStringList(originalData.getPlts()));
        }
        collectRequiredPltsRecursive(rootTypesettingInfo, pltSet, new HashSet<>());
        if (pltSet.isEmpty()) {
            return;
        }
        List<ManufacturerDeviceCfg> cuttingDeviceCfgs = findCuttingDeviceCfgs(manufacturerMetaId);
        LinkedHashSet<String> cuttingDeviceInfoIds = new LinkedHashSet<>();
        LinkedHashSet<String> cuttingDeviceCodes = new LinkedHashSet<>();
        for (ManufacturerDeviceCfg cfg : cuttingDeviceCfgs) {
            if (StringUtils.isNotBlank(cfg.getDeviceInfoId())) {
                cuttingDeviceInfoIds.add(cfg.getDeviceInfoId());
            }
            if (StringUtils.isNotBlank(cfg.getDeviceCode())) {
                cuttingDeviceCodes.add(cfg.getDeviceCode());
            }
        }
        if (cuttingDeviceInfoIds.isEmpty() && originalData != null) {
            cuttingDeviceInfoIds.addAll(normalizeStringList(originalData.getDeviceInfoIds()));
            if (cuttingDeviceInfoIds.isEmpty() && StringUtils.isNotBlank(originalData.getDeviceInfoId())) {
                cuttingDeviceInfoIds.add(originalData.getDeviceInfoId());
            }
        }
        if (cuttingDeviceCodes.isEmpty() && originalData != null) {
            cuttingDeviceCodes.addAll(normalizeStringList(originalData.getDeviceCodes()));
        }
        if (cuttingDeviceInfoIds.isEmpty() && originalData != null) {
            cuttingDeviceInfoIds.addAll(normalizeStringList(originalData.getDeviceInfoIds()));
            if (cuttingDeviceInfoIds.isEmpty() && StringUtils.isNotBlank(originalData.getDeviceInfoId())) {
                cuttingDeviceInfoIds.add(originalData.getDeviceInfoId());
            }
        }
        if (cuttingDeviceCodes.isEmpty() && originalData != null) {
            cuttingDeviceCodes.addAll(normalizeStringList(originalData.getDeviceCodes()));
        }
        if (cuttingDeviceInfoIds.isEmpty()) {
            return;
        }
        TypesettingDownloadTaskData pltOnlyData = new TypesettingDownloadTaskData();
        pltOnlyData.setId(originalData == null ? typesettingInfoId : originalData.getId());
        pltOnlyData.setDeviceInfoId(cuttingDeviceInfoIds.iterator().next());
        pltOnlyData.setDeviceInfoIds(new ArrayList<>(cuttingDeviceInfoIds));
        pltOnlyData.setDeviceCodes(new ArrayList<>(cuttingDeviceCodes));
        pltOnlyData.setImamges(Collections.emptyList());
        pltOnlyData.setPlts(new ArrayList<>(pltSet));
        pltOnlyData.setJsons(Collections.emptyList());
        pltOnlyData.setMarks(Collections.emptyList());
        savePrintTask(typesettingInfoId + "_plt", typesettingCode, manufacturerMetaId, new ArrayList<>(cuttingDeviceInfoIds), new ArrayList<>(cuttingDeviceCodes), pltOnlyData);
    }

    private List<ManufacturerDeviceCfg> findCuttingDeviceCfgs(String manufacturerMetaId) {
        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(manufacturerMetaId)) {
            filters.put("manufacturerMetaId", manufacturerMetaId);
        }
        List<ManufacturerDeviceCfg> cfgList = manufacturerDeviceCfgRepository.filterList(1, 1000, filters);
        if (cfgList == null || cfgList.isEmpty()) {
            return Collections.emptyList();
        }
        List<ManufacturerDeviceCfg> cuttingCfgs = new ArrayList<>();
        for (ManufacturerDeviceCfg cfg : cfgList) {
            if (cfg == null || StringUtils.isBlank(cfg.getDeviceName())) {
                continue;
            }
            if (cfg.getDeviceName().contains("切割")) {
                cuttingCfgs.add(cfg);
            }
        }
        return cuttingCfgs;
    }

    private TypesettingDownloadTaskData copyDownloadTaskDataWithoutPlts(TypesettingDownloadTaskData originalData) {
        if (originalData == null) {
            return null;
        }
        TypesettingDownloadTaskData copied = new TypesettingDownloadTaskData();
        copied.setId(originalData.getId());
        copied.setDeviceInfoId(originalData.getDeviceInfoId());
        copied.setDeviceInfoIds(originalData.getDeviceInfoIds() == null ? Collections.emptyList() : new ArrayList<>(originalData.getDeviceInfoIds()));
        copied.setDeviceCodes(originalData.getDeviceCodes() == null ? Collections.emptyList() : new ArrayList<>(originalData.getDeviceCodes()));
        copied.setImamges(originalData.getImamges() == null ? Collections.emptyList() : new ArrayList<>(originalData.getImamges()));
        copied.setPlts(Collections.emptyList());
        copied.setJsons(originalData.getJsons() == null ? Collections.emptyList() : new ArrayList<>(originalData.getJsons()));
        copied.setMarks(originalData.getMarks() == null ? Collections.emptyList() : new ArrayList<>(originalData.getMarks()));
        return copied;
    }

    /**
     * 开始打印：将排版数据根据状态机改为打印中状态
     *
     * @param productionPieceIds 生产工件 ID 列表
     * @return 操作结果
     */
    public ConfirmPrintResult startPrint(List<String> productionPieceIds) {
        if (productionPieceIds == null || productionPieceIds.isEmpty()) {
            throw new RuntimeException("生产工件 ID 列表不能为空");
        }

        List<ProductionPiece> productionPieces = new ArrayList<>();

        for (String pieceId : productionPieceIds) {
            try {
                // 使用状态机方法更新为打印中状态
                productionPieceService.startPrinting(pieceId);

                // 获取更新后的工件信息
                ProductionPiece piece = productionPieceService.findById(pieceId);
                productionPieces.add(piece);

            } catch (Exception e) {
                System.err.println("更新生产工件 " + pieceId + " 状态失败：" + e.getMessage());
            }
        }

        ConfirmPrintResult result = new ConfirmPrintResult();
        result.setSuccess(true);
        result.setMessage("开始打印成功，共更新 " + productionPieces.size() + " 个工件为打印中状态");
        result.setUpdatedPieceCount(productionPieces.size());
        result.setUpdatedPieceIds(productionPieces.stream()
                .map(ProductionPiece::getId)
                .collect(Collectors.toList()));

        return result;
    }

    /**
     * 释放排版：删除排版文件，将参与的零件状态改回待排版状态
     *
     * @param typesettingIds 排版 ID 列表
     * @return 操作结果
     */
    public ReleaseLayoutResult releaseLayout(List<String> typesettingIds) {
        if (typesettingIds == null || typesettingIds.isEmpty()) {
            throw new RuntimeException("排版ID列表不能为空");
        }

        Map<String, Integer> productionPieceRollbackQuantity = new LinkedHashMap<>();
        Map<String, Integer> typesettingRollbackQuantity = new LinkedHashMap<>();
        List<String> releasedPieceIds = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        List<String> deletedLayoutIds = new ArrayList<>();
        Set<String> deletedLayoutIdSet = new LinkedHashSet<>();

        for (String typesettingId : typesettingIds) {
            if (StringUtils.isBlank(typesettingId)) {
                continue;
            }
            if (deletedLayoutIdSet.contains(typesettingId)) {
                continue;
            }
            TypesettingInfo info = domainTypesettingService.findById(typesettingId);
            if (info == null || StringUtils.isBlank(info.getId())) {
                errorMessages.add("排版记录不存在: " + typesettingId);
                continue;
            }
            TypesettingInfo pairedMirrorTypesetting = findReleaseLayoutMirrorPair(info);
            if (pairedMirrorTypesetting != null && StringUtils.isNotBlank(pairedMirrorTypesetting.getId())) {
                boolean pairedLayoutCanRelease = (pairedMirrorTypesetting.getLeaveQuantity() != null && pairedMirrorTypesetting.getLeaveQuantity() != 0)
                        && TypesettingStatus.PENDING.getCode().equals(pairedMirrorTypesetting.getStatus());
                if (!pairedLayoutCanRelease) {
                    errorMessages.add("排版记录 " + info.getId() + " 的正面或反面文件已经被使用，无法释放");
                    continue;
                }
            }

            List<TypesettingSourceCell> usedCells = info.getTypesettingCells();
            if ((usedCells == null || usedCells.isEmpty()) && info.getElement() != null
                    && StringUtils.isNotBlank(info.getElement().getNestedSvg())) {
                usedCells = extractUsedSourceCells(info.getTypesettingId(), info.getElement().getNestedSvg());
            }
            for (TypesettingSourceCell usedCell : usedCells == null ? Collections.<TypesettingSourceCell>emptyList() : usedCells) {
                if (usedCell == null || StringUtils.isBlank(usedCell.getSourceType()) || StringUtils.isBlank(usedCell.getSourceId())) {
                    continue;
                }
                int usedQuantity = usedCell.getQuantity() == null || usedCell.getQuantity() <= 0 ? 1 : usedCell.getQuantity();
                if (TypesettingSourceType.PART.getCode().equals(usedCell.getSourceType())) {
                    if (!isMirrorTypesettingInfo(info)) {
                        productionPieceRollbackQuantity.merge(usedCell.getSourceId(), usedQuantity, Integer::sum);
                    }
                } else if (TypesettingSourceType.TYPESETTING.getCode().equals(usedCell.getSourceType())) {
                    typesettingRollbackQuantity.merge(usedCell.getSourceId(), usedQuantity, Integer::sum);
                }
            }

            try {
                domainTypesettingService.deleteTypesetting(info.getId());
                deletedLayoutIds.add(info.getId());
                deletedLayoutIdSet.add(info.getId());
            } catch (Exception e) {
                errorMessages.add("删除排版记录失败(" + info.getId() + "): " + e.getMessage());
                continue;
            }

            TypesettingInfo mirrorTypesetting = findMirrorTypesettingInfo(info);
            if (mirrorTypesetting == null || StringUtils.isBlank(mirrorTypesetting.getId())) {
                continue;
            }
            try {
                domainTypesettingService.deleteTypesetting(mirrorTypesetting.getId());
                deletedLayoutIds.add(mirrorTypesetting.getId());
                deletedLayoutIdSet.add(mirrorTypesetting.getId());
            } catch (Exception e) {
                errorMessages.add("删除镜像排版记录失败(" + mirrorTypesetting.getId() + "): " + e.getMessage());
            }
        }

        for (Map.Entry<String, Integer> entry : typesettingRollbackQuantity.entrySet()) {
            String sourceTypesettingId = entry.getKey();
            Integer rollbackQuantity = entry.getValue();
            if (StringUtils.isBlank(sourceTypesettingId) || rollbackQuantity == null || rollbackQuantity <= 0) {
                continue;
            }
            try {
                TypesettingInfo sourceTypesetting = domainTypesettingService.findById(sourceTypesettingId);
                if (sourceTypesetting == null || StringUtils.isBlank(sourceTypesetting.getId())) {
                    errorMessages.add("来源印版不存在: " + sourceTypesettingId);
                    continue;
                }
                sourceTypesetting.setStatus(TypesettingStatus.PENDING.getCode());
                sourceTypesetting.setLeaveQuantity(rollbackQuantity);
                domainTypesettingService.updateTypesetting(sourceTypesetting);
            } catch (Exception e) {
                errorMessages.add("回退印版失败(" + sourceTypesettingId + "): " + e.getMessage());
            }
        }

        for (Map.Entry<String, Integer> entry : productionPieceRollbackQuantity.entrySet()) {
            String productionPieceRecordId = entry.getKey();
            Integer rollbackQuantity = entry.getValue();
            if (StringUtils.isBlank(productionPieceRecordId) || rollbackQuantity == null || rollbackQuantity <= 0) {
                continue;
            }
            try {
                ProductionPiece piece = productionPieceService.findById(productionPieceRecordId);
                if (piece == null || StringUtils.isBlank(piece.getId())) {
                    errorMessages.add("生产工件不存在: " + productionPieceRecordId);
                    continue;
                }
                productionPieceService.transferPieceQuantityBetweenNodes(
                        piece.getId(),
                        "NODE_TYPESETTING_IN_PROGRESS",
                        "NODE_TYPESETTING",
                        rollbackQuantity
                );
                releasedPieceIds.add(piece.getId());
            } catch (Exception e) {
                errorMessages.add("回退工件失败(" + productionPieceRecordId + "): " + e.getMessage());
            }
        }

        ReleaseLayoutResult result = new ReleaseLayoutResult();
        result.setSuccess(errorMessages.isEmpty());
        result.setMessage(errorMessages.isEmpty()
                ? "释放排版成功，删除排版记录 " + deletedLayoutIds.size() + " 条"
                : "释放排版完成，存在部分失败: " + String.join("；", errorMessages));
        result.setReleasedPieceCount(releasedPieceIds.size());
        result.setReleasedPieceIds(releasedPieceIds);
        result.setDeletedLayoutIds(deletedLayoutIds);
        return result;
    }

    /**
     * 完全释放排版。与普通释放不同，本操作会沿印版 cell 递归到叶子零件，删除整棵印版树，
     * 并把每个叶子零件当前位于“待打印”或“打印中”节点的全部数量退回“待排版”。
     */
    public ReleaseLayoutResult completeReleaseLayout(List<String> typesettingIds) {
        if (typesettingIds == null || typesettingIds.isEmpty()) {
            throw new RuntimeException("排版ID列表不能为空");
        }

        Map<String, TypesettingInfo> layoutsToDelete = new LinkedHashMap<>();
        Set<String> productionPieceIds = new LinkedHashSet<>();
        List<String> errorMessages = new ArrayList<>();
        for (String typesettingId : typesettingIds) {
            collectCompleteReleaseTargets(typesettingId, layoutsToDelete, productionPieceIds,
                    new LinkedHashSet<>(), errorMessages);
        }

        List<String> releasedPieceIds = new ArrayList<>();
        for (String productionPieceId : productionPieceIds) {
            try {
                ProductionPiece piece = productionPieceService.findById(productionPieceId);
                if (piece == null || StringUtils.isBlank(piece.getId())) {
                    errorMessages.add("生产工件不存在: " + productionPieceId);
                    continue;
                }
                rollbackAllPrintingQuantity(piece, "待打印");
                rollbackAllPrintingQuantity(piece, "打印中");
                releasedPieceIds.add(piece.getId());
            } catch (Exception e) {
                errorMessages.add("回退工件失败(" + productionPieceId + "): " + e.getMessage());
            }
        }

        List<String> deletedLayoutIds = new ArrayList<>();
        List<TypesettingInfo> deleteOrder = new ArrayList<>(layoutsToDelete.values());
        Collections.reverse(deleteOrder);
        for (TypesettingInfo info : deleteOrder) {
            try {
                domainTypesettingService.deleteTypesetting(info.getId());
                deletedLayoutIds.add(info.getId());
            } catch (Exception e) {
                errorMessages.add("删除排版记录失败(" + info.getId() + "): " + e.getMessage());
            }
        }

        ReleaseLayoutResult result = new ReleaseLayoutResult();
        result.setSuccess(errorMessages.isEmpty());
        result.setMessage(errorMessages.isEmpty()
                ? "完全释放排版成功，删除排版记录 " + deletedLayoutIds.size() + " 条"
                : "完全释放排版完成，存在部分失败: " + String.join("；", errorMessages));
        result.setReleasedPieceCount(releasedPieceIds.size());
        result.setReleasedPieceIds(releasedPieceIds);
        result.setDeletedLayoutIds(deletedLayoutIds);
        return result;
    }

    private void collectCompleteReleaseTargets(String typesettingId,
                                               Map<String, TypesettingInfo> layoutsToDelete,
                                               Set<String> productionPieceIds,
                                               Set<String> visitingIds,
                                               List<String> errorMessages) {
        if (StringUtils.isBlank(typesettingId) || layoutsToDelete.containsKey(typesettingId)) {
            return;
        }
        if (!visitingIds.add(typesettingId)) {
            errorMessages.add("印版引用存在循环: " + typesettingId);
            return;
        }
        TypesettingInfo info = domainTypesettingService.findById(typesettingId);
        if (info == null || StringUtils.isBlank(info.getId())) {
            errorMessages.add("排版记录不存在: " + typesettingId);
            visitingIds.remove(typesettingId);
            return;
        }
        layoutsToDelete.put(info.getId(), info);
        for (TypesettingSourceCell cell : info.getTypesettingCells() == null
                ? Collections.<TypesettingSourceCell>emptyList() : info.getTypesettingCells()) {
            if (cell == null || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            if (TypesettingSourceType.PART.getCode().equals(cell.getSourceType())) {
                productionPieceIds.add(cell.getSourceId());
            } else if (TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                collectCompleteReleaseTargets(cell.getSourceId(), layoutsToDelete, productionPieceIds,
                        visitingIds, errorMessages);
            }
        }
        TypesettingInfo mirror = findMirrorTypesettingInfo(info);
        if (mirror != null && StringUtils.isNotBlank(mirror.getId())) {
            collectCompleteReleaseTargets(mirror.getId(), layoutsToDelete, productionPieceIds,
                    visitingIds, errorMessages);
        }
        visitingIds.remove(typesettingId);
    }

    private void rollbackAllPrintingQuantity(ProductionPiece piece, String nodeName) {
        if (piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
            return;
        }
        for (ProcedureFlowNode node : piece.getProcedureFlow().getNodes()) {
            if (node == null || !nodeName.equals(node.getNodeName())
                    || StringUtils.isBlank(node.getNodeId()) || node.getPieceQuantity() == null
                    || node.getPieceQuantity() <= 0) {
                continue;
            }
            productionPieceService.transferPieceQuantityBetweenNodes(
                    piece.getId(), node.getNodeId(), "NODE_TYPESETTING", node.getPieceQuantity());
        }
    }

    private TypesettingInfo findReleaseLayoutMirrorPair(TypesettingInfo info) {
        if (info == null || StringUtils.isBlank(info.getTypesettingId())) {
            return null;
        }
        String typesettingId = info.getTypesettingId();
        if (typesettingId.endsWith("-Mirror")) {
            String frontTypesettingId = typesettingId.substring(0, typesettingId.length() - "-Mirror".length());
            return findExactTypesettingByTypesettingIdAndTemplateCode(frontTypesettingId, info.getTemplateCode(), info.getId());
        }
        String mirrorTypesettingId = typesettingId + "-Mirror";
        return findExactTypesettingByTypesettingIdAndTemplateCode(mirrorTypesettingId, info.getTemplateCode(), info.getId());
    }

    private TypesettingInfo findExactTypesettingByTypesettingIdAndTemplateCode(String typesettingId,
                                                                               String templateCode,
                                                                               String excludedRecordId) {
        if (StringUtils.isBlank(typesettingId)) {
            return null;
        }
        List<TypesettingInfo> candidates = domainTypesettingService.findTypesettingListByTypesettingId(typesettingId);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .filter(candidate -> candidate != null)
                .filter(candidate -> !Objects.equals(candidate.getId(), excludedRecordId))
                .filter(candidate -> Objects.equals(candidate.getTypesettingId(), typesettingId))
                .filter(candidate -> StringUtils.isBlank(templateCode) || Objects.equals(candidate.getTemplateCode(), templateCode))
                .findFirst()
                .orElse(null);
    }

    /**
     * 校验所有订单项的材料是否一致
     *
     * @return "PASS" 表示通过，否则返回错误信息
     */
    private String validateMaterials(List<ProductionPiece> productionPieces) {
        if (productionPieces.isEmpty()) {
            return "PASS";
        }

        MaterialConfig firstMaterial = productionPieces.get(0).getMaterialConfig();
        if (firstMaterial == null) {
            return "第一个零件的材料为空";
        }
        String firstMaterialName = getMaterialName(firstMaterial);
        if (StringUtils.isBlank(firstMaterialName)) {
            return "第一个零件的材料名称为空";
        }
        for (int i = 1; i < productionPieces.size(); i++) {
            MaterialConfig material = productionPieces.get(i).getMaterialConfig();
            if (material == null) {
                return "零件 " + productionPieces.get(i).getProductionPieceId() + " 的材料为空";
            }
            String materialName = getMaterialName(material);
            if (!Objects.equals(firstMaterialName, materialName)) {
                return "材料不一致：零件 " + productionPieces.get(0).getProductionPieceId() +
                        " 的材料为 " + firstMaterial.getMaterialSnapshot().getName() +
                        "，零件 " + productionPieces.get(i).getProductionPieceId() +
                        " 的材料为 " + material.getMaterialSnapshot().getName();
            }
        }

        return "PASS";
    }

    private String getMaterialName(MaterialConfig material) {
        if (material == null || material.getMaterialSnapshot() == null) {
            return null;
        }
        String name = material.getMaterialSnapshot().getName();
        if (name == null) {
            return null;
        }
        name = name.trim();
        return name.isEmpty() ? null : name;
    }

    /**
     * 校验特殊工艺参数一致性。
     *
     * <p>双面对裱/覆双面的工艺参数由“反面相同画面”或“反面不同画面”承载，
     * 其自身节点允许无参数，因此这里只校验仍要求自身携带参数的覆板工艺。</p>
     *
     * @param productionPieces 生产工件列表
     * @return "PASS" 表示通过，否则返回错误信息
     */
    private String validateSpecialProcedureMaterials(List<ProductionPiece> productionPieces) {
        String benchmarkParamsSignature = null;
        String benchmarkPieceId = null;
        String benchmarkNodeName = null;

        for (ProductionPiece piece : productionPieces) {
            if (piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
                continue;
            }

            for (ProcedureFlowNode node : piece.getProcedureFlow().getNodes()) {
                if (!isSpecialProcedureNode(node)) {
                    continue;
                }

                String paramsSignature = buildParamConfigsSignature(node);
                if (StringUtils.isBlank(paramsSignature)) {
                    return "生产工件 " + piece.getProductionPieceId() + " 的工序 " + node.getNodeName() + " 参数为空";
                }

                if (benchmarkParamsSignature == null) {
                    benchmarkParamsSignature = paramsSignature;
                    benchmarkPieceId = piece.getProductionPieceId();
                    benchmarkNodeName = node.getNodeName();
                    continue;
                }

                if (!benchmarkParamsSignature.equals(paramsSignature)) {
                    return "特殊工序参数不一致：零件 " + benchmarkPieceId + " 的工序 " + benchmarkNodeName +
                            " 与零件 " + piece.getProductionPieceId() + " 的工序 " + node.getNodeName() + " 参数不一致";
                }
            }
        }

        return "PASS";
    }

    /**
     * 镜像排版 SVG 不需要携带拼接预处理写入正面 mask 的 mark。
     *
     * <p>带 marks 的工件会以重写后的 mask SVG 参与排版，算法生成 nestedMirrorSvg 时会把这些
     * super-width/adhesive/photo/board-cover/inkjet/seamless/panel splice 标识一起带入。
     * 这里在回调落库前清理这些拼接 mark，并上传一份干净的 nestedMirrorSvg 供后续 -Mirror 印版使用。</p>
     */
    private String removeSpliceMarksFromNestedMirrorSvg(String nestedMirrorSvg, TypesettingInfo typesettingInfo, String templateCode) {
        if (StringUtils.isBlank(nestedMirrorSvg)) {
            return nestedMirrorSvg;
        }
        try {
            String completeUrl = buildCompleteOssUrl(nestedMirrorSvg);
            byte[] svgBytes = restTemplate.getForObject(URI.create(completeUrl), byte[].class);
            if (svgBytes == null || svgBytes.length == 0) {
                return nestedMirrorSvg;
            }
            String svgContent = new String(svgBytes, StandardCharsets.UTF_8);
            Matcher matcher = SPLICE_MARK_GROUP_PATTERN.matcher(svgContent);
            if (!matcher.find()) {
                return nestedMirrorSvg;
            }
            String sanitizedSvg = matcher.replaceAll("");
            String businessId = StringUtils.isNotBlank(typesettingInfo.getTypesettingId())
                    ? typesettingInfo.getTypesettingId()
                    : typesettingInfo.getId();
            String manufacturerMetaId = StringUtils.isBlank(typesettingInfo.getManufacturerMetaId())
                    ? "default"
                    : typesettingInfo.getManufacturerMetaId();
            String safeTemplateCode = StringUtils.isBlank(templateCode) ? "default" : templateCode;
            String uploadPath = "typesetting/" + manufacturerMetaId + "/nested-mirror-clean/" + businessId + "/";
            return ossTagUploadService.uploadTagSvg(businessId, sanitizedSvg.getBytes(StandardCharsets.UTF_8), uploadPath, safeTemplateCode + ".svg");
        } catch (Exception e) {
            log.warn("清理 nestedMirrorSvg 拼接 mark 失败，继续使用原始 mirror SVG: typesettingId={}, nestedMirrorSvg={}, error={}",
                    typesettingInfo == null ? null : typesettingInfo.getTypesettingId(), nestedMirrorSvg, e.getMessage());
            return nestedMirrorSvg;
        }
    }

    private boolean isSpecialProcedureNode(ProcedureFlowNode node) {
        return node != null && "覆板".equals(node.getNodeName());
    }

    private String buildParamConfigsSignature(ProcedureFlowNode node) {
        if (node == null || node.getParamConfigs() == null || node.getParamConfigs().isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO.ProcessParamConfigDTO paramConfig : node.getParamConfigs()) {
            if (paramConfig == null || paramConfig.getParam() == null) {
                continue;
            }
            Object param = paramConfig.getParam();
            Object paramId = invokeNoArgMethod(param, "getParamId");
            Object value = invokeNoArgMethod(param, "getValue");
            builder.append(paramId == null ? "unknown" : paramId)
                    .append("=")
                    .append(value == null ? "null" : String.valueOf(value))
                    .append(";");
        }

        return builder.length() == 0 ? null : builder.toString();
    }

    private Object invokeNoArgMethod(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }


    /**
     * 排版算法回调方法
     * @param response 排版算法响应
     */
    public void handleNestingCallback(NestingResponse response) {
        if (response == null || StringUtils.isBlank(response.getId())) {
            throw new RuntimeException("回调参数无效");
        }

        String typesettingId = response.getId();
        List<TypesettingInfo> typesettingInfos = waitForTypesettingRecords(typesettingId);
        if (typesettingInfos == null || typesettingInfos.isEmpty()) {
            throw new RuntimeException("排版信息不存在：" + typesettingId);
        }

        TypesettingInfo baseTypesettingInfo = typesettingInfos.get(0);
        try {
            if ("success".equals(response.getStatus())) {
                List<NestingResponse.Result> results = response.getResults();
                if (results == null || results.isEmpty()) {
                    throw new RuntimeException("排版回调成功但未返回结果");
                }
                // 将第一条结果落在原记录上，后续结果新增记录，使用同一个 typesettingId
                int total = results.size();
                List<List<TypesettingSourceCell>> usedCellsByResult = new ArrayList<>(total);
                LayoutConfirmRequest cachedRequest = getCachedLayoutConfirmRequest(typesettingId);
                boolean taskHaveBlood = false;
                for (NestingResponse.Result callbackResult : results) {
                    List<TypesettingSourceCell> usedCells = extractUsedSourceCells(cachedRequest, callbackResult.getNestedSvg());
                    usedCellsByResult.add(usedCells);
                    if (Boolean.TRUE.equals(resolveCallbackResultHaveBlood(callbackResult, usedCells, cachedRequest))) {
                        taskHaveBlood = true;
                    }
                }
                for (int i = 0; i < total; i++) {
                    NestingResponse.Result callbackResult = results.get(i);
                    List<TypesettingSourceCell> usedCells = usedCellsByResult.get(i);
                    String templateCode = buildTemplateCode(i + 1, total);
                    TypesettingElement element = new TypesettingElement();
                    element.setNestedSvg(buildCompleteOssUrl(callbackResult.getNestedSvg()));
                    String nestedMirrorSvg = StringUtils.isNotBlank(callbackResult.getNestedMirrorSvg())
                            ? callbackResult.getNestedMirrorSvg()
                            : callbackResult.getMirrorNestedSvg();
                    if (StringUtils.isNotBlank(nestedMirrorSvg)) {
                        String sanitizedNestedMirrorSvg = removeSpliceMarksFromNestedMirrorSvg(nestedMirrorSvg, baseTypesettingInfo, templateCode);
                        element.setNestedMirrorSvg(buildCompleteOssUrl(sanitizedNestedMirrorSvg));
                    }
                    element.setUtilization(callbackResult.getUtilization());
                    if (callbackResult.getContainerSize() != null) {
                        element.setWidth(callbackResult.getContainerSize().getWidth());
                        element.setHeight(callbackResult.getContainerSize().getHeight());
                    } else if (callbackResult.getWidth() != null || callbackResult.getHeight() != null) {
                        element.setWidth(callbackResult.getWidth());
                        element.setHeight(callbackResult.getHeight());
                    }
                    if (callbackResult.getGridLines() != null) {
                        element.setGridLines(new TypesettingElement.GridLines(
                                callbackResult.getGridLines().getXs(),
                                callbackResult.getGridLines().getYs()
                        ));
                    }
                    if (i == 0) {
                        baseTypesettingInfo.setStatus(TypesettingStatus.CONFIRMING.getCode());
                        baseTypesettingInfo.setElement(mergeElementKeepingSize(baseTypesettingInfo.getElement(), element));
                        baseTypesettingInfo.setTypesettingCells(usedCells);
                        baseTypesettingInfo.setHaveBlood(taskHaveBlood);
                        baseTypesettingInfo.setTemplateCode(templateCode);
                        domainTypesettingService.updateTypesetting(baseTypesettingInfo);
                        continue;
                    }
                    TypesettingInfo newTypesettingInfo = cloneForCallback(baseTypesettingInfo);
                    newTypesettingInfo.setId(null);
                    newTypesettingInfo.setManufacturerMetaId(baseTypesettingInfo.getManufacturerMetaId());
                    newTypesettingInfo.setElement(element);
                    newTypesettingInfo.setTypesettingCells(usedCells);
                    newTypesettingInfo.setHaveBlood(taskHaveBlood);
                    newTypesettingInfo.setTemplateCode(templateCode);
                    newTypesettingInfo.setStatus(TypesettingStatus.CONFIRMING.getCode());
                    domainTypesettingService.addTypesetting(newTypesettingInfo);
                }
            } else {
                markTypesettingsFailed(typesettingInfos,
                        StringUtils.isNotBlank(response.getError()) ? response.getError() : "排版异步生成失败");
            }
        } catch (Exception e) {
            String failureReason = "排版回调处理异常：" + resolveExceptionMessage(e);
            log.error(failureReason, e);
            markTypesettingsFailed(typesettingInfos, failureReason);
            throw new RuntimeException(failureReason, e);
        }
    }

    /**
     * 算法服务可能在 toLayout 完成初始排版记录落库前就发起回调。
     * 在这个很短的并发窗口内立即判定记录不存在，会丢失本次成功回调，
     * 并使新建印版一直停留在 in_progress。
     */
    private List<TypesettingInfo> waitForTypesettingRecords(String typesettingId) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(NESTING_CALLBACK_RECORD_WAIT_MILLIS);
        List<TypesettingInfo> typesettingInfos;
        do {
            typesettingInfos = domainTypesettingService.findTypesettingListByTypesettingId(typesettingId);
            if (typesettingInfos != null && !typesettingInfos.isEmpty()) {
                return typesettingInfos;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(NESTING_CALLBACK_RECORD_POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待排版信息落库时被中断：" + typesettingId, e);
            }
        } while (System.nanoTime() < deadline);
        return typesettingInfos;
    }


    private TypesettingElement mergeElementKeepingSize(TypesettingElement oldElement, TypesettingElement newElement) {
        if (newElement == null) {
            return oldElement;
        }
        if (oldElement == null) {
            return newElement;
        }
        if (newElement.getWidth() == null) {
            newElement.setWidth(oldElement.getWidth());
        }
        if (newElement.getHeight() == null) {
            newElement.setHeight(oldElement.getHeight());
        }
        return newElement;
    }

    private Boolean resolveCallbackResultHaveBlood(NestingResponse.Result callbackResult,
                                                    List<TypesettingSourceCell> usedCells,
                                                    LayoutConfirmRequest cachedRequest) {
        if (callbackResult != null && callbackResult.getHaveBlood() != null) {
            return callbackResult.getHaveBlood();
        }
        Boolean cachedHaveBlood = resolveCachedHaveBlood(usedCells, cachedRequest);
        if (cachedHaveBlood != null) {
            return cachedHaveBlood;
        }
        return hasBloodInTypesettingCells(usedCells, new HashSet<>());
    }

    /**
     * Reads the source blood snapshot persisted by toLayout. Returning null is intentional:
     * callbacks for cache entries created before this field existed must retain the database fallback.
     */
    private Boolean resolveCachedHaveBlood(List<TypesettingSourceCell> usedCells, LayoutConfirmRequest cachedRequest) {
        if (usedCells == null || usedCells.isEmpty() || cachedRequest == null
                || CollectionUtils.isEmpty(cachedRequest.getTypesettingCells())) {
            return null;
        }
        Map<String, Boolean> bloodBySource = new HashMap<>();
        for (TypesettingProductionPieceVO cachedCell : cachedRequest.getTypesettingCells()) {
            if (cachedCell == null || StringUtils.isBlank(cachedCell.getSourceType())
                    || StringUtils.isBlank(cachedCell.getSourceId()) || cachedCell.getHaveBlood() == null) {
                continue;
            }
            bloodBySource.put(cachedCell.getSourceType() + ":" + cachedCell.getSourceId(), cachedCell.getHaveBlood());
        }
        boolean matchedSource = false;
        for (TypesettingSourceCell usedCell : usedCells) {
            if (usedCell == null || StringUtils.isBlank(usedCell.getSourceType())
                    || StringUtils.isBlank(usedCell.getSourceId())) {
                continue;
            }
            String sourceKey = usedCell.getSourceType() + ":" + usedCell.getSourceId();
            if (!bloodBySource.containsKey(sourceKey)) {
                return null;
            }
            matchedSource = true;
            if (Boolean.TRUE.equals(bloodBySource.get(sourceKey))) {
                return true;
            }
        }
        return matchedSource ? false : null;
    }

    private boolean hasBloodInTypesettingCells(List<TypesettingSourceCell> usedCells, Set<String> visitedTypesettingIds) {
        if (usedCells == null || usedCells.isEmpty()) {
            return false;
        }
        for (TypesettingSourceCell cell : usedCells) {
            if (cell == null || StringUtils.isBlank(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            if (TypesettingSourceType.PART.getCode().equals(cell.getSourceType())) {
                ProductionPiece piece = productionPieceService.findById(cell.getSourceId());
                if (isBloodPieceByCoordinates(piece)) {
                    return true;
                }
                continue;
            }
            if (!TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                continue;
            }
            if (!visitedTypesettingIds.add(cell.getSourceId())) {
                continue;
            }
            TypesettingInfo sourceTypesetting = domainTypesettingService.findById(cell.getSourceId());
            if (sourceTypesetting == null) {
                continue;
            }
            if (Boolean.TRUE.equals(sourceTypesetting.getHaveBlood())) {
                return true;
            }
            if (hasBloodInTypesettingCells(sourceTypesetting.getTypesettingCells(), visitedTypesettingIds)) {
                return true;
            }
        }
        return false;
    }

    private String buildTemplateCode(int current, int total) {
        if (total <= 0) {
            total = 1;
        }
        if (current <= 0) {
            current = 1;
        }
        if (current > total) {
            current = total;
        }
        return current + "/" + total;
    }
    private TypesettingInfo cloneForCallback(TypesettingInfo source) {
        TypesettingInfo target = new TypesettingInfo();
        target.setTypesettingId(source.getTypesettingId());
        target.setMaterialConfig(source.getMaterialConfig());
        target.setMaterialConfigs(source.getMaterialConfigs());
        target.setProcessingFlow(source.getProcessingFlow());
        target.setQuantity(source.getQuantity());
        target.setLeaveQuantity(source.getLeaveQuantity());
        target.setTypesettingCells(source.getTypesettingCells());
        target.setProcedureFlow(source.getProcedureFlow());
        target.setRemark(source.getRemark());
        target.setMaskSvg(source.getMaskSvg());
        target.setLayoutMode(source.getLayoutMode());
        target.setLayoutCategory(source.getLayoutCategory());
        target.setRequireJsonFile(source.getRequireJsonFile());
        target.setRequirePltFile(source.getRequirePltFile());
        target.setRequireSvgFile(source.getRequireSvgFile());
        target.setCodeGenerateType(source.getCodeGenerateType());
        target.setTempCodeFormat(source.getTempCodeFormat());
        target.setAnchorPointShape(source.getAnchorPointShape());
        target.setTemplateCode(source.getTemplateCode());
        return target;
    }

    private String resolveProcessingFlowFromOrderItem(List<ProductionPiece> productionPieces, List<TypesettingInfo> typesettingInfos) {
        if (productionPieces != null) {
            for (ProductionPiece piece : productionPieces) {
                if (piece != null && StringUtils.isNotBlank(piece.getProcessingFlow())) {
                    return piece.getProcessingFlow();
                }
            }
        }
        if (typesettingInfos != null) {
            for (TypesettingInfo info : typesettingInfos) {
                if (info != null && StringUtils.isNotBlank(info.getProcessingFlow())) {
                    return info.getProcessingFlow();
                }
            }
        }
        return null;
    }

    private void validateNoSecondaryTypesettingCells(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null || typesettingInfo.getTypesettingCells() == null) {
            return;
        }
        for (TypesettingSourceCell cell : typesettingInfo.getTypesettingCells()) {
            if (cell != null && TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                throw new IllegalArgumentException("该印版已是二次排版生成的印版，不能继续排版");
            }
        }
    }


    /**
     * 校验并构建新的排版工序流。
     * 规则：
     * 1) 不能混排“覆膜”和“不覆膜”；
     * 2) 如果存在“覆膜”，其 paramConfigs 必须一致；
     * 3) 如果存在“覆板”，其 paramConfigs 也必须一致；
     * 4) 校验通过后，按节点顺序提取所有来源工序流的最长公共前缀；
     * 5) 如果本次所有生产工件来源都包含“覆板”工艺，则生成的排版工序流必须包含“覆板”节点。
     */
    private ProcedureFlow validateAndBuildCommonProcedureFlow(List<ProductionPiece> productionPieces, List<TypesettingInfo> typesettingInfos) {
        List<ProcedureFlow> procedureFlows = new ArrayList<>();
        if (productionPieces != null) {
            procedureFlows.addAll(productionPieces.stream().map(ProductionPiece::getProcedureFlow).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (typesettingInfos != null) {
            procedureFlows.addAll(typesettingInfos.stream().map(TypesettingInfo::getProcedureFlow).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (procedureFlows.isEmpty()) {
            return null;
        }

        // 覆膜场景：既要校验是否与“不覆膜”冲突，也要校验膜参数一致性
        validateNodeConsistency(procedureFlows, "覆膜", "不覆膜", true);
        // 覆板场景：只校验覆板参数一致性
        validateNodeConsistency(procedureFlows, "覆板", null, false);

        List<List<ProcedureFlowNode>> nodeLists = procedureFlows.stream()
                .map(ProcedureFlow::getNodes)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (nodeLists.isEmpty()) {
            return null;
        }
        int minLen = nodeLists.stream().mapToInt(List::size).min().orElse(0);
        List<ProcedureFlowNode> commonNodes = new ArrayList<>();
        for (int i = 0; i < minLen; i++) {
            ProcedureFlowNode base = nodeLists.get(0).get(i);
            boolean allMatch = true;
            for (int j = 1; j < nodeLists.size(); j++) {
                ProcedureFlowNode candidate = nodeLists.get(j).get(i);
                if (!isSameNode(base, candidate)) {
                    allMatch = false;
                    break;
                }
            }
            if (!allMatch) {
                break;
            }
            commonNodes.add(base);
        }
        appendCoverBoardNodeIfAllProductionPiecesHaveIt(commonNodes, productionPieces);
        if (commonNodes.isEmpty()) {
            return null;
        }
        ProcedureFlow result = new ProcedureFlow();
        result.setNodes(new ArrayList<>(commonNodes));
        result.setTotalNodes(commonNodes.size());
        return result;
    }

    /**
     * 覆板生产工件生成排版后仍需要保留“覆板”工序。
     *
     * <p>最长公共前缀可能在“覆板”之前就结束，导致新印版的 procedureFlow 丢失覆板节点；
     * 当本次所有生产工件来源都包含覆板时，补入第一个生产工件上的覆板节点。若公共节点中已经
     * 包含覆板，则不重复添加。</p>
     */
    private void appendCoverBoardNodeIfAllProductionPiecesHaveIt(List<ProcedureFlowNode> commonNodes,
                                                                  List<ProductionPiece> productionPieces) {
        if (commonNodes == null || CollectionUtils.isEmpty(productionPieces)) {
            return;
        }
        if (commonNodes.stream().anyMatch(node -> node != null && "覆板".equals(node.getNodeName()))) {
            return;
        }
        boolean allProductionPiecesHaveCoverBoard = productionPieces.stream()
                .allMatch(piece -> piece != null && hasProcedureNode(piece.getProcedureFlow(), "覆板"));
        if (!allProductionPiecesHaveCoverBoard) {
            return;
        }
        productionPieces.stream()
                .map(ProductionPiece::getProcedureFlow)
                .map(flow -> findProcedureNode(flow, "覆板"))
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(commonNodes::add);
    }

    private ProcedureFlowNode findProcedureNode(ProcedureFlow procedureFlow, String nodeName) {
        if (procedureFlow == null || procedureFlow.getNodes() == null || StringUtils.isBlank(nodeName)) {
            return null;
        }
        return procedureFlow.getNodes().stream()
                .filter(node -> node != null && nodeName.equals(node.getNodeName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 节点一致性校验。
     * @param positiveNode 需要校验参数一致性的节点（例如 覆膜/覆板）
     * @param negativeNode 与 positiveNode 互斥的节点（例如 不覆膜）
     * @param checkOpposite 是否需要执行互斥校验
     */
    private void validateNodeConsistency(List<ProcedureFlow> flows, String positiveNode, String negativeNode, boolean checkOpposite) {
        List<ProcedureFlowNode> positiveNodes = new ArrayList<>();
        boolean hasPositive = false;
        boolean hasNegative = false;
        for (ProcedureFlow flow : flows) {
            List<ProcedureFlowNode> nodes = flow.getNodes();
            if (nodes == null) {
                continue;
            }
            for (ProcedureFlowNode node : nodes) {
                if (positiveNode.equals(node.getNodeName())) {
                    hasPositive = true;
                    positiveNodes.add(node);
                }
                if (checkOpposite && negativeNode != null && negativeNode.equals(node.getNodeName())) {
                    hasNegative = true;
                }
            }
        }
        if (checkOpposite && hasPositive && hasNegative) {
            throw new IllegalArgumentException("当前排版单元同时存在“" + positiveNode + "”和“" + negativeNode + "”，不能一起排版");
        }
        if (hasPositive) {
            Set<String> signatures = positiveNodes.stream().map(this::buildParamConfigSignature).collect(Collectors.toSet());
            if (signatures.size() > 1) {
                throw new IllegalArgumentException("当前排版单元“" + positiveNode + "”参数不一致，不能一起排版");
            }
        }
    }

    /**
     * 生成参数签名：将 paramConfigs 的 param 序列化后排序拼接，
     * 用于判断同一工序（覆膜/覆板）是否为同一种配置。
     */
    private String buildParamConfigSignature(ProcedureFlowNode node) {
        if (node == null || node.getParamConfigs() == null || node.getParamConfigs().isEmpty()) {
            return "";
        }
        return node.getParamConfigs().stream()
                .map(cfg -> (cfg == null ? "" : JSON.toJSONString(cfg.getParam())))
                .sorted()
                .collect(Collectors.joining("|"));
    }

    /**
     * 判断两个工序节点是否可视为同一节点（当前按 nodeName 比较）。
     */
    private boolean isSameNode(ProcedureFlowNode left, ProcedureFlowNode right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getNodeName(), right.getNodeName());
    }
    private String validateFilmConsistency(List<ProductionPiece> productionPieces, List<TypesettingInfo> typesettingInfos) {
        List<String> flows = new ArrayList<>();
        if (productionPieces != null) {
            flows.addAll(productionPieces.stream()
                    .map(ProductionPiece::getProcessingFlow)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList()));
        }
        if (typesettingInfos != null) {
            flows.addAll(typesettingInfos.stream()
                    .map(TypesettingInfo::getProcessingFlow)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList()));
        }
        if (flows.isEmpty()) {
            return "PASS";
        }
        boolean hasFilm = flows.stream()
                .flatMap(flow -> Arrays.stream(flow.split("-")))
                .map(String::trim)
                .anyMatch("覆膜"::equals);
        boolean hasNoFilm = flows.stream()
                .flatMap(flow -> Arrays.stream(flow.split("-")))
                .map(String::trim)
                .anyMatch("不覆膜"::equals);
        if (hasFilm && hasNoFilm) {
            return "当前排版单元包含“覆膜”和“不覆膜”，不能一起排版";
        }
        return "PASS";
    }

    private List<TypesettingSourceCell> toSourceCells(List<TypesettingProductionPieceVO> sourceCells) {
        if (sourceCells == null || sourceCells.isEmpty()) {
            return Collections.emptyList();
        }
        return sourceCells.stream()
                .filter(Objects::nonNull)
                .filter(cell -> StringUtils.isNotBlank(cell.getSourceType()) && StringUtils.isNotBlank(cell.getSourceId()))
                .map(cell -> {
                    TypesettingSourceCell sourceCell = new TypesettingSourceCell();
                    sourceCell.setSourceType(cell.getSourceType());
                    sourceCell.setSourceId(cell.getSourceId());
                    sourceCell.setOrderItemId(cell.getOrderItemId());
                    sourceCell.setQuantity(cell.getQuantity());
                    sourceCell.setIsRedo(cell.getIsRedo());
                    return sourceCell;
                })
                .collect(Collectors.toList());
    }

    private String resolvePieceNestingImg(ProductionPiece piece, boolean mirrorTypesettingTask) {
        if (StringUtils.isNotBlank(piece.getRouteImg())) {
            return piece.getRouteImg();
        }
        if (!mirrorTypesettingTask) {
            return piece.getTemplateCode();
        }
        if (piece.getMirrorConfigs() != null && !piece.getMirrorConfigs().isEmpty()) {
            MirrorConfig mirrorConfig = piece.getMirrorConfigs().get(0);
            if (mirrorConfig != null && StringUtils.isNotBlank(mirrorConfig.getImg())) {
                return mirrorConfig.getImg();
            }
        }
        return piece.getTemplateCode();
    }

    private List<TypesettingSourceCell> extractUsedSourceCells(String typesettingId, String nestedSvgUrl) {
        if (StringUtils.isBlank(typesettingId) || StringUtils.isBlank(nestedSvgUrl)) {
            return Collections.emptyList();
        }
        return extractUsedSourceCells(getCachedLayoutConfirmRequest(typesettingId), nestedSvgUrl);
    }

    private LayoutConfirmRequest getCachedLayoutConfirmRequest(String typesettingId) {
        if (StringUtils.isBlank(typesettingId)) {
            return null;
        }
        Object requestObj = redisTemplate.opsForValue().get(typesettingId);
        if (!(requestObj instanceof String)) {
            return null;
        }
        return JSON.parseObject((String) requestObj, LayoutConfirmRequest.class);
    }

    private List<TypesettingSourceCell> extractUsedSourceCells(LayoutConfirmRequest request, String nestedSvgUrl) {
        if (StringUtils.isBlank(nestedSvgUrl)) {
            return Collections.emptyList();
        }
        if (request == null || request.getTypesettingCells() == null || request.getTypesettingCells().isEmpty()) {
            return Collections.emptyList();
        }

        Path tempSvgPath = null;
        try {
            tempSvgPath = downloadNestedSvgToTempFile(nestedSvgUrl);
            if (tempSvgPath == null || !Files.exists(tempSvgPath)) {
                return Collections.emptyList();
            }
            String svgContent = Files.readString(tempSvgPath, StandardCharsets.UTF_8);
            if (StringUtils.isBlank(svgContent)) {
                return Collections.emptyList();
            }

            Map<String, Integer> sourceIdCountMap = new LinkedHashMap<>();
            Matcher matcher = SVG_SOURCE_INDEX_PATTERN.matcher(svgContent);
            while (matcher.find()) {
                String sourceId = matcher.group(1);
                sourceIdCountMap.put(sourceId, sourceIdCountMap.getOrDefault(sourceId, 0) + 1);
            }
            if (sourceIdCountMap.isEmpty()) {
                return Collections.emptyList();
            }

            List<TypesettingProductionPieceVO> sourceCells = request.getTypesettingCells();
            Set<String> markedSourceCellKeys = resolveMarkedSourceCellKeys(sourceIdCountMap.keySet(), sourceCells);
            Map<String, TypesettingSourceCell> usedCellMap = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : sourceIdCountMap.entrySet()) {
                String sourceId = entry.getKey();
                TypesettingProductionPieceVO matchedCell = findMatchedSourceCell(sourceId, sourceCells);
                if (matchedCell == null || StringUtils.isBlank(matchedCell.getSourceType()) || StringUtils.isBlank(matchedCell.getSourceId())) {
                    continue;
                }
                String sourceCellKey = buildSourceCellKey(matchedCell);
                if (markedSourceCellKeys.contains(sourceCellKey) && !isMarkedNestingElementId(sourceId)) {
                    continue;
                }
                TypesettingSourceCell usedCell = usedCellMap.computeIfAbsent(sourceCellKey, key -> {
                    return createUsedSourceCell(matchedCell);
                });
                usedCell.setQuantity((usedCell.getQuantity() == null ? 0 : usedCell.getQuantity()) + entry.getValue());
            }
            return new ArrayList<>(usedCellMap.values());
        } catch (Exception e) {
            System.err.println("解析 nestedSvg 失败: " + nestedSvgUrl + ", error=" + e.getMessage());
            return Collections.emptyList();
        } finally {
            if (tempSvgPath != null) {
                try {
                    Files.deleteIfExists(tempSvgPath);
                } catch (Exception ignore) {
                    System.err.println("删除临时 nestedSvg 文件失败: " + tempSvgPath);
                }
            }
        }
    }

    /**
     * 从 toLayout 请求快照创建回调使用的来源 cell。
     *
     * <p>存在拼接工艺的零件会携带 marks，并以 {@code marked-nesting-*} 外层元素参与排版；回调时原始
     * cell 会被该外层元素对应的新 cell 替换。普通零件通常还能凭原 sourceId 查询数据库中的 isRedo
     * 兜底，而拼接路径若不复制此字段，就会在这次替换中只丢失拼接零件的“重做”快照。</p>
     */
    static TypesettingSourceCell createUsedSourceCell(TypesettingProductionPieceVO matchedCell) {
        TypesettingSourceCell newCell = new TypesettingSourceCell();
        newCell.setSourceType(matchedCell.getSourceType());
        newCell.setSourceId(matchedCell.getSourceId());
        newCell.setOrderItemId(matchedCell.getOrderItemId());
        newCell.setQuantity(0);
        newCell.setIsRedo(matchedCell.getIsRedo());
        return newCell;
    }

    private Set<String> resolveMarkedSourceCellKeys(Set<String> nestedElementIds, List<TypesettingProductionPieceVO> sourceCells) {
        if (nestedElementIds == null || nestedElementIds.isEmpty() || sourceCells == null || sourceCells.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> markedSourceCellKeys = new HashSet<>();
        for (String nestedElementId : nestedElementIds) {
            if (!isMarkedNestingElementId(nestedElementId)) {
                continue;
            }
            TypesettingProductionPieceVO matchedCell = findMatchedSourceCell(nestedElementId, sourceCells);
            if (matchedCell != null) {
                markedSourceCellKeys.add(buildSourceCellKey(matchedCell));
            }
        }
        return markedSourceCellKeys;
    }

    private TypesettingProductionPieceVO findMatchedSourceCell(String nestedElementId, List<TypesettingProductionPieceVO> sourceCells) {
        if (StringUtils.isBlank(nestedElementId) || sourceCells == null || sourceCells.isEmpty()) {
            return null;
        }
        for (TypesettingProductionPieceVO cell : sourceCells) {
            if (matchesSourceCellId(nestedElementId, cell)) {
                return cell;
            }
        }
        return null;
    }

    private String buildSourceCellKey(TypesettingProductionPieceVO cell) {
        if (cell == null) {
            return "";
        }
        return String.join("|",
                StringUtils.isBlank(cell.getSourceType()) ? "" : cell.getSourceType(),
                StringUtils.isBlank(cell.getSourceId()) ? "" : cell.getSourceId(),
                StringUtils.isBlank(cell.getOrderItemId()) ? "" : cell.getOrderItemId());
    }

    private boolean isMarkedNestingElementId(String nestedElementId) {
        return StringUtils.isNotBlank(nestedElementId) && nestedElementId.startsWith("marked-nesting-");
    }

    /**
     * 判断 nestedSvg 中解析到的元素 ID 是否对应本次缓存的来源 cell。
     *
     * <p>带 marks 的生产工件提交给算法时，外层元素 ID 会被改写为
     * {@code marked-nesting-{productionPieceId}}，避免和预处理 SVG 内部原始 ID 重复。
     * callback 解析 nestedSvg 回填 cells 时需要把该特殊 ID 还原成原来源 cell，否则生成的印版会丢失
     * typesettingCells。</p>
     */
    private boolean matchesSourceCellId(String nestedElementId, TypesettingProductionPieceVO cell) {
        if (StringUtils.isBlank(nestedElementId) || cell == null) {
            return false;
        }
        if (Objects.equals(nestedElementId, cell.getId())
                || Objects.equals(nestedElementId, cell.getSourceId())) {
            return true;
        }
        String markedPrefix = "marked-nesting-";
        if (isMarkedNestingElementId(nestedElementId)) {
            String originalId = nestedElementId.substring(markedPrefix.length());
            return Objects.equals(originalId, cell.getId())
                    || Objects.equals(originalId, cell.getSourceId());
        }
        return false;
    }

    private Path downloadNestedSvgToTempFile(String nestedSvg) {
        if (StringUtils.isBlank(nestedSvg)) {
            return null;
        }
        try {
            String completeUrl = buildCompleteOssUrl(nestedSvg);
            if (completeUrl.startsWith("http://") || completeUrl.startsWith("https://")) {
                try {
                    byte[] svgBytes = restTemplate.getForObject(URI.create(completeUrl), byte[].class);
                    if (svgBytes != null && svgBytes.length > 0) {
                        Path tempFile = Files.createTempFile("nested-svg-", ".svg");
                        Files.write(tempFile, svgBytes, StandardOpenOption.TRUNCATE_EXISTING);
                        return tempFile;
                    }
                } catch (Exception ex) {
                    System.err.println("下载 nestedSvg 失败，尝试按本地文件读取: " + completeUrl + ", error=" + ex.getMessage());
                }
            }
            Path localPath = Path.of(nestedSvg);
            if (Files.exists(localPath)) {
                byte[] svgBytes = Files.readAllBytes(localPath);
                if (svgBytes.length > 0) {
                    Path tempFile = Files.createTempFile("nested-svg-", ".svg");
                    Files.write(tempFile, svgBytes, StandardOpenOption.TRUNCATE_EXISTING);
                    return tempFile;
                }
            }
            System.err.println("nestedSvg 不是可下载URL且本地文件不存在: " + nestedSvg);
            return null;
        } catch (Exception e) {
            System.err.println("读取 nestedSvg 失败: " + nestedSvg + ", error=" + e.getMessage());
            return null;
        }
    }

    private String buildCompleteOssUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String normalizedPath = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        if (StringUtils.isBlank(ossBucket) || StringUtils.isBlank(ossEndpoint)) {
            return normalizedPath;
        }
        return "https://" + ossBucket + "." + ossEndpoint + "/" + normalizedPath;
    }

    private String appendManufacturerMetaIdToUploadPath(String uploadPath, TypesettingInfo typesettingInfo) {
        if (StringUtils.isBlank(uploadPath) || typesettingInfo == null) {
            return uploadPath;
        }
        String manufacturerMetaId = typesettingInfo.getManufacturerMetaId();
        if (StringUtils.isBlank(manufacturerMetaId)) {
            return uploadPath;
        }
        String normalizedPath = uploadPath.trim();
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        if (!normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath + "/";
        }
        if (normalizedPath.startsWith("printingplate/")) {
            String typesettingId = typesettingInfo.getTypesettingId();
            if (StringUtils.isBlank(typesettingId)) {
                return normalizedPath + manufacturerMetaId + "/";
            }
            return normalizedPath + manufacturerMetaId + "/" + typesettingId + "/";
        }

        if (!normalizedPath.startsWith("forme/")) {
            return normalizedPath + manufacturerMetaId + "/";
        }

        String suffix = normalizedPath.substring("forme/".length());
        return "forme/" + manufacturerMetaId + "/" + suffix;
    }

    private String resolveFormeBusinessId(TypesettingInfo typesettingInfo, TypesettingLayoutMode layoutMode) {
        if (typesettingInfo == null) {
            return null;
        }
        if (isPrintingPlateLayoutMode(layoutMode)) {
            return typesettingInfo.getId();
        }
        return StringUtils.isNotBlank(typesettingInfo.getTypesettingId()) ? typesettingInfo.getTypesettingId() : typesettingInfo.getId();
    }

    private boolean isPrintingPlateLayoutMode(TypesettingLayoutMode layoutMode) {
        return layoutMode == TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CIRCLE
                || layoutMode == TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CROSS
                || layoutMode == TypesettingLayoutMode.RECT_TYPESETTING_PLT_QR_CIRCLE
                || layoutMode == TypesettingLayoutMode.RECT_TYPESETTING_PLT_QR_CROSS
                || layoutMode == TypesettingLayoutMode.GRID_TYPESETTING_PLT_QR_CIRCLE
                || layoutMode == TypesettingLayoutMode.GRID_TYPESETTING_PLT_QR_CROSS;
    }


    private String buildLayoutUploadPath(String manufacturerMetaId, String typesettingInfoId) {
        if (StringUtils.isBlank(typesettingInfoId)) {
            return "layout/";
        }
        if (StringUtils.isBlank(manufacturerMetaId)) {
            return "layout/" + typesettingInfoId + "/";
        }
        return "layout/" + manufacturerMetaId + "/" + typesettingInfoId + "/";
    }

    private void applyFormeGenerationResult(TypesettingInfo typesettingInfo, FormeGenerationResponse.Result formeResult) {
        if (typesettingInfo == null || formeResult == null) {
            return;
        }
        TypesettingElement element = typesettingInfo.getElement();
        if (element == null) {
            element = new TypesettingElement();
            typesettingInfo.setElement(element);
        }

        element.setJson(buildCompleteOssUrl(formeResult.getJson()));
        element.setFormeSvg(buildCompleteOssUrl(formeResult.getFormeSvg()));
        element.setPlt(convertPltObjectName(formeResult.getPlt()));
    }

    /**
     * 更新排版记录前合并库内已存在的 marks，避免“先清空再写入”导致历史 marks 丢失。
     * 说明：仅用于落库数据合并；不会改写 formeRequest 的 marks 拼接来源。
     */
    private void mergeExistingMarksBeforeUpdate(TypesettingInfo target) {
        if (target == null || StringUtils.isBlank(target.getId())) {
            return;
        }
        TypesettingInfo persisted = domainTypesettingService.findById(target.getId());
        if (persisted == null || persisted.getMarks() == null || persisted.getMarks().isEmpty()) {
            return;
        }
        LinkedHashMap<String, String> mergedMarks = new LinkedHashMap<>(persisted.getMarks());
        if (target.getMarks() != null && !target.getMarks().isEmpty()) {
            mergedMarks.putAll(target.getMarks());
        }
        target.setMarks(mergedMarks);
    }

    private TypesettingElement.PltObjectName convertPltObjectName(FormeGenerationResponse.PltObjectName plt) {
        if (plt == null) {
            return null;
        }
        return new TypesettingElement.PltObjectName(
                buildCompleteOssUrl(plt.getNormal()),
                buildCompleteOssUrl(plt.getReverse())
        );
    }

    /**
     * 仅生成二维码图片（Base64），不上传 OSS
     */
    public GenerateQrCodeResult generateQrCode(GenerateQrCodeRequest request) {
        if (request == null || StringUtils.isBlank(request.getContent())) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }
        if (StringUtils.isBlank(request.getManufacturerMetaId())) {
            throw new IllegalArgumentException("manufacturerMetaId 不能为空");
        }

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(request.getContent(), BarcodeFormat.QR_CODE, 512, 512);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] bytes = outputStream.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(bytes);

            GenerateQrCodeResult result = new GenerateQrCodeResult();
            result.setManufacturerMetaId(request.getManufacturerMetaId());
            result.setContent(request.getContent());
            result.setQrCodeBase64(base64);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("生成二维码失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成临时码:
     * 1) 每个 manufacturerMetaId 维护 1~100000 的循环序列
     * 2) 每次取队首数字，使用后放到队尾
     * 3) 临时码格式: xxx（即号码池中的数字字符串）
     */
    public GenerateTempCodeResult generateTempCode(GenerateTempCodeRequest request) {
        if (request == null || StringUtils.isBlank(request.getManufacturerMetaId())) {
            throw new IllegalArgumentException("manufacturerMetaId 不能为空");
        }
        String manufacturerMetaId = request.getManufacturerMetaId();
        String queueKey = TEMP_CODE_QUEUE_KEY_PREFIX + manufacturerMetaId;
        initTempCodeQueueIfAbsent(manufacturerMetaId, queueKey);

        Long codeNumber = rotateAndGetCodeNumber(queueKey);
        if (codeNumber == null) {
            initTempCodeQueueIfAbsent(manufacturerMetaId, queueKey);
            codeNumber = rotateAndGetCodeNumber(queueKey);
        }
        if (codeNumber == null) {
            throw new RuntimeException("临时码号码池为空，无法生成");
        }

        GenerateTempCodeResult result = new GenerateTempCodeResult();
        result.setManufacturerMetaId(manufacturerMetaId);
        result.setCodeNumber(codeNumber);
        result.setTempCode(String.valueOf(codeNumber));
        return result;
    }

    private void initTempCodeQueueIfAbsent(String manufacturerMetaId, String queueKey) {
        String initFlagKey = TEMP_CODE_QUEUE_INIT_KEY_PREFIX + manufacturerMetaId;
        Boolean firstInit = redisTemplate.opsForValue().setIfAbsent(initFlagKey, "1");
        if (Boolean.TRUE.equals(firstInit) || redisTemplate.opsForList().size(queueKey) == 0) {
            List<Object> initialNumbers = new ArrayList<>(TEMP_CODE_QUEUE_MAX);
            for (long i = 1; i <= TEMP_CODE_QUEUE_MAX; i++) {
                initialNumbers.add(i);
            }
            redisTemplate.delete(queueKey);
            redisTemplate.opsForList().rightPushAll(queueKey, initialNumbers);
        }
    }

    private Long rotateAndGetCodeNumber(String queueKey) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText(
                "local v = redis.call('LPOP', KEYS[1]); " +
                        "if (not v) then return nil end; " +
                        "redis.call('RPUSH', KEYS[1], v); " +
                        "return v;"
        );
        String code = redisTemplate.execute(script, Collections.singletonList(queueKey));
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return Long.parseLong(code);
    }

}
