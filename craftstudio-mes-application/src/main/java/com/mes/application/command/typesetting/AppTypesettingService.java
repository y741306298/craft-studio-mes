package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.vo.*;
import com.mes.application.dto.TypesettingQuery;
import com.mes.application.command.typesetting.enums.TypesettingQueryType;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
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
    private RestTemplate restTemplate;


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
            TypesettingProductionPieceVO vo = TypesettingProductionPieceVO.fromPiece(piece);
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
            TypesettingProductionPieceVO vo = new TypesettingProductionPieceVO();
            vo.setSourceType("TYPESETTING");
            vo.setSourceId(info.getId());
            vo.setQuantity(info.getQuantity());
            vo.setCompletedQuantity(info.getCompletedQuantity());
            vo.setMaterial(info.getMaterial()); // 直接从 TypesettingInfo 获取 material
//            vo.setProcedureFlow(info.getProcedureFlow());
            vo.setPreviewUrl(info.getTypesettingUrl());
            vo.setRemark(null); // 排版暂无备注
//            vo.setOrderItemInfo(null); // 排版不需要 OrderItem 信息

            voList.add(vo);
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
     *
     * @param productionPieceIds 生产工件 ID 列表
     * @return 排版结果
     */
    public LayoutConfirmResult confirmLayout(List<String> productionPieceIds) {
        if (productionPieceIds == null || productionPieceIds.isEmpty()) {
            throw new RuntimeException("生产工件 ID 列表不能为空");
        }

        // 1. 根据 productionPieceId 获取所有的生产零件信息
        List<ProductionPiece> productionPieces = new ArrayList<>();
        for (String pieceId : productionPieceIds) {
            ProductionPiece piece = productionPieceService.findById(pieceId);
            if (piece == null) {
                throw new RuntimeException("生产工件不存在：" + pieceId);
            }
            productionPieces.add(piece);
        }

        // 2. 获取相对应的 OrderItem 信息
        List<OrderItem> orderItems = new ArrayList<>();
        Set<String> orderItemIdSet = new HashSet<>();
        for (ProductionPiece piece : productionPieces) {
            if (!orderItemIdSet.contains(piece.getOrderItemId())) {
                OrderItem orderItem = orderItemService.findById(piece.getOrderItemId());
                if (orderItem == null) {
                    throw new RuntimeException("订单项不存在：" + piece.getOrderItemId());
                }
                orderItems.add(orderItem);
                orderItemIdSet.add(piece.getOrderItemId());
            }
        }

        // 3. 校验材料是否一致
        String validateMaterialResult = validateMaterials(orderItems);
        if (!validateMaterialResult.equals("PASS")) {
            return LayoutConfirmResult.failed(validateMaterialResult);
        }

        // 4. 校验特殊工艺的材料一致性
        String validateProcedureResult = validateSpecialProcedureMaterials(productionPieces);
        if (!validateProcedureResult.equals("PASS")) {
            return LayoutConfirmResult.failed(validateProcedureResult);
        }

        // 5. 调用排版 API
        LayoutApiResponse apiResponse = callLayoutApi(productionPieces, orderItems);

        if (apiResponse == null || !apiResponse.isSuccess()) {
            return LayoutConfirmResult.failed("排版 API 调用失败");
        }

        // 6. 构建返回结果
        LayoutConfirmResult result = new LayoutConfirmResult();
        result.setSuccess(true);
        result.setLayoutId(apiResponse.getLayoutId());
        result.setLayoutUrl(apiResponse.getLayoutUrl());
        result.setProductionPieceCount(productionPieces.size());
        result.setMessage("排版确认成功");

        // 7. 更新所有生产工件的状态为排版待确认
        for (ProductionPiece piece : productionPieces) {
            try {
                productionPieceService.completeTypesetting(piece.getId());
            } catch (Exception e) {
                System.err.println("更新生产工件 " + piece.getId() + " 状态失败：" + e.getMessage());
            }
        }

        return result;
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
     * @param orderItems 订单项列表
     * @return "PASS" 表示通过，否则返回错误信息
     */
    private String validateMaterials(List<OrderItem> orderItems) {
        if (orderItems.isEmpty()) {
            return "订单项列表不能为空";
        }

        MaterialConfig firstMaterial = orderItems.get(0).getMaterial();
        if (firstMaterial == null) {
            return "第一个订单项的材料为空";
        }

        for (int i = 1; i < orderItems.size(); i++) {
            MaterialConfig material = orderItems.get(i).getMaterial();
            if (material == null) {
                return "订单项 " + orderItems.get(i).getOrderItemId() + " 的材料为空";
            }
            if (!firstMaterial.equals(material)) {
                return "材料不一致：订单项 " + orderItems.get(0).getOrderItemId() +
                        " 的材料为 " + firstMaterial +
                        "，订单项 " + orderItems.get(i).getOrderItemId() +
                        " 的材料为 " + material;
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
     * 调用排版 API
     *
     * @param productionPieces 生产工件列表
     * @param orderItems 订单项列表
     * @return API 响应
     */
    private LayoutApiResponse callLayoutApi(List<ProductionPiece> productionPieces,
                                            List<OrderItem> orderItems) {
        try {
            // 构建请求参数
            Map<String, Object> requestParams = new HashMap<>();
            requestParams.put("productionPieces", productionPieces);
            requestParams.put("orderItems", orderItems);
            requestParams.put("material", orderItems.get(0).getMaterial());
            requestParams.put("pieceCount", productionPieces.size());

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestParams, headers);

            // 调用 POST 接口
            String apiUrl = "";
            ResponseEntity<LayoutApiResponse> response = restTemplate.postForEntity(
                    apiUrl,
                    requestEntity,
                    LayoutApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                return null;
            }

        } catch (Exception e) {
            System.err.println("调用排版 API 异常：" + e.getMessage());
            return null;
        }
    }

}
