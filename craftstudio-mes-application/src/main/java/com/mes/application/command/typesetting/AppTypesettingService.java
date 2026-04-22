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
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.ConfirmPrintResult;
import com.mes.application.command.typesetting.vo.GenerateQrCodeResult;
import com.mes.application.command.typesetting.vo.GenerateTempCodeResult;
import com.mes.application.command.typesetting.vo.LayoutConfirmResult;
import com.mes.application.command.typesetting.vo.ReleaseLayoutResult;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.req.typesetting.GenerateQrCodeRequest;
import com.mes.application.dto.req.typesetting.GenerateTempCodeRequest;
import com.mes.application.dto.TypesettingQuery;
import com.mes.application.dto.req.typesetting.ConfirmPrintRequest;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingPrintTaskService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingDownloadTaskData;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.piliofpala.craftstudio.shared.infra.cloud.platforms.alicloud.AliCloudAuthService;
import com.piliofpala.craftstudio.shared.infra.cloud.storage.dto.ObjectStorageTempAuthConfig;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
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
import java.util.stream.Collectors;

@Service
public class AppTypesettingService {

    @Autowired
    private TypesettingService domainTypesettingService;

    @Autowired
    private ProductionPieceService productionPieceService;

    @Autowired
    private TypesettingPrintTaskService typesettingPrintTaskService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private AlgorithmCoreApiService algorithmCoreApiService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AliCloudAuthService aliCloudAuthService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private List<TypesettingLayoutModeBuildService> layoutModeBuildServices;

    /**
     * layoutMode -> builder 的运行时映射表。
     * 在容器初始化完成后由 initLayoutModeBuilders 填充。
     */
    private final Map<TypesettingLayoutMode, TypesettingLayoutModeBuildService> layoutModeBuildServiceMap = new EnumMap<>(TypesettingLayoutMode.class);

    private static final String LAYOUT_CONFIRM_CACHE_PREFIX = "layout:confirm:";
    private static final long CACHE_EXPIRE_HOURS = 72;
    private static final String TEMP_CODE_QUEUE_KEY_PREFIX = "typesetting:temp-code:queue:";
    private static final String TEMP_CODE_QUEUE_INIT_KEY_PREFIX = "typesetting:temp-code:init:";
    private static final int TEMP_CODE_QUEUE_MAX = 100000;
    private static final Pattern SVG_SOURCE_INDEX_PATTERN = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"");
    private static final int TAG_STRIP_HEIGHT_MM = 30;

    @PostConstruct
    public void initLayoutModeBuilders() {
        // 将所有模式构建器注册到 map，供 confirmLayout 阶段按 mode 分发调用
        if (layoutModeBuildServices == null) {
            return;
        }
        for (TypesettingLayoutModeBuildService buildService : layoutModeBuildServices) {
            layoutModeBuildServiceMap.put(buildService.supportMode(), buildService);
        }
    }

    @Value("${external.callbackApi.generate_nested_files}")
    private String generateNestedFilesCallbackUrl;
    @Value("${external.callbackApi.generate_grid_nested_files}")
    private String generateGridNestedFilesCallbackUrl;
    @Value("${external.callbackApi.generate_forme}")
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

        List<ProductionPiece> productionPieces = productionPieceService.findProductionPiecesByConditions(
                query.getManufacturerMetaId(),
                null,
                null,
                null,
                null,
                null,
                1,
                Integer.MAX_VALUE
        );
        for (ProductionPiece piece : productionPieces) {
            if (getPendingTypesettingQuantity(piece) > 0) {
                items.add(TypesettingProductionPieceVO.fromProductionPiece(piece));
            }
        }

        List<TypesettingInfo> typesettingInfos = domainTypesettingService.findTypesettingByConditions(
                query.getManufacturerMetaId(),
                null,
                null,
                null,
                1,
                Integer.MAX_VALUE
        );
        for (TypesettingInfo info : typesettingInfos) {
            Integer leaveQuantity = info.getLeaveQuantity() == null ? 0 : info.getLeaveQuantity();
            if (leaveQuantity > 0) {
                items.add(TypesettingProductionPieceVO.fromTypesettingInfo(info));
            }
        }

        long total = items.size();
        return new PagedResult<>(items, total, items.size(), 1);
    }

    /**
     * 查询状态为待确认（confirming）的排版信息列表（分页）
     * @param manufacturerMetaId 厂商元数据ID
     * @param current 当前页码
     * @param size 每页大小
     * @return 分页结果
     */
    public PagedResult<TypesettingInfo> findConfirmingTypesetting(String manufacturerMetaId, int current, int size) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("manufacturerMetaId 不能为空");
        }
        if (current < 1) {
            current = 1;
        }
        if (size < 1 || size > 100) {
            size = 20;
        }

        List<TypesettingInfo> typesettingInfos = domainTypesettingService.findTypesettingByConditions(
                manufacturerMetaId,
                TypesettingStatus.CONFIRMING.getCode(),
                null,
                null,
                current,
                size
        );

        long total = domainTypesettingService.countTypesettingByConditions(
                TypesettingStatus.CONFIRMING.getCode(),
                null,
                null
        );

        return new PagedResult<>(typesettingInfos, total, typesettingInfos.size(), current);
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
                query.getMaterial(),
                query.getNodeName(),
                query.getStartDate(),
                query.getEndDate(),
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
                query.getMaterial(),
                query.getNodeName(),
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
                    return LayoutConfirmResult.failed("生产工件不存在：" + productionPiece.getProductionPieceId());
                }
                if (productionPiece.getQuantity() != null) {
                    dbPiece.setQuantity(productionPiece.getQuantity());
                }
                productionPieces.add(dbPiece);
            } else if (TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                TypesettingInfo typesettingInfo = cell.toTypesettingInfo();
                TypesettingInfo dbTypesettingInfo = domainTypesettingService.findById(typesettingInfo.getId());
                if (dbTypesettingInfo == null) {
                    return LayoutConfirmResult.failed("排版信息不存在：" + typesettingInfo.getId());
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
                return LayoutConfirmResult.failed(typesettingInfo.getId() + "排版数量超出");
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
        String cacheKey = IdGenerator.generateId("LAYOUT");
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
        System.out.println("nestingRequest========:"+JSON.toJSONString(nestingRequest));
        TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(request.getLayoutMode());
        NestingResponse nestingResponse = "grid_typesetting".equals(layoutMode.getLayoutCategory())
                ? algorithmCoreApiService.generateGridNestedFilesAsync(nestingRequest)
                : algorithmCoreApiService.generateNestedFilesAsync(nestingRequest);
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
        typesettingInfo.setMaterialConfigs(materialConfigs);
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
     * <p>说明：圆形二维码模式（shaped_cutting_plt_qr_circle）依赖 manufacturerMetaId 生成队列码与二维码。
     */
    public LayoutConfirmResult confirmLayout(TypesettingInfo request) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            return LayoutConfirmResult.failed("确认排版参数不能为空，且必须包含排版ID");
        }
        TypesettingInfo typesettingInfo = domainTypesettingService.findById(request.getId());
        if (typesettingInfo == null) {
            return LayoutConfirmResult.failed("排版信息不存在：" + request.getId());
        }
        if (typesettingInfo.getElement() == null || StringUtils.isBlank(typesettingInfo.getElement().getNestedSvg())) {
            return LayoutConfirmResult.failed("排版信息缺少 nestedSvg，无法确认排版");
        }

        TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(
                StringUtils.isNotBlank(request.getLayoutMode()) ? request.getLayoutMode() : typesettingInfo.getLayoutMode()
        );
        if (TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CIRCLE == layoutMode
                && StringUtils.isBlank(typesettingInfo.getManufacturerMetaId())) {
            return LayoutConfirmResult.failed("圆形定位点排版缺少 manufacturerMetaId，无法生成队列编号与二维码");
        }
        typesettingInfo.setLayoutMode(layoutMode.getCode());
        typesettingInfo.applyLayoutModeConfig();

        String businessId = StringUtils.isNotBlank(typesettingInfo.getTypesettingId()) ? typesettingInfo.getTypesettingId() : typesettingInfo.getId();
        FormeGenerationRequest formeRequest = buildFormeGenerationRequest(typesettingInfo, layoutMode, businessId);
        System.out.println(JSON.toJSONString(formeRequest));
        FormeGenerationResponse response = algorithmCoreApiService.generateForme(formeRequest);
        if (response == null || StringUtils.isBlank(response.getStatus())) {
            return LayoutConfirmResult.failed("确认排版失败：印版生成服务返回为空");
        }
        if (!"success".equalsIgnoreCase(response.getStatus())) {
            String errorMessage = StringUtils.isNotBlank(response.getError()) ? response.getError() : "确认排版失败：印版生成服务处理失败";
            return LayoutConfirmResult.failed(errorMessage);
        }

        // 更新印版信息为“待排版”，并保存印版生成结果（json/plt/formeSvg）
        typesettingInfo.setStatus(TypesettingStatus.PENDING.getCode());
        applyFormeGenerationResult(typesettingInfo, response.getResult());
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
        // element 原始宽高（单位 mm），若算法回调中缺失则给默认值兜底
        BigDecimal nestedWidth = typesettingInfo.getElement() != null && typesettingInfo.getElement().getWidth() != null
                ? typesettingInfo.getElement().getWidth() : BigDecimal.valueOf(1200);
        BigDecimal nestedHeight = typesettingInfo.getElement() != null && typesettingInfo.getElement().getHeight() != null
                ? typesettingInfo.getElement().getHeight() : BigDecimal.valueOf(800);
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
        uploadConfig.setUploadPath(modeResult.getUploadPath());
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
     * <p>号码来源：复用现有临时码队列（循环 1~100000）。
     */
    private String generatePrintingPlateName(String manufacturerMetaId) {
        GenerateTempCodeRequest request = new GenerateTempCodeRequest();
        request.setManufacturerMetaId(manufacturerMetaId);
        GenerateTempCodeResult tempCodeResult = generateTempCode(request);
        return tempCodeResult.getTempCode() + ".plt";
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


    private NestingRequest buildNestingRequest(LayoutConfirmRequest request, String cacheKey) {
        if (StringUtils.isBlank(generateNestedFilesCallbackUrl)) {
            throw new IllegalArgumentException("排版回调地址未配置");
        }
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
                    ProductionPiece dbPiece = productionPieceService.findByProductionPieceId(piece.getProductionPieceId());
                    if (dbPiece == null) {
                        throw new IllegalArgumentException("生产工件不存在：" + piece.getProductionPieceId());
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
                if (piece.getProductImageFile() != null && piece.getProductImageFile().getRawFile() != null) {
                    element.setImg(piece.getTemplateCode());
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
        manifest.setSpacing(10);
        manifest.setContainers(containers);
        manifest.setElements(elements);

        CallbackConfig callbackConfig = new CallbackConfig();
        TypesettingLayoutMode layoutMode = TypesettingLayoutMode.fromCode(request.getLayoutMode());
        if ("grid_typesetting".equals(layoutMode.getLayoutCategory())) {
            callbackConfig.setCallbackUrl(generateGridNestedFilesCallbackUrl);
        } else {
            callbackConfig.setCallbackUrl(generateNestedFilesCallbackUrl);
        }
        CallbackCustomValue callbackCustomValue = new CallbackCustomValue();
        callbackCustomValue.setId(cacheKey);
        callbackConfig.setCallbackCustomValue(callbackCustomValue);

        ObjectStorageTempAuthConfig objectStorageTempAuthConfig = aliCloudAuthService.getObjectStorageTempAuthConfig(cacheKey);
        UploadConfig uploadConfig = new UploadConfig();
        uploadConfig.setUploadPath("layout/"+cacheKey+"/");
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
        typesettingInfo.setLayoutMode(layoutMode.getCode());
        typesettingInfo.applyLayoutModeConfig();

        String businessId = StringUtils.isNotBlank(typesettingInfo.getTypesettingId()) ? typesettingInfo.getTypesettingId() : typesettingInfo.getId();
        FormeGenerationRequest formeRequest = buildFormeGenerationRequest(typesettingInfo, layoutMode, businessId);
        FormeGenerationResponse response = algorithmCoreApiService.generateForme(formeRequest);
        if (response == null || StringUtils.isBlank(response.getStatus())) {
            throw new RuntimeException("确认打印失败：印版生成服务返回为空");
        }
        if (!"success".equalsIgnoreCase(response.getStatus())) {
            String errorMessage = StringUtils.isNotBlank(response.getError()) ? response.getError() : "确认打印失败：印版生成服务处理失败";
            throw new RuntimeException(errorMessage);
        }

        applyFormeGenerationResult(typesettingInfo, response.getResult());
        typesettingInfo.setStatus("待打印");

        Set<String> visitedTypesettingIds = new HashSet<>();
        Map<String, Integer> productionPieceUsage = new LinkedHashMap<>();
        collectProductionPieceUsage(typesettingInfo, 1, visitedTypesettingIds, productionPieceUsage);
        int plateUseCount = typesettingInfo.getLeaveQuantity() != null && typesettingInfo.getLeaveQuantity() > 0
                ? typesettingInfo.getLeaveQuantity() : 1;
        transferTypesettingQuantityToPrinting(productionPieceUsage, plateUseCount);

        Set<String> productionPieceIds = productionPieceUsage.keySet();
        TypesettingDownloadTaskData downloadTaskData = buildDownloadTaskData(
                typesettingInfo.getId(),
                request.getDeviceCode(),
                typesettingInfo.getElement(),
                productionPieceIds
        );
        domainTypesettingService.updateTypesetting(typesettingInfo);
        savePrintTask(typesettingInfo.getId(), request.getDeviceCode(), downloadTaskData);

        ConfirmPrintResult result = new ConfirmPrintResult();
        result.setSuccess(true);
        result.setMessage("确认打印成功，共生成图片任务 " + downloadTaskData.getImamges().size()
                + " 条、plt任务 " + downloadTaskData.getPlts().size()
                + " 条、json任务 " + downloadTaskData.getJsons().size() + " 条");
        result.setUpdatedPieceCount(productionPieceIds.size());
        result.setUpdatedPieceIds(new ArrayList<>(productionPieceIds));
        return result;
    }

    private void collectProductionPieceUsage(TypesettingInfo typesettingInfo,
                                             int multiplier,
                                             Set<String> visitedTypesettingIds,
                                             Map<String, Integer> productionPieceUsage) {
        if (typesettingInfo == null || StringUtils.isBlank(typesettingInfo.getId())) {
            return;
        }
        if (!visitedTypesettingIds.add(typesettingInfo.getId())) {
            return;
        }
        if (typesettingInfo.getTypesettingCells() == null) {
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
            TypesettingInfo child = domainTypesettingService.findById(cell.getSourceId());
            if (child == null) {
                child = domainTypesettingService.findTypesettingByTypesettingId(cell.getSourceId());
            }
            collectProductionPieceUsage(child, currentMultiplier, visitedTypesettingIds, productionPieceUsage);
        }
    }

    private void transferTypesettingQuantityToPrinting(Map<String, Integer> productionPieceUsage, int plateUseCount) {
        if (productionPieceUsage == null || productionPieceUsage.isEmpty() || plateUseCount <= 0) {
            return;
        }
        for (Map.Entry<String, Integer> entry : productionPieceUsage.entrySet()) {
            String productionPieceId = entry.getKey();
            int requiredQuantity = entry.getValue() * plateUseCount;
            if (requiredQuantity <= 0) {
                continue;
            }
            ProductionPiece piece = productionPieceService.findByProductionPieceId(productionPieceId);
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
                throw new RuntimeException("零件 " + productionPieceId + " 的“排版中”数量不足，需求="
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

    private TypesettingDownloadTaskData buildDownloadTaskData(String typesettingInfoId,
                                                              String deviceCode,
                                                              TypesettingElement typesettingElement,
                                                              Set<String> productionPieceIds) {
        LinkedHashSet<String> imageSet = new LinkedHashSet<>();
        for (String productionPieceId : productionPieceIds) {
            ProductionPiece piece = productionPieceService.findByProductionPieceId(productionPieceId);
            if (piece == null) {
                continue;
            }
            appendRawFile(imageSet, piece.getProductImageFile() == null ? null : piece.getProductImageFile().getRawFile());
            appendRawFile(imageSet, piece.getMaskImageFile() == null ? null : piece.getMaskImageFile().getRawFile());
        }
        LinkedHashSet<String> pltSet = new LinkedHashSet<>();
        LinkedHashSet<String> jsonSet = new LinkedHashSet<>();
        if (typesettingElement != null) {
            if (typesettingElement.getPlt() != null) {
                appendRawFile(pltSet, typesettingElement.getPlt().getNormal());
                appendRawFile(pltSet, typesettingElement.getPlt().getReverse());
            }
            appendRawFile(jsonSet, typesettingElement.getJson());
        }
        TypesettingDownloadTaskData data = new TypesettingDownloadTaskData();
        data.setId(typesettingInfoId);
        data.setDeviceCode(deviceCode);
        data.setImamges(new ArrayList<>(imageSet));
        data.setPlts(new ArrayList<>(pltSet));
        data.setJsons(new ArrayList<>(jsonSet));
        return data;
    }

    private void appendRawFile(Set<String> container, String fileUrl) {
        if (StringUtils.isNotBlank(fileUrl)) {
            container.add(fileUrl);
        }
    }

    private void savePrintTask(String typesettingInfoId, String deviceCode, TypesettingDownloadTaskData data) {
        TypesettingPrintTask task = new TypesettingPrintTask();
        task.setTypesettingInfoId(typesettingInfoId);
        task.setDeviceCode(deviceCode);
        task.setData(data);
        typesettingPrintTaskService.saveOrUpdate(task);
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
     * @param productionPieceIds 生产工件 ID 列表
     * @return 操作结果
     */
    public ReleaseLayoutResult releaseLayout(List<String> productionPieceIds) {
        if (productionPieceIds == null || productionPieceIds.isEmpty()) {
            throw new RuntimeException("生产工件 ID 列表不能为空");
        }

        List<ProductionPiece> productionPieces = new ArrayList<>();
        List<String> deletedLayoutIds = new ArrayList<>();
        
        for (String pieceId : productionPieceIds) {
            try {
                ProductionPiece piece = productionPieceService.findById(pieceId);
                if (piece == null) {
                    System.err.println("生产工件不存在：" + pieceId);
                    continue;
                }
                
                // 1. 删除关联的排版文件（如果存在）
                if (StringUtils.isNotBlank(piece.getTemplateCode())) {
                    try {
                        // 调用 API 删除排版文件
                        deleteLayoutFile(piece.getTemplateCode());
                        deletedLayoutIds.add(piece.getTemplateCode());
                    } catch (Exception e) {
                        System.err.println("删除排版文件失败：" + piece.getTemplateCode() + ", 错误：" + e.getMessage());
                    }
                }
                
                // 2. 将生产工件状态改回待排版状态
                piece.setStatus("PENDING_TYPESITTING");
                productionPieceService.updateProductionPiece(piece);
                
                productionPieces.add(piece);
                
            } catch (Exception e) {
                System.err.println("处理生产工件 " + pieceId + " 失败：" + e.getMessage());
            }
        }

        ReleaseLayoutResult result = new ReleaseLayoutResult();
        result.setSuccess(true);
        result.setMessage("释放排版成功，共处理 " + productionPieces.size() + " 个工件，删除 " + deletedLayoutIds.size() + " 个排版文件");
        result.setReleasedPieceCount(productionPieces.size());
        result.setReleasedPieceIds(productionPieces.stream()
                .map(ProductionPiece::getId)
                .collect(Collectors.toList()));
        result.setDeletedLayoutIds(deletedLayoutIds);

        return result;
    }

    /**
     * 删除排版文件
     * TODO: 需要根据实际存储方式实现删除逻辑
     * 
     * @param layoutUrl 排版文件 URL 或路径
     */
    private void deleteLayoutFile(String layoutUrl) {
        // TODO: 实现删除排版文件的逻辑
        // 可能是调用文件系统 API、云存储 API 等
        // 示例：
        // if (layoutUrl.startsWith("http")) {
        //     restTemplate.delete(layoutUrl);
        // } else {
        //     File file = new File(layoutUrl);
        //     if (file.exists()) {
        //         file.delete();
        //     }
        // }
        System.out.println("删除排版文件：" + layoutUrl);
    }

    /**
     * 校验所有订单项的材料是否一致
     *
     * @return "PASS" 表示通过，否则返回错误信息
     */
    private String validateMaterials(List<ProductionPiece> productionPieces) {
        if (productionPieces.isEmpty()) {
            return "零件列表不能为空";
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
        for (ProductionPiece piece : productionPieces) {
            if (piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
                continue;
            }

            List<ProcedureFlowNode> nodes = piece.getProcedureFlow().getNodes();

            // 查找是否存在"覆板"或"双面对裱"工序
            ProcedureFlowNode fuBanNode = null;
            ProcedureFlowNode shuangMianNode = null;

            for (ProcedureFlowNode node : nodes) {
                if ("覆板".equals(node.getNodeName())) {
                    fuBanNode = node;
                } else if ("双面对裱".equals(node.getNodeName())) {
                    shuangMianNode = node;
                }
            }

            // 如果存在这两种工序，需要校验材料一致性
            if (fuBanNode != null || shuangMianNode != null) {
                // 这里需要根据实际业务逻辑获取工序所用的材料
                // 假设通过某种方式可以获取工序对应的材料信息
                String procedureMaterial = getProcedureMaterial(piece, fuBanNode != null ? fuBanNode : shuangMianNode);

                if (procedureMaterial == null || procedureMaterial.trim().isEmpty()) {
                    return "生产工件 " + piece.getProductionPieceId() +
                            " 的工序材料为空";
                }

                // 获取订单项的材料进行对比
                OrderItem orderItem = orderItemService.findById(piece.getOrderItemId());
                if (orderItem == null) {
                    return "生产工件 " + piece.getProductionPieceId() +
                            " 对应的订单项不存在";
                }

                String orderItemMaterial = null;
                if (orderItem.getMaterial() != null && orderItem.getMaterial().getMaterialSnapshot() != null) {
                    orderItemMaterial = orderItem.getMaterial().getMaterialSnapshot().getName();
                }
                
                if (procedureMaterial != null && !procedureMaterial.equals(orderItemMaterial)) {
                    return "生产工件 " + piece.getProductionPieceId() +
                            " 的工序材料与订单项材料不一致：工序材料为 " +
                            procedureMaterial + "，订单项材料为 " + orderItemMaterial;
                }
            }
        }

        return "PASS";
    }

    /**
     * 获取工序所使用的材料
     * TODO: 需要根据实际业务逻辑实现
     *
     * @param piece 生产工件
     * @param procedureNode 工序节点
     * @return 工序材料
     */
    private String getProcedureMaterial(ProductionPiece piece, ProcedureFlowNode procedureNode) {
        // TODO: 实现获取工序材料的逻辑
        // 可能需要查询工序配置表或者从其他地方获取
        // 这里暂时从订单项获取材料
        OrderItem orderItem = orderItemService.findById(piece.getOrderItemId());
        if (orderItem != null && orderItem.getMaterial() != null) {
            MaterialConfig.MaterialSnapshot snapshot = orderItem.getMaterial().getMaterialSnapshot();
            return snapshot != null ? snapshot.getName() : null;
        }
        return null;
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
            for (int i = 0; i < results.size(); i++) {
                NestingResponse.Result callbackResult = results.get(i);
                TypesettingElement element = new TypesettingElement();
                element.setNestedSvg(buildCompleteOssUrl(callbackResult.getNestedSvg()));
                element.setUtilization(callbackResult.getUtilization());
                if (callbackResult.getContainerSize() != null) {
                    element.setWidth(callbackResult.getContainerSize().getWidth());
                    element.setHeight(callbackResult.getContainerSize().getHeight());
                } else if (callbackResult.getWidth() != null || callbackResult.getHeight() != null) {
                    element.setWidth(callbackResult.getWidth());
                    element.setHeight(callbackResult.getHeight());
                }
                if (i == 0) {
                    baseTypesettingInfo.setStatus(TypesettingStatus.CONFIRMING.getCode());
                    baseTypesettingInfo.setElement(element);
                    baseTypesettingInfo.setTypesettingCells(extractUsedSourceCells(typesettingId, callbackResult.getNestedSvg()));
                    domainTypesettingService.updateTypesetting(baseTypesettingInfo);
                    continue;
                }
                TypesettingInfo newTypesettingInfo = cloneForCallback(baseTypesettingInfo);
                newTypesettingInfo.setId(null);
                newTypesettingInfo.setManufacturerMetaId(baseTypesettingInfo.getManufacturerMetaId());
                newTypesettingInfo.setElement(element);
                newTypesettingInfo.setTypesettingCells(extractUsedSourceCells(typesettingId, callbackResult.getNestedSvg()));
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

    private TypesettingInfo cloneForCallback(TypesettingInfo source) {
        TypesettingInfo target = new TypesettingInfo();
        target.setTypesettingId(source.getTypesettingId());
        target.setMaterialConfigs(source.getMaterialConfigs());
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
        return target;
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
