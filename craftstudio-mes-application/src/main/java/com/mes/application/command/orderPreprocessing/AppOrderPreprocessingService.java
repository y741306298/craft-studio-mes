package com.mes.application.command.orderPreprocessing;

import com.mes.application.command.orderPreprocessing.vo.CutResult;
import com.mes.application.command.orderPreprocessing.vo.MaskResult;
import com.mes.application.command.orderPreprocessing.vo.PltApiResponse;
import com.mes.application.command.orderPreprocessing.vo.PltGenerateResult;
import com.mes.application.command.productionPiece.AppPieceCirculationService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.procedure.service.ProcedureService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.service.ProcedureFlowService;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppOrderPreprocessingService {

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ProductionPieceService productionPieceService;

    @Autowired
    private ProcedureFlowService procedureFlowService;

    @Autowired
    private ProcedureService procedureService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppPieceCirculationService appPieceCirculationService;

    /**
     * 订单预处理：根据 orderId 查询订单下的所有订单项，解析工艺流程并处理裁切和异形工艺
     *
     */
    public void preprocessOrder(List<OrderItem> orderItems) {
        List<ProductionPiece> resultPieces = new ArrayList<>();

        // 2. 遍历每个订单项进行处理
        for (OrderItem orderItem : orderItems) {
            try {
                //转化成生产零件
                List<ProductionPiece> pieces = processSingleOrderItem(orderItem);

                // 处理成功，将订单项状态改为生产中
                updateOrderItemStatusToInProduction(orderItem.getOrderItemId());
                resultPieces.addAll(pieces);
            } catch (Exception e) {
                // 单个订单项处理失败时，标记该订单项为失败状态，但继续处理其他订单项
                orderItemService.markAsFailed(orderItem.getOrderItemId(), e.getMessage());
                System.err.println("处理订单项失败：" + orderItem.getOrderItemId() + ", 错误：" + e.getMessage());
            }
        }

        // 3. 预处理完成后，让所有的零件进入第一个节点
        for (ProductionPiece resultPiece : resultPieces) {
            appPieceCirculationService.moveToNextNode(resultPiece, 1);
            //暂时默认将零件状态改为待排版
            productionPieceService.updateProductionPieceStatusByproductionPieceId(resultPiece.getProductionPieceId(), ProductionPieceStatus.PENDING_TYPESITTING);
        }
    }

    /**
     * 处理单个订单项的工艺流程
     *
     * @param orderItem 订单项
     * @return 处理后的生产零件列表
     */
    private List<ProductionPiece> processSingleOrderItem(OrderItem orderItem) {
        // 1. 解析工艺流程
        String processingFlow = orderItem.getProcessingFlow();
        if (processingFlow == null || processingFlow.trim().isEmpty()) {
            throw new RuntimeException("工艺流程不能为空：" + orderItem.getOrderItemId());
        }
        //将订单工艺转化为生产工序
        ProcedureFlow procedureFlow = procedureFlowService.parseProcessingFlow(orderItem.getProcedureFlow());
        List<ProcedureFlowNode> processingNodes = procedureFlow.getNodes();

        // 2. 判断是否有"裁切"工艺
        boolean hasCutting = procedureFlowService.hasNodeWithName(processingNodes, "裁切");

        // 3. 判断是否有"异形"工艺
        boolean hasSpecialShape = procedureFlowService.hasNodeWithName(processingNodes, "异形");

        // 4. 根据不同情况处理
        if (hasCutting) {
            // 情况一：需要分切（无论是否有异形）
            return processWithCutting(orderItem, processingNodes, hasSpecialShape);
        } else if (hasSpecialShape) {
            // 情况二：不需要分切但需要抠图
            return processWithoutCuttingButWithMasking(orderItem);
        } else {
            // 情况三：既不需要分切也不需要抠图
            return processWithoutCuttingAndMasking(orderItem);
        }
    }

    /**
     * 情况一：有裁切工艺的处理流程
     */
    private List<ProductionPiece> processWithCutting(OrderItem orderItem, List<ProcedureFlowNode> processingNodes, boolean hasSpecialShape) {
        // 1. 如果有异形工艺，校验 maskImgUrl 是否存在
        if (hasSpecialShape) {
            if (orderItem.getMaskImgFile() == null ||
                    orderItem.getMaskImgFile().getFilePreview() == null ||
                    orderItem.getMaskImgFile().getFilePreview().getRaw() == null) {
                throw new RuntimeException("存在异形工艺但蒙版图片不存在：" + orderItem.getOrderItemId());
            }
        }

        // 2. 获取切割坐标
        List<Double> cuttingCoordinates = procedureService.generateCuttingCoordinates(processingNodes);

        // 3. 准备分切的图片 URL 列表
        List<String> imageUrlsToCut = new ArrayList<>();

        // 如果存在异形工艺，productionImgUrl 和 maskImgUrl 都需要分切
        if (hasSpecialShape) {
            String productionImgUrl = orderItem.getProductionImgFile() != null
                    && orderItem.getProductionImgFile().getFilePreview() != null
                    ? orderItem.getProductionImgFile().getFilePreview().getRaw()
                    : null;
            String maskImgUrl = orderItem.getMaskImgFile() != null
                    && orderItem.getMaskImgFile().getFilePreview() != null
                    ? orderItem.getMaskImgFile().getFilePreview().getRaw()
                    : null;

            if (productionImgUrl != null) imageUrlsToCut.add(productionImgUrl);
            if (maskImgUrl != null) imageUrlsToCut.add(maskImgUrl);
        } else {
            // 不存在异形工艺，只需要 productionImgUrl
            String productionImgUrl = orderItem.getProductionImgFile() != null
                    && orderItem.getProductionImgFile().getFilePreview() != null
                    ? orderItem.getProductionImgFile().getFilePreview().getRaw()
                    : null;

            if (productionImgUrl != null) imageUrlsToCut.add(productionImgUrl);
        }

        // 4. 保存待分切的中间数据
        List<ProductionPiece> pendingCutPieces = savePendingCutPieces(
                orderItem,
                imageUrlsToCut,
                cuttingCoordinates
        );

        // 5. 异步调用图像分切接口
        List<CutResult> cutResults = callImageCuttingApiAsync(pendingCutPieces);

        // 6. 根据分切结果生成新的 ProductionPiece
        List<ProductionPiece> cutPieces = createPiecesFromCutResults(orderItem, cutResults);

        // 7. 如果存在异形工艺，继续调用抠图接口
        if (hasSpecialShape && !cutPieces.isEmpty()) {
            return processMaskingAfterCutting(orderItem, cutPieces);
        }

        return cutPieces;
    }

    /**
     * 情况二：不需要分切但需要抠图的处理流程
     */
    private List<ProductionPiece> processWithoutCuttingButWithMasking(OrderItem orderItem) {
        // 1. 校验 maskImgUrl 是否存在
        if (orderItem.getMaskImgFile() == null ||
                orderItem.getMaskImgFile().getFilePreview() == null ||
                orderItem.getMaskImgFile().getFilePreview().getRaw() == null) {
            throw new RuntimeException("存在异形工艺但蒙版图片不存在：" + orderItem.getOrderItemId());
        }

        // 2. 直接使用 orderItem 的图片信息调用抠图接口
        String productionImgUrl = orderItem.getProductionImgFile() != null
                && orderItem.getProductionImgFile().getFilePreview() != null
                ? orderItem.getProductionImgFile().getFilePreview().getRaw()
                : null;
        String maskImgUrl = orderItem.getMaskImgFile() != null
                && orderItem.getMaskImgFile().getFilePreview() != null
                ? orderItem.getMaskImgFile().getFilePreview().getRaw()
                : null;

        // 3. 创建待抠图的 ProductionPiece，将 maskImgUrl 存储在 maskImageFile 字段中
        List<ProductionPiece> pendingPieces = new ArrayList<>();
        ProductionPiece pendingPiece = procedureService.createProductionPiece(
                orderItem,
                "PENDING_MASK",
                productionImgUrl
        );

        // 设置 maskImageFile
        pendingPiece.setMaskImageFile(orderItem.getMaskImgFile());

        pendingPieces.add(pendingPiece);
        productionPieceService.addProductionPiece(pendingPiece);

        // 4. 异步调用抠图接口
        List<MaskResult> maskResults = callImageMaskingApiAsyncForSinglePiece(pendingPiece);

        // 5. 根据抠图结果生成新的 ProductionPiece，并删除老的
        return createPiecesFromMaskResultsAndDeleteOldOnes(
                orderItem,
                pendingPieces,
                maskResults
        );
    }

    /**
     * 情况三：既不需要分切也不需要抠图的处理流程
     */
    private List<ProductionPiece> processWithoutCuttingAndMasking(OrderItem orderItem) {
        // 直接使用 orderItem 信息生成 ProductionPiece
        String productionImgUrl = orderItem.getProductionImgFile() != null
                && orderItem.getProductionImgFile().getFilePreview() != null
                ? orderItem.getProductionImgFile().getFilePreview().getRaw()
                : null;

        ProductionPiece piece = procedureService.createProductionPiece(
                orderItem,
                "ORIGINAL",
                productionImgUrl
        );

        productionPieceService.addProductionPiece(piece);

        List<ProductionPiece> pieces = new ArrayList<>();
        pieces.add(piece);

        return pieces;
    }

    /**
     * 分切后处理异形工艺（抠图）
     */
    private List<ProductionPiece> processMaskingAfterCutting(OrderItem orderItem, List<ProductionPiece> cutPieces) {
        // 保存待抠图的中间数据
        List<ProductionPiece> pendingMaskPieces = savePendingMaskPieces(cutPieces);

        // 异步调用抠图接口
        List<MaskResult> maskResults = callImageMaskingApiAsync(pendingMaskPieces);

        // 根据抠图结果生成新的 ProductionPiece，并删除老的 ProductionPiece
        return createPiecesFromMaskResultsAndDeleteOldOnes(
                orderItem,
                pendingMaskPieces,
                maskResults
        );
    }

    /**
     * 保存待分切的中间数据
     */
    private List<ProductionPiece> savePendingCutPieces(OrderItem orderItem, List<String> imageUrls, List<Double> cuttingCoordinates) {
        List<ProductionPiece> pendingPieces = new ArrayList<>();

        for (String imageUrl : imageUrls) {
            ProductionPiece piece = new ProductionPiece();
            piece.setOrderItemId(orderItem.getOrderItemId());
            piece.setProductionPieceType("PENDING_CUT");
            piece.setStatus("PENDING");
            piece.setQuantity(orderItem.getQuantity());
            piece.setTemplateCode(imageUrl);

            // 将切割坐标转换为 JSON 字符串存储在 positionCode 字段中
            StringBuilder coordinatesJson = new StringBuilder("[");
            for (int i = 0; i < cuttingCoordinates.size(); i++) {
                coordinatesJson.append(cuttingCoordinates.get(i));
                if (i < cuttingCoordinates.size() - 1) {
                    coordinatesJson.append(",");
                }
            }
            coordinatesJson.append("]");
            piece.setPositionCode(coordinatesJson.toString());

            productionPieceService.addProductionPiece(piece);
            pendingPieces.add(piece);
        }

        return pendingPieces;
    }

    /**
     * 保存待抠图的中间数据
     */
    private List<ProductionPiece> savePendingMaskPieces(List<ProductionPiece> pieces) {
        for (ProductionPiece piece : pieces) {
            piece.setProductionPieceType("PENDING_MASK");
            piece.setStatus("PENDING");
            productionPieceService.updateProductionPiece(piece);
        }
        return pieces;
    }

    /**
     * 根据分切结果生成新的 ProductionPiece
     */
    private List<ProductionPiece> createPiecesFromCutResults(OrderItem orderItem, List<CutResult> cutResults) {
        List<ProductionPiece> resultPieces = new ArrayList<>();

        for (CutResult cutResult : cutResults) {
            ProductionPiece piece = procedureService.createProductionPiece(
                    orderItem,
                    "CUT",
                    cutResult.getImageUrl()
            );
            productionPieceService.addProductionPiece(piece);
            resultPieces.add(piece);
        }

        return resultPieces;
    }

    /**
     * 根据抠图结果生成新的 ProductionPiece，并删除老的 ProductionPiece
     */
    private List<ProductionPiece> createPiecesFromMaskResultsAndDeleteOldOnes(
            OrderItem orderItem,
            List<ProductionPiece> oldPieces,
            List<MaskResult> maskResults) {

        List<ProductionPiece> newPieces = new ArrayList<>();

        // 生成新的 ProductionPiece
        for (MaskResult maskResult : maskResults) {
            ProductionPiece piece = procedureService.createProductionPiece(
                    orderItem,
                    "MASK",
                    maskResult.getImageUrl()
            );
            productionPieceService.addProductionPiece(piece);
            newPieces.add(piece);
        }

        // 删除老的用来抠图的 ProductionPiece
        for (ProductionPiece oldPiece : oldPieces) {
            productionPieceService.deleteProductionPiece(oldPiece.getId());
        }

        return newPieces;
    }

    /**
     * 异步调用图像分切接口
     */
    private List<CutResult> callImageCuttingApiAsync(List<ProductionPiece> pendingPieces) {
        // TODO: 实现异步调用
        // 应该使用异步 HTTP 客户端，等待回调
        // 这里暂时同步返回示例数据

        List<CutResult> results = new ArrayList<>();
        for (ProductionPiece piece : pendingPieces) {
            CutResult result = new CutResult();
            result.setImageUrl(piece.getTemplateCode() + "_cut");
            results.add(result);
        }

        return results;
    }

    /**
     * 异步调用抠图接口（多个 ProductionPiece）
     */
    private List<MaskResult> callImageMaskingApiAsync(List<ProductionPiece> pendingPieces) {
        // TODO: 实现异步调用
        // 应该使用异步 HTTP 客户端，等待回调
        // 这里暂时同步返回示例数据

        List<MaskResult> results = new ArrayList<>();
        for (ProductionPiece piece : pendingPieces) {
            MaskResult result = new MaskResult();
            result.setImageUrl(piece.getTemplateCode() + "_mask");
            results.add(result);
        }

        return results;
    }

    /**
     * 异步调用抠图接口（单个 ProductionPiece）
     */
    private List<MaskResult> callImageMaskingApiAsyncForSinglePiece(ProductionPiece pendingPiece) {
        // 从 maskImageFile 中获取 maskImgUrl
        String maskImgUrl = null;
        if (pendingPiece.getMaskImageFile() != null
                && pendingPiece.getMaskImageFile().getFilePreview() != null) {
            maskImgUrl = pendingPiece.getMaskImageFile().getFilePreview().getRaw();
        }

        // TODO: 实际调用时需要传入 maskImgUrl
        List<ProductionPiece> pieces = new ArrayList<>();
        pieces.add(pendingPiece);
        return callImageMaskingApiAsync(pieces);
    }

    /**
     * 更新订单项状态为生产中
     */
    private void updateOrderItemStatusToInProduction(String orderItemId) {
        try {
            OrderItem orderItem = orderItemService.findById(orderItemId);
            if (orderItem != null) {
                orderItem.setStatus(OrderStatus.IN_PRODUCTION);
                orderItemService.updateOrderItem(orderItem);
            }
        } catch (Exception e) {
            System.err.println("更新订单项状态失败：" + orderItemId + ", 错误：" + e.getMessage());
        }
    }

    /**
     * 将订单下所有订单项标记为失败状态
     *
     * @param orderId 订单 ID
     */
    private void markAllOrderItemsAsFailed(String orderId, String manufacturerId, String reason) {
        try {
            List<OrderItem> orderItems = orderItemService.findByOrderId(orderId, manufacturerId, 1, 100);
            if (orderItems != null && !orderItems.isEmpty()) {
                for (OrderItem item : orderItems) {
                    orderItemService.markAsFailed(item.getOrderItemId(), reason);
                }
            }
        } catch (Exception e) {
            // 记录更新状态失败的错误，但不抛出，避免覆盖原始异常
            System.err.println("批量更新订单项状态失败：" + orderId + ", 错误：" + e.getMessage());
        }
    }

    /**
     * 调用图像分切 API（已废弃，保留用于兼容）
     *
     * @param productionImgUrl   生产图片 URL
     * @param cuttingCoordinates 切割坐标列表 [x1, y1, x2, y2, ...]
     * @return 分切结果列表
     */
    @Deprecated
    private List<CutResult> callImageCuttingApi(String productionImgUrl, List<Double> cuttingCoordinates) {
        return new ArrayList<>();
    }

    /**
     * 调用图像抠图 API（已废弃，保留用于兼容）
     *
     * @param productionImgUrl 生产图片 URL
     * @param maskImgUrl       蒙版图片 URL
     * @return 抠图结果列表
     */
    @Deprecated
    private List<MaskResult> callImageMaskingApi(String productionImgUrl, String maskImgUrl) {
        return new ArrayList<>();
    }

    /**
     * 调用 API 生成 PLT 文件
     *
     * @param orderItemId 订单项 ID
     * @param pltApiUrl   PLT 生成 API 的 URL
     * @return PLT 文件生成结果
     */
    public PltGenerateResult generatePltFile(String orderItemId, String pltApiUrl) {
        try {
            // 1. 根据 orderItemId 查询 OrderItem 信息
            OrderItem orderItem = orderItemService.findById(orderItemId);
            if (orderItem == null) {
                throw new RuntimeException("订单项不存在：" + orderItemId);
            }

            // 2. 解析工艺流程
            String processingFlow = orderItem.getProcessingFlow();
            if (processingFlow == null || processingFlow.trim().isEmpty()) {
                throw new RuntimeException("工艺流程不能为空：" + orderItemId);
            }

            ProcedureFlow procedureFlow = orderItem.getProcedureFlow();
            List<ProcedureFlowNode> processingNodes = procedureFlow.getNodes();

            // 3. 构建请求参数
            Map<String, Object> requestParams = new HashMap<>();
            requestParams.put("orderItemId", orderItem.getOrderItemId());

            String productionImgUrl = orderItem.getProductionImgFile() != null
                    && orderItem.getProductionImgFile().getFilePreview() != null
                    ? orderItem.getProductionImgFile().getFilePreview().getRaw()
                    : null;
            requestParams.put("productionImgUrl", productionImgUrl);

            String materialName = null;
            if (orderItem.getMaterial() != null && orderItem.getMaterial().getMaterialSnapshot() != null) {
                materialName = orderItem.getMaterial().getMaterialSnapshot().getName();
            }
            requestParams.put("material", materialName);
            requestParams.put("quantity", orderItem.getQuantity());
            requestParams.put("processingNodes", processingNodes.stream()
                    .map(ProcedureFlowNode::getNodeName)
                    .toArray(String[]::new));

            // 4. 如果有裁切工艺，添加切割坐标
            boolean hasCutting = procedureFlowService.hasNodeWithName(processingNodes, "裁切");
            if (hasCutting) {
                requestParams.put("cuttingCoordinates", procedureService.generateCuttingCoordinates(processingNodes));
            }

            // 5. 调用 PLT 生成 API
            PltGenerateResult result = callPltGenerationApi(pltApiUrl, requestParams);

            // 如果失败，标记为失败状态
            if (!result.isSuccess()) {
                orderItemService.markAsFailed(orderItemId, result.getMessage());
                throw new RuntimeException("PLT 文件生成失败：" + result.getMessage());
            }

            return result;

        } catch (Exception e) {
            // 失败时更新订单状态为失败，并记录失败原因
            orderItemService.markAsFailed(orderItemId, e.getMessage());
            throw e;
        }
    }

    /**
     * 批量生成 PLT 文件
     *
     * @param orderItemIds 订单项 ID 列表
     * @param pltApiUrl    PLT 生成 API 的 URL
     * @return PLT 文件生成结果列表
     */
    public List<PltGenerateResult> batchGeneratePltFiles(List<String> orderItemIds, String pltApiUrl) {
        List<PltGenerateResult> results = new ArrayList<>();

        for (String orderItemId : orderItemIds) {
            try {
                PltGenerateResult result = generatePltFile(orderItemId, pltApiUrl);
                results.add(result);
            } catch (Exception e) {
                // 错误已经在 generatePltFile 中处理，这里只记录日志
                System.err.println("处理订单项失败：" + orderItemId + ", 错误：" + e.getMessage());
            }
        }

        return results;
    }

    /**
     * 调用 PLT 生成 API
     *
     * @param apiUrl API 地址
     * @param params 请求参数
     * @return PLT 生成结果
     */
    private PltGenerateResult callPltGenerationApi(String apiUrl, Map<String, Object> params) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(params, headers);

            // 调用 POST 接口
            ResponseEntity<PltApiResponse> response = restTemplate.postForEntity(
                    apiUrl,
                    requestEntity,
                    PltApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                PltApiResponse apiResponse = response.getBody();

                PltGenerateResult result = new PltGenerateResult();
                result.setSuccess(true);
                result.setPltFileUrl(apiResponse.getPltFileUrl());
                result.setPltFileName(apiResponse.getPltFileName());
                result.setFileSize(apiResponse.getFileSize());
                result.setMessage("PLT 文件生成成功");

                return result;
            } else {
                PltGenerateResult result = new PltGenerateResult();
                result.setSuccess(false);
                result.setMessage("PLT 文件生成失败，HTTP 状态码：" + response.getStatusCode());
                return result;
            }

        } catch (Exception e) {
            PltGenerateResult result = new PltGenerateResult();
            result.setSuccess(false);
            result.setMessage("调用 PLT 生成 API 异常：" + e.getMessage());
            return result;
        }
    }


}
