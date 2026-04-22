package com.mes.application.command.typesetting;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.resp.NestingResponse;
import com.mes.application.command.api.vo.CallbackCustomValue;
import com.mes.application.command.api.vo.UploadConfig;
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
import com.mes.application.command.typesetting.enums.TypesettingQueryType;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.ProductionPieceCell;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingCell;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Base64;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AppTypesettingService {

    @Autowired
    private TypesettingService domainTypesettingService;

    @Autowired
    private ProductionPieceService productionPieceService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private AlgorithmCoreApiService algorithmCoreApiService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AliCloudAuthService aliCloudAuthService;

    private static final String LAYOUT_CONFIRM_CACHE_PREFIX = "layout:confirm:";
    private static final long CACHE_EXPIRE_HOURS = 72;
    private static final String TEMP_CODE_QUEUE_KEY_PREFIX = "typesetting:temp-code:queue:";
    private static final String TEMP_CODE_QUEUE_INIT_KEY_PREFIX = "typesetting:temp-code:init:";
    private static final int TEMP_CODE_QUEUE_MAX = 100000;

    @Value("${external.callbackApi.generate_nested_files}")
    private String generateNestedFilesCallbackUrl;
    @Value("${external.callbackApi.generate_grid_nested_files}")
    private String generateGridNestedFilesCallbackUrl;


    /**
     * 统一查询接口
     * @param query 查询参数
     * @return 分页结果
     */
    public PagedResult<TypesettingProductionPieceVO> findTypesettingAndProductionPieces(TypesettingQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
        if (query.getPagedQuery() == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getPagedQuery().getSize() <= 0 || query.getPagedQuery().getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        String queryType = query.getQueryType();
        if (queryType == null) {
            queryType = TypesettingQueryType.ALL.getCode();
        }

        List<TypesettingProductionPieceVO> items = new ArrayList<>();
        long total = 0;

        switch (queryType) {
            case "ALL":
                // 查询全部：包括零件和排版
                items = queryBoth(query);
                total = countBoth(query);
                break;
            case "PART":
                // 只查询零件：且只允许查询状态为待排版的零件
                items = queryPartsOnly(query);
                total = countPartsOnly(query);
                break;
            case "TYPESETTING":
                // 只查询排版
                items = queryTypesettingOnly(query);
                total = countTypesettingOnly(query);
                break;
        }

        // 分页处理
        int pageSize = query.getPagedQuery().getSize();
        int currentPage = Math.toIntExact(query.getPagedQuery().getCurrent());
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, items.size());

        List<TypesettingProductionPieceVO> pagedItems = fromIndex < items.size() ? items.subList(fromIndex, toIndex) : Collections.emptyList();

        return new PagedResult<>(pagedItems, total, pageSize, currentPage);
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
        int currentPage = Math.toIntExact(query.getPagedQuery().getCurrent());
        int pageSize = query.getPagedQuery().getSize();

        // 当类型为"零件"时，只允许查询状态为待排版的零件
        // ProductionPiece 的 status 是 ProductionPieceStatus 的枚举，这里只查 PENDING_TYPESITTING 待排版
        List<ProductionPiece> parts = productionPieceService.findProductionPiecesByConditions(
                query.getManufacturerId(),
                ProductionPieceStatus.PENDING_TYPESITTING.getCode(),
            query.getMaterial(),
            query.getNodeName(),
            query.getStartDate(),
            query.getEndDate(),
            currentPage,
            pageSize
        );

        // 转换为 VO
        List<TypesettingProductionPieceVO> voList = new ArrayList<>();
        for (ProductionPiece piece : parts) {
            TypesettingProductionPieceVO vo = TypesettingProductionPieceVO.fromProductionPiece(piece);
            voList.add(vo);
        }

        return voList;
    }

    /**
     * 统计零件数量
     */
    private long countPartsOnly(TypesettingQuery query) {
        return productionPieceService.countProductionPiecesByConditions(
            "pending",
            query.getMaterial(),
            query.getNodeName(),
            query.getStartDate(),
            query.getEndDate()
        );
    }

    /**
     * 只查询排版
     */
    private List<TypesettingProductionPieceVO> queryTypesettingOnly(TypesettingQuery query) {
        int currentPage = Math.toIntExact(query.getPagedQuery().getCurrent());
        int pageSize = query.getPagedQuery().getSize();

        List<TypesettingInfo> typesettings = domainTypesettingService.findTypesettingByConditions(
            query.getStatus(),
            query.getMaterial(),
            query.getNodeName(),
            currentPage,
            pageSize
        );

        // 转换为 VO
        List<TypesettingProductionPieceVO> voList = new ArrayList<>();
        for (TypesettingInfo info : typesettings) {
            voList.add(TypesettingProductionPieceVO.fromTypesettingInfo(info));
        }

        return voList;
    }

    /**
     * 统计排版数量
     */
    private long countTypesettingOnly(TypesettingQuery query) {
        return domainTypesettingService.countTypesettingByConditions(
            query.getStatus(),
            query.getMaterial(),
            query.getNodeName()
        );
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

        // 7. 根据实际数量更新零件在排版这一节点的剩余数量，并将该次的数量填入下一个节点"排版中"
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
                System.err.println("更新生产工件 " + piece.getId() + " 状态失败：" + e.getMessage());
            }
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
        typesettingInfo.setStatus(TypesettingStatus.IN_PROGRESS.getCode());
        typesettingInfo.setQuantity(1);
        typesettingInfo.setLeaveQuantity(1);
        if (StringUtils.isNotBlank(request.getLayoutMode())) {
            typesettingInfo.setLayoutMode(request.getLayoutMode());
        } else if (!typesettingInfos.isEmpty()) {
            typesettingInfo.setLayoutMode(typesettingInfos.get(0).getLayoutMode());
        }
        List<ProductionPieceCell> productionPieceCells = productionPieces.stream()
                .map(piece -> {
                    ProductionPieceCell pieceCell = new ProductionPieceCell();
                    pieceCell.setProductionPieceId(piece.getProductionPieceId());
                    pieceCell.setOrderItemId(piece.getOrderItemId());
                    pieceCell.setQuantity(piece.getQuantity());
                    return pieceCell;
                })
                .collect(Collectors.toList());
        typesettingInfo.setPieceCells(productionPieceCells);
        domainTypesettingService.addTypesetting(typesettingInfo);
        return result;
    }

    /**
     * 确认排版（占位实现）
     */
    public LayoutConfirmResult confirmLayout(LayoutConfirmRequest request) {
        LayoutConfirmResult result = new LayoutConfirmResult();
        result.setSuccess(true);
        result.setMessage("确认排版接口待实现");
        return result;
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
                    element.setImg(piece.getProductImageFile().getRawFile());
                }
                elements.add(element);
            }
        }
        if (typesettingInfos != null) {
            for (TypesettingInfo info : typesettingInfos) {
                if (info == null) {
                    continue;
                }
                if (StringUtils.isBlank(info.getMaskSvg())) {
                    throw new IllegalArgumentException("排版信息缺少参与排版的maskSvg：" + info.getTypesettingId());
                }
                NestingRequest.Element element = new NestingRequest.Element();
                element.setId(info.getId());
                element.setSvg(info.getMaskSvg());
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
        uploadConfig.setUploadPath("layout/");
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
     * @param productionPieceIds 生产工件 ID 列表
     * @return 操作结果
     */
    public ConfirmPrintResult confirmPrint(List<String> productionPieceIds) {
        if (productionPieceIds == null || productionPieceIds.isEmpty()) {
            throw new RuntimeException("生产工件 ID 列表不能为空");
        }

        List<ProductionPiece> productionPieces = new ArrayList<>();
        
        for (String pieceId : productionPieceIds) {
            try {
                // 使用状态机方法更新为待打印状态
                productionPieceService.confirmTypesetting(pieceId);
                
                // 获取更新后的工件信息
                ProductionPiece piece = productionPieceService.findById(pieceId);
                productionPieces.add(piece);
                
            } catch (Exception e) {
                System.err.println("更新生产工件 " + pieceId + " 状态失败：" + e.getMessage());
            }
        }

        ConfirmPrintResult result = new ConfirmPrintResult();
        result.setSuccess(true);
        result.setMessage("确认打印成功，共更新 " + productionPieces.size() + " 个工件为待打印状态");
        result.setUpdatedPieceCount(productionPieces.size());
        result.setUpdatedPieceIds(productionPieces.stream()
                .map(ProductionPiece::getId)
                .collect(Collectors.toList()));

        return result;
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
                element.setNestedSvg(callbackResult.getNestedSvg());
                element.setUtilization(callbackResult.getUtilization());
                if (callbackResult.getWidth() != null || callbackResult.getHeight() != null) {
                    element.setWidth(callbackResult.getWidth());
                    element.setHeight(callbackResult.getHeight());
                } else if (callbackResult.getContainerSize() != null) {
                    element.setWidth(callbackResult.getContainerSize().getWidth());
                    element.setHeight(callbackResult.getContainerSize().getHeight());
                }
                if (i == 0) {
                    baseTypesettingInfo.setStatus(TypesettingStatus.CONFIRMING.getCode());
                    baseTypesettingInfo.setElement(element);
                    domainTypesettingService.updateTypesetting(baseTypesettingInfo);
                    continue;
                }
                TypesettingInfo newTypesettingInfo = cloneForCallback(baseTypesettingInfo);
                newTypesettingInfo.setId(null);
                newTypesettingInfo.setElement(element);
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
        target.setPieceCells(source.getPieceCells());
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
