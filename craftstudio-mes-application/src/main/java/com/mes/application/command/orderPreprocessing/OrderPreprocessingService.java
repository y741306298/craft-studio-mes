package com.mes.application.command.orderPreprocessing;

import com.mes.application.command.orderPreprocessing.vo.CutResult;
import com.mes.application.command.orderPreprocessing.vo.MaskResult;
import com.mes.application.command.orderPreprocessing.vo.PltApiResponse;
import com.mes.application.command.orderPreprocessing.vo.PltGenerateResult;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderPreprocessingService {

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ProductionPieceService productionPieceService;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 订单预处理：根据 orderItemId 查询订单信息，解析工艺流程并处理裁切和异形工艺
     * 
     * @param orderItemId 订单项 ID
     * @return 处理后的生产零件列表
     */
    public List<ProductionPiece> preprocessOrder(String orderItemId) {
        // 1. 根据 orderItemId 查询 OrderItem 信息
        OrderItem orderItem = orderItemService.findById(orderItemId);
        if (orderItem == null) {
            throw new RuntimeException("订单项不存在：" + orderItemId);
        }

        List<ProductionPiece> resultPieces = new ArrayList<>();

        // 2. 根据 processingFlow 信息按"-"切分成 List<ProcedureFlowNode>
        String processingFlow = orderItem.getProcessingFlow();
        if (processingFlow == null || processingFlow.trim().isEmpty()) {
            throw new RuntimeException("工艺流程不能为空：" + orderItemId);
        }

        List<ProcedureFlowNode> processingNodes = parseProcessingFlow(processingFlow);

        // 3. 判断是否有"裁切"工艺
        boolean hasCutting = processingNodes.stream()
                .anyMatch(node -> "裁切".equals(node.getNodeName()));

        if (hasCutting) {
            // 4. 调用图像分切接口
            List<CutResult> cutResults = callImageCuttingApi(
                    orderItem.getProductionImgUrl(),
                    generateCuttingCoordinates(processingNodes)
            );

            // 5. 保存分切结果为新的 ProductionPiece
            for (CutResult cutResult : cutResults) {
                ProductionPiece piece = createProductionPiece(orderItem, "CUT", cutResult.getImageUrl());
                productionPieceService.addProductionPiece(piece);
                resultPieces.add(piece);
            }
        }

        // 6. 判断是否有"异形"工艺
        boolean hasSpecialShape = processingNodes.stream()
                .anyMatch(node -> "异形".equals(node.getNodeName()));

        if (hasSpecialShape) {
            // 7. 调用抠图接口
            List<MaskResult> maskResults = callImageMaskingApi(
                    orderItem.getProductionImgUrl(),
                    orderItem.getMaskImgUrl()
            );

            // 8. 保存抠图结果为新的 ProductionPiece
            for (MaskResult maskResult : maskResults) {
                ProductionPiece piece = createProductionPiece(orderItem, "MASK", maskResult.getImageUrl());
                productionPieceService.addProductionPiece(piece);
                resultPieces.add(piece);
            }
        }

        return resultPieces;
    }

    /**
     * 解析工艺流程字符串，转换为 ProcedureFlowNode 列表
     * 
     * @param processingFlow 工艺流程字符串，格式如"开料 - 裁切 - 封边 - 异形"
     * @return 工艺节点列表
     */
    private List<ProcedureFlowNode> parseProcessingFlow(String processingFlow) {
        String[] nodeNames = processingFlow.split("-");
        List<ProcedureFlowNode> nodes = new ArrayList<>();

        for (int i = 0; i < nodeNames.length; i++) {
            ProcedureFlowNode node = new ProcedureFlowNode();
            node.setNodeId("NODE_" + i);
            node.setNodeName(nodeNames[i].trim());
            node.setNodeOrder(i);
            nodes.add(node);
        }

        return nodes;
    }

    /**
     * 生成切割直线的 xy 坐标值
     * 这里需要根据实际业务逻辑生成切割坐标
     * 
     * @param processingNodes 工艺节点列表
     * @return 切割坐标列表 [x1, y1, x2, y2, ...]
     */
    private List<Double> generateCuttingCoordinates(List<ProcedureFlowNode> processingNodes) {
        // TODO: 根据实际业务需求生成切割坐标
        // 示例：返回一个默认的切割线坐标
        return Arrays.asList(
                0.0, 0.0,      // 起点 (x1, y1)
                1000.0, 1000.0 // 终点 (x2, y2)
        );
    }

    /**
     * 调用图像分切 API
     * 
     * @param productionImgUrl 生产图片 URL
     * @param cuttingCoordinates 切割坐标列表 [x1, y1, x2, y2, ...]
     * @return 分切结果列表
     */
    private List<CutResult> callImageCuttingApi(String productionImgUrl, List<Double> cuttingCoordinates) {
        // TODO: 实现实际的 HTTP API 调用
        // 示例代码结构：
        // Map<String, Object> params = new HashMap<>();
        // params.put("imageUrl", productionImgUrl);
        // params.put("coordinates", cuttingCoordinates);
        // CutResponse response = restTemplate.postForObject(CUTTING_API_URL, params, CutResponse.class);
        // return response.getResults();

        // 临时返回空列表
        return new ArrayList<>();
    }

    /**
     * 调用图像抠图 API
     * 
     * @param productionImgUrl 生产图片 URL
     * @param maskImgUrl 蒙版图片 URL
     * @return 抠图结果列表
     */
    private List<MaskResult> callImageMaskingApi(String productionImgUrl, String maskImgUrl) {
        // TODO: 实现实际的 HTTP API 调用
        // 示例代码结构：
        // Map<String, Object> params = new HashMap<>();
        // params.put("imageUrl", productionImgUrl);
        // params.put("maskUrl", maskImgUrl);
        // MaskResponse response = restTemplate.postForObject(MASKING_API_URL, params, MaskResponse.class);
        // return response.getResults();

        // 临时返回空列表
        return new ArrayList<>();
    }

    /**
     * 创建生产工件实体
     * 
     * @param orderItem 订单项
     * @param pieceType 工件类型（CUT/MASK）
     * @param imageUrl 处理后的图片 URL
     * @return 生产工件实体
     */
    private ProductionPiece createProductionPiece(OrderItem orderItem, String pieceType, String imageUrl) {
        ProductionPiece piece = new ProductionPiece();
        piece.setOrderItemId(orderItem.getOrderItemId());
        piece.setProcedureFlowId(orderItem.getProcedureFlowId());
        piece.setProductionPieceType(pieceType);
        piece.setStatus("PENDING");
        piece.setQuantity(orderItem.getQuantity());
        piece.setTemplateCode(imageUrl); // 将处理后的图片 URL 保存到 templateCode
        return piece;
    }

    /**
     * 调用 API 生成 PLT 文件
     * 
     * @param orderItemId 订单项 ID
     * @param pltApiUrl PLT 生成 API 的 URL
     * @return PLT 文件生成结果
     */
    public PltGenerateResult generatePltFile(String orderItemId, String pltApiUrl) {
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

        List<ProcedureFlowNode> processingNodes = parseProcessingFlow(processingFlow);

        // 3. 构建请求参数
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("orderItemId", orderItem.getOrderItemId());
        requestParams.put("productionImgUrl", orderItem.getProductionImgUrl());
        requestParams.put("material", orderItem.getMaterial());
        requestParams.put("quantity", orderItem.getQuantity());
        requestParams.put("processingNodes", processingNodes.stream()
                .map(ProcedureFlowNode::getNodeName)
                .toArray(String[]::new));

        // 4. 如果有裁切工艺，添加切割坐标
        boolean hasCutting = processingNodes.stream()
                .anyMatch(node -> "裁切".equals(node.getNodeName()));
        if (hasCutting) {
            requestParams.put("cuttingCoordinates", generateCuttingCoordinates(processingNodes));
        }

        // 5. 调用 PLT 生成 API
        PltGenerateResult result = callPltGenerationApi(pltApiUrl, requestParams);

        return result;
    }

    /**
     * 批量生成 PLT 文件
     * 
     * @param orderItemIds 订单项 ID 列表
     * @param pltApiUrl PLT 生成 API 的 URL
     * @return PLT 文件生成结果列表
     */
    public List<PltGenerateResult> batchGeneratePltFiles(List<String> orderItemIds, String pltApiUrl) {
        List<PltGenerateResult> results = new ArrayList<>();

        for (String orderItemId : orderItemIds) {
            try {
                PltGenerateResult result = generatePltFile(orderItemId, pltApiUrl);
                results.add(result);
            } catch (Exception e) {
                // 记录错误并继续处理其他订单
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
