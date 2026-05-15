package com.mes.application.command.typesetting;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.resp.NestingResponse;
import com.mes.application.command.api.resp.FormeGenerationResponse;
import com.mes.application.command.api.vo.CallbackCustomValue;
import com.mes.application.command.api.vo.UploadConfig;
import com.mes.application.command.typesetting.layout.FormeBuildContext;
import com.mes.application.command.typesetting.layout.FormeLayoutBuildResult;
import com.mes.application.command.typesetting.layout.TypesettingLayoutModeBuildService;
import com.mes.application.command.typesetting.layout.TypesettingLayoutModeConfirmService;
import com.mes.application.command.typesetting.strategy.MirrorFormeStrategy;
import com.mes.application.command.typesetting.strategy.NestingManifestStrategy;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.ConfirmPrintResult;
import com.mes.application.command.typesetting.vo.GenerateQrCodeResult;
import com.mes.application.command.typesetting.vo.GenerateTempCodeResult;
import com.mes.application.command.typesetting.vo.LayoutConfirmResult;
import com.mes.application.command.typesetting.vo.TypesettingLayoutModeVO;
import com.mes.application.command.typesetting.vo.ReleaseLayoutResult;
import com.mes.application.command.typesetting.vo.TypesettingLayoutSpecVO;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.req.typesetting.GenerateQrCodeRequest;
import com.mes.application.dto.req.typesetting.GenerateTempCodeRequest;
import com.mes.application.dto.TypesettingQuery;
import com.mes.application.dto.req.typesetting.ConfirmPrintRequest;
import com.mes.application.dto.req.typesetting.BatchConfirmLayoutRequest;
import com.mes.application.dto.req.typesetting.BatchConfirmPrintRequest;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingPrintTaskStatus;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingSequenceUsageType;
import com.mes.domain.manufacturer.typesetting.service.TypesettingPrintTaskService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingSequencePoolService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingDownloadTaskData;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
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
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
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
import java.util.Comparator;

@Slf4j
@Service
public class AppTypesettingService {

    @Autowired
    private TypesettingService domainTypesettingService;

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

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private List<TypesettingLayoutModeBuildService> layoutModeBuildServices;

    @Autowired(required = false)
    private List<TypesettingLayoutModeConfirmService> layoutModeConfirmServices;

    /**
     * layoutMode -> builder 的运行时映射表。
     * 在容器初始化完成后由 initLayoutModeBuilders 填充。
     */
    private final Map<TypesettingLayoutMode, TypesettingLayoutModeBuildService> layoutModeBuildServiceMap = new EnumMap<>(TypesettingLayoutMode.class);
    private final Map<TypesettingLayoutMode, TypesettingLayoutModeConfirmService> layoutModeConfirmServiceMap = new EnumMap<>(TypesettingLayoutMode.class);

    private static final String LAYOUT_CONFIRM_CACHE_PREFIX = "layout:confirm:";
    private static final long CACHE_EXPIRE_HOURS = 72;
    private static final DateTimeFormatter TYPESETTING_ID_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String TEMP_CODE_QUEUE_KEY_PREFIX = "typesetting:temp-code:queue:";
    private static final String TEMP_CODE_QUEUE_INIT_KEY_PREFIX = "typesetting:temp-code:init:";
    private static final int TEMP_CODE_QUEUE_MAX = 100000;
    private static final Pattern SVG_SOURCE_INDEX_PATTERN = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"");
    private static final int TAG_STRIP_HEIGHT_MM = 20;
    private static final List<TypesettingLayoutSpecVO> DEFAULT_LAYOUT_SPECS = List.of(
            new TypesettingLayoutSpecVO("1200*2400", 1200, 2400),
            new TypesettingLayoutSpecVO("1200*3000", 1200, 3000),
            new TypesettingLayoutSpecVO("1040*2403", 1040, 2403),
            new TypesettingLayoutSpecVO("1040*3210", 1040, 3210),
            new TypesettingLayoutSpecVO("1040*9950", 1040, 9950),
            new TypesettingLayoutSpecVO("1270*50000", 1270, 50000)
    );

    @PostConstruct
    public void initLayoutModeBuilders() {
        // 将所有模式构建器注册到 map，供 confirmLayout 阶段按 mode 分发调用
        if (layoutModeBuildServices == null) {
            return;
        }
        for (TypesettingLayoutModeBuildService buildService : layoutModeBuildServices) {
            layoutModeBuildServiceMap.put(buildService.supportMode(), buildService);
        }
        if (layoutModeConfirmServices == null) {
            return;
        }
        for (TypesettingLayoutModeConfirmService confirmService : layoutModeConfirmServices) {
            layoutModeConfirmServiceMap.put(confirmService.supportMode(), confirmService);
        }
    }

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


    /**
     * 统一查询接口
     * @param query 查询参数
     * @return 分页结果
     */
    public PagedResult<TypesettingProductionPieceVO> findTypesettingAndProductionPieces(TypesettingQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
        if (StringUtils.isBlank(query.getManufacturerMetaId())) {
            throw new IllegalArgumentException("manufacturerMetaId 不能为空");
        }

        List<TypesettingProductionPieceVO> items = new ArrayList<>();
        boolean queryPartOnly = TypesettingSourceType.PART.getCode().equals(query.getSourceType());
        boolean queryTypesettingOnly = TypesettingSourceType.TYPESETTING.getCode().equals(query.getSourceType());

        if (!queryTypesettingOnly) {
            List<ProductionPiece> productionPieces = productionPieceService.findProductionPiecesByConditions(
                    query.getManufacturerMetaId(),
                    null,
                    query.getMaterialName(),
                    query.getProcessingName(),
                    query.getStartTime(),
                    query.getEndTime(),
                    1,
                    Integer.MAX_VALUE
            );
            for (ProductionPiece piece : productionPieces) {
                if (getPendingTypesettingQuantity(piece) > 0) {
                    items.add(TypesettingProductionPieceVO.fromProductionPiece(piece));
                }
            }
        }

        if (!queryPartOnly) {
            List<TypesettingInfo> typesettingInfos = domainTypesettingService.findTypesettingByConditions(
                    query.getManufacturerMetaId(),
                    null,
                    query.getMaterialName(),
                    query.getProcessingName(),
                    query.getStartTime(),
                    query.getEndTime(),
                    null,
                    1,
                    Integer.MAX_VALUE
            );
            for (TypesettingInfo info : typesettingInfos) {
                Integer leaveQuantity = info.getLeaveQuantity() == null ? 0 : info.getLeaveQuantity();
                boolean isPending = TypesettingStatus.PENDING.getCode().equals(info.getStatus());
                if (leaveQuantity > 0 && isPending) {
                    items.add(TypesettingProductionPieceVO.fromTypesettingInfo(info));
                }
            }
        }

        items.sort(Comparator.comparing(TypesettingProductionPieceVO::getCreateTime,
                Comparator.nullsLast(Date::compareTo)).reversed());

        long total = items.size();
        return new PagedResult<>(items, total, items.size(), 1);
    }

    /**
     * 查询默认排版规格
     */
    public List<TypesettingLayoutSpecVO> listDefaultLayoutSpecs() {
        return DEFAULT_LAYOUT_SPECS;
    }

    /**
     * 查询所有排版方式（完整对象）
     */
    public List<TypesettingLayoutModeVO> listLayoutModes() {
        return Arrays.stream(TypesettingLayoutMode.values())
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

        long total = allTypesettingInfos.size();

        return new PagedResult<>(pagedTypesettingInfos, total, size, current);
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
     * 只查询零件（待排版状态）
     */
    private List<TypesettingProductionPieceVO> queryPartsOnly(TypesettingQuery query) {
        // 不走分页查询：先按 manufacturerId 查询符合基础条件的全部零件，再在内存中过滤“待排版数量>0”
        List<ProductionPiece> parts = productionPieceService.findProductionPiecesByConditions(
                query.getManufacturerMetaId(),
                ProductionPieceStatus.PENDING_TYPESITTING.getCode(),
                query.getMaterialName(),
                query.getProcessingName(),
                query.getStartTime(),
                query.getEndTime(),
                1,
                Integer.MAX_VALUE
        );

        // 转换为 VO
        List<TypesettingProductionPieceVO> voList = new ArrayList<>();
        for (ProductionPiece piece : parts) {
            if (getPendingTypesettingQuantity(piece) <= 0) {
                continue;
            }
            TypesettingProductionPieceVO vo = TypesettingProductionPieceVO.fromProductionPiece(piece);
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
        List<TypesettingInfo> typesettings = domainTypesettingService.findTypesettingByConditions(
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

    private static BigDecimal ceilBigDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(0, RoundingMode.CEILING);
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
     * 确认排版：校验材料和工艺，调用 API 生成排版结果，并更新生产工件状态
     * @return 排版结果
     */
    public LayoutConfirmResult toLayout(LayoutConfirmRequest request) {
        List<ProductionPiece> productionPieces = new ArrayList<>();
        List<TypesettingInfo> typesettingInfos = new ArrayList<>();
        List<TypesettingProductionPieceVO> typesettingCells = request.getTypesettingCells();
        if (typesettingCells == null) {
            typesettingCells = new ArrayList<>();
        }
        for (TypesettingProductionPieceVO cell : typesettingCells) {
            if (cell == null || StringUtils.isBlank(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            if (TypesettingSourceType.PART.getCode().equals(cell.getSourceType())) {
                ProductionPiece productionPiece = cell.toProductionPiece();
                ProductionPiece dbPiece = productionPieceService.findById(productionPiece.getId());
                if (dbPiece == null) {
                    throw new IllegalArgumentException("生产工件不存在：" + productionPiece.getProductionPieceId());
                }
                if (productionPiece.getQuantity() != null) {
                    dbPiece.setQuantity(productionPiece.getQuantity());
                }
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
            List<ProcedureFlowNode> nodes = productionPiece.getProcedureFlow().getNodes();
            for (ProcedureFlowNode node : nodes) {
                if (node.getNodeName().equals("排版")) {
                    if (node.getPieceQuantity() < quantity){
                        return LayoutConfirmResult.failed(productionPiece.getProductionPieceId()+"可排版数量不足");
                    }
                }
            }
        }

        String filmConsistencyResult = validateFilmConsistency(productionPieces, typesettingInfos);
        if (!filmConsistencyResult.equals("PASS")) {
            return LayoutConfirmResult.failed(filmConsistencyResult);
        }

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
        log.info("nestingRequest========:{}",JSON.toJSONString(nestingRequest));
        TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(request.getLayoutMode());
        NestingResponse nestingResponse;
        switch (layoutMode.getLayoutCategory()) {
            case "grid_typesetting":
                nestingResponse = algorithmCoreApiService.generateGridNestedFilesAsync(nestingRequest);
                break;
            case "vertical_typesetting":
                nestingResponse = algorithmCoreApiService.generateVerticalNestedFilesAsync(nestingRequest);
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
                System.err.println("更新生产工件 " + piece.getId() + " 节点数量失败：" + e.getMessage());
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
        if (StringUtils.isNotBlank(request.getLayoutMode())) {
            typesettingInfo.setLayoutMode(request.getLayoutMode());
        } else if (!typesettingInfos.isEmpty()) {
            typesettingInfo.setLayoutMode(typesettingInfos.get(0).getLayoutMode());
        }
        typesettingInfo.setTypesettingCells(toSourceCells(request.getTypesettingCells()));
        domainTypesettingService.addTypesetting(typesettingInfo);
        return result;
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
     * <p>说明：圆形二维码模式（shaped_cutting_plt_qr_circle / grid_typesetting_plt_qr_circle）
     * 依赖 manufacturerMetaId 生成队列码与二维码。
     */
    public LayoutConfirmResult confirmLayout(TypesettingInfo request) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            throw new IllegalArgumentException("确认排版参数不能为空，且必须包含排版ID");
        }
        TypesettingInfo typesettingInfo = domainTypesettingService.findById(request.getId());
        if (typesettingInfo == null) {
            throw new IllegalArgumentException("排版信息不存在：" + request.getId());
        }
        validateNoSecondaryTypesettingCells(typesettingInfo);

        TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(
                StringUtils.isNotBlank(request.getLayoutMode()) ? request.getLayoutMode() : typesettingInfo.getLayoutMode()
        );
        if (typesettingInfo.getElement() == null || StringUtils.isBlank(typesettingInfo.getElement().getNestedSvg())) {
            throw new IllegalArgumentException("排版信息缺少 nestedSvg，无法确认排版");
        }
        if (requireManufacturerMetaId(layoutMode) && StringUtils.isBlank(typesettingInfo.getManufacturerMetaId())) {
            throw new IllegalArgumentException("圆形定位点排版缺少 manufacturerMetaId，无法生成队列编号与二维码");
        }
        if (typesettingInfo.getElement() == null || StringUtils.isBlank(typesettingInfo.getElement().getNestedSvg())) {
            throw new IllegalArgumentException("排版信息缺少 nestedSvg，无法确认排版");
        }
        typesettingInfo.setLayoutMode(layoutMode.getCode());
        typesettingInfo.applyLayoutModeConfig();

        String businessId = resolveFormeBusinessId(typesettingInfo, layoutMode);
        FormeGenerationRequest formeRequest = buildFormeGenerationRequest(typesettingInfo, layoutMode, businessId);
        mergeAnchorPointMarks(typesettingInfo, formeRequest);
        String formeRequestJson = JSON.toJSONString(formeRequest);
        log.info("formeRequest========:{}", formeRequestJson);
        algorithmCoreApiService.generateFormeAsync(formeRequestJson, formeRequest.getCallbackConfig().getCallbackUrl());

        String formeOpRemark = "FORME_OP:LAYOUT";
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
                    businessId + "-mirror"
            );
            mergeAnchorPointMarks(mirrorTypesettingInfo, mirrorFormeRequest);
            // 镜像印版由 DoubleSideMountingLayoutBuildService 回填了 marks，这里同步落库
            domainTypesettingService.updateTypesetting(mirrorTypesettingInfo);
            String mirrorFormeRequestJson = JSON.toJSONString(mirrorFormeRequest);
            log.info("mirrorFormeRequest========:{}", mirrorFormeRequestJson);
            algorithmCoreApiService.generateFormeAsync(mirrorFormeRequestJson, mirrorFormeRequest.getCallbackConfig().getCallbackUrl());
        }

        // 异步处理中，先进入确认中状态，回调成功后再走后续逻辑
        typesettingInfo.setStatus(TypesettingStatus.CONFIRMED.getCode());
        typesettingInfo.setRemark(formeOpRemark);
        domainTypesettingService.updateTypesetting(typesettingInfo);

        LayoutConfirmResult result = new LayoutConfirmResult();
        result.setSuccess(true);
        result.setMessage("确认排版任务已提交，等待回调");
        return result;
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
        formeInfo.setSvgUrl(buildCompleteOssUrl(typesettingInfo.getElement().getNestedSvg()));
        formeInfo.setMargin(modeResult.getMargin());
        formeInfo.setMarks(modeResult.getMarks());
        formeInfo.setAnchorPoints(modeResult.getAnchorPoints());
        request.setForme(formeInfo);
        request.setOutputs(modeResult.getOutputs());

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

    private void ensureMirrorTypesettingExists(TypesettingInfo mirrorTypesettingInfo) {
        if (mirrorTypesettingInfo == null || StringUtils.isBlank(mirrorTypesettingInfo.getTypesettingId())) {
            return;
        }
        TypesettingInfo existing = domainTypesettingService.findTypesettingByTypesettingId(mirrorTypesettingInfo.getTypesettingId());
        if (existing == null) {
            mirrorTypesettingInfo.setId(null);
            TypesettingInfo created = domainTypesettingService.addTypesetting(mirrorTypesettingInfo);
            if (created != null && StringUtils.isNotBlank(created.getId())) {
                mirrorTypesettingInfo.setId(created.getId());
            } else {
                TypesettingInfo persisted = domainTypesettingService.findTypesettingByTypesettingId(mirrorTypesettingInfo.getTypesettingId());
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
     * 提取元素 A（typesetting 来源标识）。
     *
     * <p>优先读取 typesettingCells 中 sourceType=typesetting 的 sourceId；
     * 兜底 typesettingId，再兜底当前记录 id。
     */
    private String extractElementA(TypesettingInfo typesettingInfo) {
        if (typesettingInfo.getTypesettingCells() != null) {
            for (TypesettingSourceCell cell : typesettingInfo.getTypesettingCells()) {
                if (cell == null) {
                    continue;
                }
                if (TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType()) && StringUtils.isNotBlank(cell.getSourceId())) {
                    return cell.getSourceId();
                }
            }
        }
        return StringUtils.isNotBlank(typesettingInfo.getTypesettingId()) ? typesettingInfo.getTypesettingId() : typesettingInfo.getId();
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
                    if (piece.getQuantity() != null) {
                        dbPiece.setQuantity(piece.getQuantity());
                    }
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
        List<NestingRequest.Element> elements = new ArrayList<>();
        if (productionPieces != null) {
            for (ProductionPiece piece : productionPieces) {
                if (piece == null || StringUtils.isBlank(piece.getProductionPieceId())) {
                    continue;
                }
                if (StringUtils.isBlank(piece.getTemplateCode())) {
                    throw new IllegalArgumentException("生产工件缺少排版SVG地址：" + piece.getProductionPieceId());
                }
                NestingRequest.Element element = new NestingRequest.Element();
                element.setId(piece.getId());
                if (piece.getMaskImageFile() != null && piece.getMaskImageFile().getRawFile() != null) {
                    element.setSvg(piece.getMaskImageFile().getRawFile());
                }
                element.setCounts(piece.getQuantity() != null && piece.getQuantity() > 0 ? piece.getQuantity() : 1);
                element.setForme(Boolean.FALSE);
                String pieceImg = resolvePieceNestingImg(piece, mirrorTypesettingTask);
                if (StringUtils.isNotBlank(pieceImg)) {
                    element.setImg(pieceImg);
                }
                if (isVerticalTypesetting) {
                    element.setVMargin(0);
                    element.setHGravity("left");
                    element.setHMargin(0);
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
        manifest.setSpacing(layoutMode.getNestingSpacingMm());
        manifest.setRequirePlt(Boolean.TRUE);
        manifest.setMirrorAppend(Boolean.FALSE);
        manifest.setMirrorRequirePlt(Boolean.FALSE);
        manifest.setContainers(containers);
        manifest.setElements(elements);
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
     * 确认打印：将排版数据根据状态机改为待打印状态
     *
     * @param request 确认打印请求（排版ID、可选layoutMode、deviceCode）
     * @return 操作结果
     */
    public ConfirmPrintResult confirmPrint(ConfirmPrintRequest request) {
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
        if (typesettingInfo.getElement() == null || StringUtils.isBlank(typesettingInfo.getElement().getNestedSvg())) {
            throw new RuntimeException("排版信息缺少 nestedSvg，无法确认打印");
        }

        TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(
                StringUtils.isNotBlank(request.getLayoutMode()) ? request.getLayoutMode() : typesettingInfo.getLayoutMode()
        );
        if (requireManufacturerMetaId(layoutMode) && StringUtils.isBlank(typesettingInfo.getManufacturerMetaId())) {
            throw new RuntimeException("圆形定位点排版缺少 manufacturerMetaId，无法生成队列编号与二维码");
        }
        typesettingInfo.setLayoutMode(layoutMode.getCode());
        typesettingInfo.applyLayoutModeConfig();

        String businessId = resolveFormeBusinessId(typesettingInfo, layoutMode);
        FormeGenerationRequest formeRequest = buildFormeGenerationRequest(typesettingInfo, layoutMode, businessId);
        mergeAnchorPointMarks(typesettingInfo, formeRequest);
        String formeRequestJson = JSON.toJSONString(formeRequest);
        log.info("formeRequest-print========:{}", formeRequestJson);
        FormeGenerationResponse response = algorithmCoreApiService.generateFormeAsync(formeRequestJson, formeRequest.getCallbackConfig().getCallbackUrl());
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
                    businessId + "-mirror"
            );
            mergeAnchorPointMarks(mirrorTypesettingInfo, mirrorFormeRequest);
            // 镜像印版由 DoubleSideMountingLayoutBuildService 回填了 marks，这里同步落库
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
        domainTypesettingService.updateTypesetting(typesettingInfo);

        ConfirmPrintResult result = new ConfirmPrintResult();
        result.setSuccess(true);
        result.setMessage("确认打印任务已提交，等待回调");
        return result;
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
            return;
        }
        String recordId = null;
        if (response.getCallbackConfig() != null && response.getCallbackConfig().getCallbackCustomValue() != null) {
            recordId = response.getCallbackConfig().getCallbackCustomValue().getId();
        }
        if (StringUtils.isBlank(recordId)) {
            recordId = response.getId();
        }
        if (StringUtils.isBlank(recordId)) {
            return;
        }
        TypesettingInfo typesettingInfo = domainTypesettingService.findById(recordId);
        if (typesettingInfo == null) {
            return;
        }
        if (!"success".equalsIgnoreCase(response.getStatus())) {
            typesettingInfo.setStatus(TypesettingStatus.FAILED.getCode());
            typesettingInfo.setRemark(StringUtils.isNotBlank(response.getError()) ? response.getError() : "印版异步生成失败");
            domainTypesettingService.updateTypesetting(typesettingInfo);
            return;
        }
        applyFormeGenerationResult(typesettingInfo, response.getResult());
        if (StringUtils.isBlank(typesettingInfo.getTemplateCode())) {
            typesettingInfo.setTemplateCode(buildTemplateCode(1, 1));
        }
        String remark = typesettingInfo.getRemark();
        if ("FORME_OP:LAYOUT".equals(remark)) {
            typesettingInfo.setStatus(TypesettingStatus.PENDING.getCode());
            typesettingInfo.setRemark(null);
            domainTypesettingService.updateTypesetting(typesettingInfo);
            return;
        }
        log.info("开始进行打印印版回调参数remark,{}", JSON.toJSONString(remark));
        try {
            if (remark != null && remark.startsWith("FORME_OP:PRINT:")) {
                log.info("开始进行打印印版回调参数,{}", JSON.toJSONString(typesettingInfo));
                String deviceCode = remark.substring("FORME_OP:PRINT:".length());
                typesettingInfo.setStatus(TypesettingStatus.PRINTING.getCode());
                typesettingInfo.setDeviceCode(deviceCode);
                typesettingInfo.setLeaveQuantity(1);
                Set<String> visitedTypesettingKeys = new HashSet<>();
                Map<String, Integer> productionPieceUsage = new LinkedHashMap<>();
                collectProductionPieceUsage(typesettingInfo, 1, visitedTypesettingKeys, productionPieceUsage);
                int plateUseCount = typesettingInfo.getLeaveQuantity() != null && typesettingInfo.getLeaveQuantity() > 0
                        ? typesettingInfo.getLeaveQuantity() : 1;
                String callbackTypesettingId = StringUtils.isNotBlank(typesettingInfo.getTypesettingId())
                        ? typesettingInfo.getTypesettingId() : typesettingInfo.getId();
                if (callbackTypesettingId == null || !callbackTypesettingId.contains("-Mirror")) {
                    transferTypesettingQuantityToPrinting(productionPieceUsage, plateUseCount);
                }
                Set<String> productionPieceIds = productionPieceUsage.keySet();
                String printTaskTypesettingId = callbackTypesettingId;
                String deviceInfoId = resolveDeviceInfoIdByDeviceCode(typesettingInfo.getManufacturerMetaId(), deviceCode);
                Map<String, String> allMarks = collectTypesettingMarks(typesettingInfo);
                TypesettingDownloadTaskData downloadTaskData = buildDownloadTaskData(
                        printTaskTypesettingId,
                        deviceInfoId,
                        deviceCode,
                        typesettingInfo.getElement(),
                        allMarks,
                        productionPieceIds);
                typesettingInfo.setRemark(null);
                domainTypesettingService.updateTypesetting(typesettingInfo);
                TypesettingDownloadTaskData nonPltData = copyDownloadTaskDataWithoutPlts(downloadTaskData);
                savePrintTaskByDeviceCode(printTaskTypesettingId, typesettingInfo.getManufacturerMetaId(), deviceCode, nonPltData);
                savePltBroadcastPrintTask(printTaskTypesettingId, typesettingInfo.getManufacturerMetaId(), downloadTaskData);
            }
        }catch (Exception e) {
            log.error("处理打印印版回调异常", e);
        }

    }

    private boolean requireManufacturerMetaId(TypesettingLayoutMode layoutMode) {
        return TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CIRCLE == layoutMode
                || TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CROSS == layoutMode
                || TypesettingLayoutMode.GRID_TYPESETTING_PLT_QR_CIRCLE == layoutMode;
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

    private void collectProductionPieceUsage(TypesettingInfo typesettingInfo,
                                             int multiplier,
                                             Set<String> visitedTypesettingKeys,
                                             Map<String, Integer> productionPieceUsage) {
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
                productionPieceUsage.merge(cell.getSourceId(), currentMultiplier, Integer::sum);
                continue;
            }
            if (!TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                continue;
            }
            TypesettingInfo nestedById = domainTypesettingService.findById(cell.getSourceId());
            List<TypesettingInfo> nestedTypesettingInfos = nestedById == null
                    ? Collections.emptyList()
                    : Collections.singletonList(nestedById);
            for (TypesettingInfo nestedInfo : nestedTypesettingInfos) {
                if (nestedInfo.getTypesettingId() == null || !nestedInfo.getTypesettingId().contains("-Mirror")) {
                    continue;
                }
                collectProductionPieceUsage(nestedInfo, currentMultiplier, visitedTypesettingKeys, productionPieceUsage);
            }
        }
        visitedTypesettingKeys.remove(currentKey);
    }

    private void transferTypesettingQuantityToPrinting(Map<String, Integer> productionPieceUsage, int plateUseCount) {
        if (productionPieceUsage == null || productionPieceUsage.isEmpty() || plateUseCount <= 0) {
            return;
        }
        for (Map.Entry<String, Integer> entry : productionPieceUsage.entrySet()) {
            String productionPieceRecordId = entry.getKey();
            int requiredQuantity = entry.getValue() * plateUseCount;
            if (requiredQuantity <= 0) {
                continue;
            }
            ProductionPiece piece = productionPieceService.findById(productionPieceRecordId);
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
            typesettingNode.setPieceQuantity(typesettingQuantity - requiredQuantity);
            if (typesettingNode.getPieceQuantity() <= 0) {
                typesettingNode.setNodeStatus(NodeStatus.COMPLETED);
            }
            int printingQuantity = printingNode.getPieceQuantity() == null ? 0 : printingNode.getPieceQuantity();
            printingNode.setPieceQuantity(printingQuantity + requiredQuantity);
            printingNode.setNodeStatus(NodeStatus.PENDING);
            productionPieceService.updateProductionPiece(piece);
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
                                                              Set<String> productionPieceIds) {
        LinkedHashSet<String> imageSet = new LinkedHashSet<>();
        boolean isMirrorTypesetting = StringUtils.isNotBlank(typesettingInfoId) && typesettingInfoId.contains("-Mirror");
        for (String productionPieceId : productionPieceIds) {
            ProductionPiece piece = productionPieceService.findById(productionPieceId);
            if (piece == null) {
                continue;
            }
            appendRawFile(imageSet, piece.getProductImageFile() == null ? null : piece.getProductImageFile().getRawFile());
            if (isMirrorTypesetting) {
                if (piece.getMirrorConfigs() != null && !piece.getMirrorConfigs().isEmpty()) {
                    appendRawFile(imageSet, piece.getMirrorConfigs().get(0).getImg());
                }
            } else {
                appendRawFile(imageSet, piece.getProductImageFile() == null ? null : piece.getProductImageFile().getRawFile());
            }
            appendRawFile(imageSet, piece.getMaskImageFile() == null ? null : piece.getMaskImageFile().getRawFile());
        }
        LinkedHashSet<String> pltSet = new LinkedHashSet<>();
        LinkedHashSet<String> jsonSet = new LinkedHashSet<>();
        LinkedHashSet<String> markSet = new LinkedHashSet<>();
        if (typesettingElement != null) {
            if (typesettingElement.getPlt() != null) {
                appendRawFile(pltSet, typesettingElement.getPlt().getNormal());
                appendRawFile(pltSet, typesettingElement.getPlt().getReverse());
            }
            appendRawFile(jsonSet, typesettingElement.getJson());
        }
        TypesettingInfo typesettingInfo = domainTypesettingService.findById(typesettingInfoId);
        collectCellTypesettingPltsRecursive(typesettingInfo, pltSet, new HashSet<>());
        if (marks != null && !marks.isEmpty()) {
            for (String markFile : marks.values()) {
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

    private void collectCellTypesettingPltsRecursive(TypesettingInfo typesettingInfo,
                                                     Set<String> pltSet,
                                                     Set<String> visitedIds) {
        if (typesettingInfo == null || StringUtils.isBlank(typesettingInfo.getId()) || visitedIds.contains(typesettingInfo.getId())) {
            return;
        }
        visitedIds.add(typesettingInfo.getId());
        if (typesettingInfo.getTypesettingCells() == null) {
            return;
        }
        for (TypesettingSourceCell cell : typesettingInfo.getTypesettingCells()) {
            if (cell == null || !TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            TypesettingInfo cellTypesetting = domainTypesettingService.findById(cell.getSourceId());
            if (cellTypesetting == null) {
                continue;
            }
            TypesettingElement cellElement = cellTypesetting.getElement();
            if (cellElement != null && cellElement.getPlt() != null) {
                appendRawFile(pltSet, cellElement.getPlt().getNormal());
                appendRawFile(pltSet, cellElement.getPlt().getReverse());
            }
            collectCellTypesettingPltsRecursive(cellTypesetting, pltSet, visitedIds);
        }
    }

    private void appendRawFile(Set<String> container, String fileUrl) {
        if (StringUtils.isNotBlank(fileUrl)) {
            container.add(fileUrl);
        }
    }

    private void appendMarkFiles(Set<String> container, String markFileUrl) {
        appendRawFile(container, markFileUrl);
        if (StringUtils.isBlank(markFileUrl)) {
            return;
        }
        String lower = markFileUrl.toLowerCase(Locale.ROOT);
        if (lower.contains("/basetag/") && lower.endsWith(".svg")) {
            String pngUrl = markFileUrl.substring(0, markFileUrl.length() - 4) + ".png";
            appendRawFile(container, pngUrl);
        }
    }


    private void savePrintTaskByDeviceCode(String typesettingInfoId,
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
        savePrintTask(typesettingInfoId, manufacturerMetaId, Collections.singletonList(deviceInfoId), Collections.singletonList(resolvedDeviceCode), data);
    }

    private void savePrintTask(String typesettingInfoId, String deviceInfoId, TypesettingDownloadTaskData data) {
        savePrintTask(typesettingInfoId, null, Collections.singletonList(deviceInfoId), Collections.emptyList(), data);
    }

    private void savePrintTask(String typesettingInfoId,
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
            data.setDeviceInfoIds(normalizedDeviceInfoIds);
            data.setDeviceCodes(normalizedDeviceCodes);
            if (StringUtils.isBlank(data.getDeviceInfoId()) && !normalizedDeviceInfoIds.isEmpty()) {
                data.setDeviceInfoId(normalizedDeviceInfoIds.get(0));
            }
        }
        TypesettingPrintTask task = new TypesettingPrintTask();
        task.setTypesettingInfoId(typesettingInfoId);
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
                                           String manufacturerMetaId,
                                           TypesettingDownloadTaskData originalData) {
        if (originalData == null || originalData.getPlts() == null || originalData.getPlts().isEmpty()) {
            return;
        }
        List<ManufacturerDeviceCfg> cuttingDeviceCfgs = findCuttingDeviceCfgs(manufacturerMetaId);
        if (cuttingDeviceCfgs.isEmpty()) {
            return;
        }
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
        if (cuttingDeviceInfoIds.isEmpty()) {
            return;
        }
        TypesettingDownloadTaskData pltOnlyData = new TypesettingDownloadTaskData();
        pltOnlyData.setId(originalData.getId());
        pltOnlyData.setDeviceInfoId(originalData.getDeviceInfoId());
        pltOnlyData.setDeviceInfoIds(new ArrayList<>(cuttingDeviceInfoIds));
        pltOnlyData.setDeviceCodes(new ArrayList<>(cuttingDeviceCodes));
        pltOnlyData.setImamges(Collections.emptyList());
        pltOnlyData.setPlts(new ArrayList<>(originalData.getPlts()));
        pltOnlyData.setJsons(Collections.emptyList());
        pltOnlyData.setMarks(Collections.emptyList());
        savePrintTask(typesettingInfoId + "_plt", manufacturerMetaId, new ArrayList<>(cuttingDeviceInfoIds), new ArrayList<>(cuttingDeviceCodes), pltOnlyData);
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

        for (String typesettingId : typesettingIds) {
            if (StringUtils.isBlank(typesettingId)) {
                continue;
            }
            TypesettingInfo info = domainTypesettingService.findById(typesettingId);
            if (info == null || StringUtils.isBlank(info.getId())) {
                errorMessages.add("排版记录不存在: " + typesettingId);
                continue;
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
                    productionPieceRollbackQuantity.merge(usedCell.getSourceId(), usedQuantity, Integer::sum);
                } else if (TypesettingSourceType.TYPESETTING.getCode().equals(usedCell.getSourceType())) {
                    typesettingRollbackQuantity.merge(usedCell.getSourceId(), usedQuantity, Integer::sum);
                }
            }

            try {
                domainTypesettingService.deleteTypesetting(info.getId());
                deletedLayoutIds.add(info.getId());
            } catch (Exception e) {
                errorMessages.add("删除排版记录失败(" + info.getId() + "): " + e.getMessage());
                continue;
            }

            if (StringUtils.isBlank(info.getTypesettingId())) {
                continue;
            }
            TypesettingInfo mirrorTypesetting = domainTypesettingService.findTypesettingByTypesettingId(info.getTypesettingId() + "-Mirror");
            if (mirrorTypesetting == null || StringUtils.isBlank(mirrorTypesetting.getId())) {
                continue;
            }
            try {
                domainTypesettingService.deleteTypesetting(mirrorTypesetting.getId());
                deletedLayoutIds.add(mirrorTypesetting.getId());
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
        String firstMaterialId = firstMaterial.getMaterialId();
        for (int i = 1; i < productionPieces.size(); i++) {
            MaterialConfig material = productionPieces.get(i).getMaterialConfig();
            if (material == null) {
                return "零件 " + productionPieces.get(i).getProductionPieceId() + " 的材料为空";
            }
            String materialId = material.getMaterialId();
            if (!firstMaterialId.equals(materialId)) {
                return "材料不一致：零件 " + productionPieces.get(0).getProductionPieceId() +
                        " 的材料为 " + firstMaterial.getMaterialSnapshot().getName() +
                        "，零件 " + productionPieces.get(i).getProductionPieceId() +
                        " 的材料为 " + material.getMaterialSnapshot().getName();
            }
        }

        return "PASS";
    }

    /**
     * 校验特殊工艺（覆板、双面对裱）的材料一致性
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

    private boolean isSpecialProcedureNode(ProcedureFlowNode node) {
        return node != null && ("覆板".equals(node.getNodeName()));
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
        List<TypesettingInfo> typesettingInfos = domainTypesettingService.findTypesettingListByTypesettingId(typesettingId);
        if (typesettingInfos == null || typesettingInfos.isEmpty()) {
            throw new RuntimeException("排版信息不存在：" + typesettingId);
        }

        TypesettingInfo baseTypesettingInfo = typesettingInfos.get(0);
        if ("success".equals(response.getStatus())) {
            List<NestingResponse.Result> results = response.getResults();
            if (results == null || results.isEmpty()) {
                throw new RuntimeException("排版回调成功但未返回结果");
            }
            // 将第一条结果落在原记录上，后续结果新增记录，使用同一个 typesettingId
            int total = results.size();
            for (int i = 0; i < total; i++) {
                NestingResponse.Result callbackResult = results.get(i);
                String templateCode = buildTemplateCode(i + 1, total);
                TypesettingElement element = new TypesettingElement();
                element.setNestedSvg(buildCompleteOssUrl(callbackResult.getNestedSvg()));
                String nestedMirrorSvg = StringUtils.isNotBlank(callbackResult.getNestedMirrorSvg())
                        ? callbackResult.getNestedMirrorSvg()
                        : callbackResult.getMirrorNestedSvg();
                if (StringUtils.isNotBlank(nestedMirrorSvg)) {
                    element.setNestedMirrorSvg(buildCompleteOssUrl(nestedMirrorSvg));
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
                    baseTypesettingInfo.setTypesettingCells(extractUsedSourceCells(typesettingId, callbackResult.getNestedSvg()));
                    baseTypesettingInfo.setTemplateCode(templateCode);
                    domainTypesettingService.updateTypesetting(baseTypesettingInfo);
                    continue;
                }
                TypesettingInfo newTypesettingInfo = cloneForCallback(baseTypesettingInfo);
                newTypesettingInfo.setId(null);
                newTypesettingInfo.setManufacturerMetaId(baseTypesettingInfo.getManufacturerMetaId());
                newTypesettingInfo.setElement(element);
                newTypesettingInfo.setTypesettingCells(extractUsedSourceCells(typesettingId, callbackResult.getNestedSvg()));
                newTypesettingInfo.setTemplateCode(templateCode);
                newTypesettingInfo.setStatus(TypesettingStatus.CONFIRMING.getCode());
                domainTypesettingService.addTypesetting(newTypesettingInfo);
            }
        } else {
            for (TypesettingInfo typesettingInfo : typesettingInfos) {
                typesettingInfo.setStatus(TypesettingStatus.FAILED.getCode());
                typesettingInfo.setRemark(response.getError());
                domainTypesettingService.updateTypesetting(typesettingInfo);
            }
        }
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
     * 4) 校验通过后，按节点顺序提取所有来源工序流的最长公共前缀。
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
        if (commonNodes.isEmpty()) {
            return null;
        }
        ProcedureFlow result = new ProcedureFlow();
        result.setNodes(new ArrayList<>(commonNodes));
        result.setTotalNodes(commonNodes.size());
        return result;
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
                    return sourceCell;
                })
                .collect(Collectors.toList());
    }

    private String resolvePieceNestingImg(ProductionPiece piece, boolean mirrorTypesettingTask) {
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
        Object requestObj = redisTemplate.opsForValue().get(typesettingId);
        if (!(requestObj instanceof String)) {
            return Collections.emptyList();
        }
        LayoutConfirmRequest request = JSON.parseObject((String) requestObj, LayoutConfirmRequest.class);
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
            List<TypesettingSourceCell> usedCells = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sourceIdCountMap.entrySet()) {
                String sourceId = entry.getKey();
                TypesettingProductionPieceVO matchedCell = null;
                for (TypesettingProductionPieceVO cell : sourceCells) {
                    if (cell != null && sourceId.equals(cell.getId())) {
                        matchedCell = cell;
                        break;
                    }
                }
                if (matchedCell == null || StringUtils.isBlank(matchedCell.getSourceType()) || StringUtils.isBlank(matchedCell.getSourceId())) {
                    continue;
                }
                TypesettingSourceCell usedCell = new TypesettingSourceCell();
                usedCell.setSourceType(matchedCell.getSourceType());
                usedCell.setSourceId(matchedCell.getSourceId());
                usedCell.setOrderItemId(matchedCell.getOrderItemId());
                usedCell.setQuantity(entry.getValue());
                usedCells.add(usedCell);
            }
            return usedCells;
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
                || layoutMode == TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CROSS;
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
