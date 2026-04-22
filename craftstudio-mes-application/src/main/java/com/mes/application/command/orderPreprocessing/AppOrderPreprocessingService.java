package com.mes.application.command.orderPreprocessing;

import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.ImageMaskRequest;
import com.mes.application.command.api.resp.ImageMaskResponse;
import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.CallbackCustomValue;
import com.mes.application.command.api.vo.UploadConfig;
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
import com.piliofpala.craftstudio.shared.infra.cloud.platforms.alicloud.AliCloudAuthService;
import com.piliofpala.craftstudio.shared.infra.cloud.storage.dto.ObjectStorageTempAuthConfig;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private AlgorithmCoreApiService algorithmCoreApiService;

    @Autowired
    private AliCloudAuthService aliCloudAuthService;

    @Value("${external.callbackApi.generate_mask_files}")
    private String generateMaskFilesApiUrl;


    /**
     * 订单预处理：根据 orderId 查询订单下的所有订单项，解析工艺流程并处理裁切和异形工艺
     *
     */
    public void preprocessOrder(List<OrderItem> orderItems) {
        List<ProductionPiece> resultPieces = new ArrayList<>();

        // 2. 遍历每个订单项进行处理
        for (OrderItem orderItem : orderItems) {
            try {
                //转化成生产零件，同时判断是否需要调用抠图算法
                List<ProductionPiece> pieces = processSingleOrderItem(orderItem);
                // 处理成功，将订单项状态改为生产中
                updateOrderItemStatusToInProduction(orderItem.getOrderItemId());
                if(pieces != null){
                    resultPieces.addAll(pieces);
                    //说明直接生成了零件，预处理完成后，让所有的零件进入第一个节点，否则说明在等待处理零件，暂不处理
                    for (ProductionPiece resultPiece : resultPieces) {
                        appPieceCirculationService.moveToNextNode(resultPiece, 1);
                        //暂时默认将零件状态改为待排版
                        productionPieceService.updateProductionPieceStatusByproductionPieceId(resultPiece.getProductionPieceId(), ProductionPieceStatus.PENDING_TYPESITTING);
                    }
                }
            } catch (Exception e) {
                // 单个订单项处理失败时，标记该订单项为失败状态，但继续处理其他订单项
                orderItemService.markAsFailed(orderItem.getOrderItemId(), e.getMessage());
                System.err.println("处理订单项失败：" + orderItem.getOrderItemId() + ", 错误：" + e.getMessage());
            }
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
        //将订单工艺转化为生产工序
        ProcedureFlow procedureFlow = procedureFlowService.parseProcessingFlow(orderItem.getProcedureFlow());
        List<ProcedureFlowNode> processingNodes = procedureFlow.getNodes();

        // 2. 判断是否有"裁切"工艺
        boolean hasCutting = procedureFlowService.hasNodeWithName(processingNodes, "超幅拼接");

        // 3. 判断是否有"异形"工艺
        boolean hasSpecialShape = procedureFlowService.hasNodeWithName(processingNodes, "异形切割");

        // 4. 根据不同情况处理
        if (!hasCutting && !hasSpecialShape ) {
            // 情况一：既不需要分切也不需要抠图,直接生成生产零件
            return processWithoutCuttingAndMasking(orderItem,procedureFlow);
        } else{
            // 情况二：需要分切或抠图, 暂时不生成生产零件
            ImageMaskRequest imageMaskRequest = ImageMaskRequest.processWithCutting(orderItem, processingNodes, hasSpecialShape,hasCutting);
            //配置oss信息
            ObjectStorageTempAuthConfig objectStorageTempAuthConfig = aliCloudAuthService.getObjectStorageTempAuthConfig(orderItem.getOrderItemId());
            UploadConfig uploadConfig = new UploadConfig();
            uploadConfig.setUploadPath("pieceImg/");
            uploadConfig.setOssConfig(objectStorageTempAuthConfig);
            imageMaskRequest.setUploadConfig(uploadConfig);
            //配置callback信息
            CallbackConfig callbackConfig = new CallbackConfig();
            callbackConfig.setCallbackUrl(generateMaskFilesApiUrl);
            CallbackCustomValue callbackCustomValue = new CallbackCustomValue();
            callbackCustomValue.setId(orderItem.getOrderItemId());
            callbackConfig.setCallbackCustomValue(callbackCustomValue);
            imageMaskRequest.setCallbackConfig(callbackConfig);
            ImageMaskResponse imageMaskResponse = algorithmCoreApiService.generateMaskFilesAsync(imageMaskRequest);
            return null;
        }
    }



    /**
     * 情况三：既不需要分切也不需要抠图的处理流程
     */
    private List<ProductionPiece> processWithoutCuttingAndMasking(OrderItem orderItem,ProcedureFlow procedureFlow) {
        // 直接使用 orderItem 信息生成 ProductionPiece
        String productionImgUrl = orderItem.getProductionImgFile() != null
                && orderItem.getProductionImgFile().getFilePreview() != null
                ? orderItem.getProductionImgFile().getFilePreview().getRaw()
                : null;
        String maskImgUrl = orderItem.getMaskImgFile() != null
                && orderItem.getMaskImgFile().getFilePreview() != null
                ? orderItem.getMaskImgFile().getFilePreview().getRaw()
                : null;
        ProductionPiece piece = procedureService.createProductionPiece(
                orderItem,
                "ORIGINAL",
                productionImgUrl,
                procedureFlow,
                maskImgUrl
        );

        productionPieceService.addProductionPiece(piece);

        List<ProductionPiece> pieces = new ArrayList<>();
        pieces.add(piece);

        return pieces;
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
     * 处理图像蒙版回调，根据回调结果生成生产零件
     *
     * @param response 算法服务返回的蒙版结果
     * @param orderItemId 订单项ID（从callbackCustomValue传入）
     */
    public void handleGenerateMaskFilesCallback(ImageMaskResponse response, String orderItemId) {
        try {
            // 1. 验证回调响应
            if (response == null) {
                throw new RuntimeException("回调响应不能为空");
            }

            if ("error".equals(response.getStatus())) {
                String errorMsg = response.getError() != null ? response.getError() : "未知错误";
                orderItemService.markAsFailed(orderItemId, "图像蒙版处理失败：" + errorMsg);
                System.err.println("图像蒙版处理失败，订单项ID：" + orderItemId + "，错误：" + errorMsg);
                return;
            }

            // 2. 查询订单项
            OrderItem orderItem = orderItemService.findByOrderItemId(orderItemId);
            if (orderItem == null) {
                throw new RuntimeException("订单项不存在：" + orderItemId);
            }

            // 3. 检查pairs是否为空
            if (response.getPairs() == null || response.getPairs().isEmpty()) {
                orderItemService.markAsFailed(orderItemId, "图像蒙版处理结果为空");
                System.err.println("图像蒙版处理结果为空，订单项ID：" + orderItemId);
                return;
            }

            // 4. 根据pairs生成生产零件
            List<ProductionPiece> resultPieces = new ArrayList<>();
            for (ImageMaskResponse.Pair pair : response.getPairs()) {
                try {
                    String rawImageUrl = pair.getImg();
                    String maskedImageUrl = pair.getSvg();
                    
                    ProcedureFlow originalFlow = orderItem.getProcedureFlow();
                    ProcedureFlow newProcedureFlow = new ProcedureFlow();
                    newProcedureFlow.setProcedureFlowId(originalFlow.getProcedureFlowId());
                    newProcedureFlow.setProcedureFlowName(originalFlow.getProcedureFlowName());
                    newProcedureFlow.setFlowStatus(originalFlow.getFlowStatus());
                    if (originalFlow.getNodes() != null) {
                        List<ProcedureFlowNode> newNodes = new ArrayList<>();
                        for (ProcedureFlowNode node : originalFlow.getNodes()) {
                            ProcedureFlowNode newNode = new ProcedureFlowNode();
                            newNode.setNodeId(node.getNodeId());
                            newNode.setNodeName(node.getNodeName());
                            newNode.setNodeOrder(node.getNodeOrder());
                            newNode.setNodeStatus(node.getNodeStatus());
                            newNode.setPieceQuantity(node.getPieceQuantity());
                            newNodes.add(newNode);
                        }
                        newProcedureFlow.setNodes(newNodes);
                    }
                    
                    ProcedureFlow parsedFlow = procedureFlowService.parseProcessingFlow(newProcedureFlow);
                    
                    if (rawImageUrl != null && !rawImageUrl.isEmpty()) {
                        ProductionPiece piece = procedureService.createProductionPiece(
                                orderItem,
                                "ORIGINAL",
                                rawImageUrl,
                                parsedFlow,
                                maskedImageUrl
                        );
                        productionPieceService.addProductionPiece(piece);
                        resultPieces.add(piece);
                    }
                } catch (Exception e) {
                    System.err.println("生成生产零件失败：" + e.getMessage());
                    throw e;
                }
            }

            // 5. 如果成功生成了零件，更新订单项状态并推进到下一个节点
            if (!resultPieces.isEmpty()) {
                updateOrderItemStatusToInProduction(orderItemId);
                
                for (ProductionPiece resultPiece : resultPieces) {
                    appPieceCirculationService.moveToNextNode(resultPiece, 1);
                    productionPieceService.updateProductionPieceStatusByproductionPieceId(
                            resultPiece.getProductionPieceId(), 
                            ProductionPieceStatus.PENDING_TYPESITTING
                    );
                }
                
                System.out.println("成功为订单项 " + orderItemId + " 生成 " + resultPieces.size() + " 个生产零件");
            } else {
                orderItemService.markAsFailed(orderItemId, "未能生成任何生产零件");
                System.err.println("未能生成任何生产零件，订单项ID：" + orderItemId);
            }

        } catch (Exception e) {
            // 处理异常，标记订单项为失败
            orderItemService.markAsFailed(orderItemId, "处理图像蒙版回调异常：" + e.getMessage());
            System.err.println("处理图像蒙版回调异常，订单项ID：" + orderItemId + "，错误：" + e.getMessage());
            throw e;
        }
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
