package com.mes.domain.manufacturer.productionPiece.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.manufacturer.productionPiece.repository.ProductionPieceRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductionPieceService {

    @Autowired
    private ProductionPieceRepository productionPieceRepository;

    /**
     * 根据多条件查询生产工件（支持分页）
     * @param status 状态
     * @param material 材质（从关联的 TypesettingInfo 获取，这里暂时不支持）
     * @param nodeName 工序节点名称
     * @param startDate 开始时间（可为 null）
     * @param endDate 结束时间（可为 null）
     * @param current 当前页码
     * @param size 每页大小
     * @return 生产工件列表
     */
    public List<ProductionPiece> findProductionPiecesByConditions(
            String manufacturerId,
            String status,
            String material,
            String nodeName,
            Date startDate,
            Date endDate,
            int current,
            int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerId",manufacturerId);
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (StringUtils.isNotBlank(material)) {
            filters.put("material", material);
        }
        if (startDate != null) {
            filters.put("createTime_gte", startDate);
        }
        if (endDate != null) {
            filters.put("createTime_lte", endDate);
        }

        List<ProductionPiece> allItems = productionPieceRepository.filterList(current, size, filters);

        if (StringUtils.isNotBlank(nodeName)) {
            return allItems.stream()
                .filter(piece -> {
                    if (piece.getProcedureFlow() != null && piece.getProcedureFlow().getNodes() != null) {
                        return piece.getProcedureFlow().getNodes().stream()
                            .anyMatch(node -> nodeName.equals(node.getNodeName()));
                    }
                    return false;
                })
                .collect(Collectors.toList());
        }

        return allItems;
    }

    /**
     * 根据多条件查询生产工件总数
     * @param status 状态
     * @param material 材质
     * @param nodeName 工序节点名称
     * @param startDate 开始时间（可为 null）
     * @param endDate 结束时间（可为 null）
     * @return 总数
     */
    public long countProductionPiecesByConditions(
            String status,
            String material,
            String nodeName,
            Date startDate,
            Date endDate) {
        Map<String, Object> filters = new HashMap<>();

        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (StringUtils.isNotBlank(material)) {
            filters.put("material", material);
        }
        if (startDate != null) {
            filters.put("createTime_gte", startDate);
        }
        if (endDate != null) {
            filters.put("createTime_lte", endDate);
        }

        long total = filters.isEmpty() ? productionPieceRepository.total() : productionPieceRepository.filterTotal(filters);

        if (StringUtils.isNotBlank(nodeName)) {
            List<ProductionPiece> allItems = filters.isEmpty() ? 
                productionPieceRepository.filterList(1, Integer.MAX_VALUE, new HashMap<>()) :
                productionPieceRepository.filterList(1, Integer.MAX_VALUE, filters);
            return allItems.stream()
                .filter(piece -> {
                    if (piece.getProcedureFlow() != null && piece.getProcedureFlow().getNodes() != null) {
                        return piece.getProcedureFlow().getNodes().stream()
                            .anyMatch(node -> nodeName.equals(node.getNodeName()));
                    }
                    return false;
                })
                .count();
        }

        return total;
    }

    /**
     * 根据订单项目 ID 查询生产工件（支持分页）
     */
    public List<ProductionPiece> findProductionPiecesByOrderItemId(String orderItemId, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(orderItemId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单项目 ID 不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("orderItemId", orderItemId);
        return productionPieceRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 根据工艺路线 ID 查询生产工件（支持分页）
     */
    public List<ProductionPiece> findProductionPiecesByProcedureFlowId(String procedureFlowId, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(procedureFlowId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工艺路线 ID 不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("procedureFlowId", procedureFlowId);
        return productionPieceRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 获取生产工件总数
     */
    public long getTotalCount(String orderItemId) {
        if (StringUtils.isNotBlank(orderItemId)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("orderItemId", orderItemId);
            return productionPieceRepository.totalByFuzzySearch(searchFilters);
        } else {
            return productionPieceRepository.total();
        }
    }

    /**
     * 添加生产工件
     */
    public ProductionPiece addProductionPiece(ProductionPiece productionPiece) {
        if (productionPiece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不能为空");
        }
        if (StringUtils.isBlank(productionPiece.getOrderItemId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单项目 ID 不能为空");
        }
        if (StringUtils.isBlank(productionPiece.getProductionPieceType())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件类型不能为空");
        }

        // 生成唯一的 productionPieceId
        if (StringUtils.isBlank(productionPiece.getProductionPieceId())) {
            productionPiece.setProductionPieceId(com.mes.domain.shared.utils.IdGenerator.generateId("PP"));
        }

        return productionPieceRepository.add(productionPiece);
    }

    /**
     * 更新生产工件
     */
    public void updateProductionPiece(ProductionPiece productionPiece) {
        if (productionPiece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不能为空");
        }
        if (StringUtils.isBlank(productionPiece.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件 ID 不能为空");
        }

        productionPieceRepository.update(productionPiece);
    }

    /**
     * 删除生产工件
     */
    public void deleteProductionPiece(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }

        ProductionPiece productionPiece = productionPieceRepository.findById(id);
        if (productionPiece != null) {
            productionPieceRepository.delete(productionPiece);
        }
    }

    /**
     * 根据 ID 获取生产工件
     */
    public ProductionPiece findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        return productionPieceRepository.findById(id);
    }

    /**
     * 根据 productionPieceId 获取生产工件
     *
     * @param productionPieceId 生产工件唯一标识
     * @return 生产工件对象
     */
    public ProductionPiece findByProductionPieceId(String productionPieceId) {
        if (StringUtils.isBlank(productionPieceId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件唯一标识不能为空");
        }

        List<ProductionPiece> pieces = productionPieceRepository.filterList(1, 1, 
            java.util.Collections.singletonMap("productionPieceId", productionPieceId));
        
        if (pieces == null || pieces.isEmpty()) {
            return null;
        }
        
        return pieces.get(0);
    }

    /**
     * 根据 productionPieceId 更新生产工件
     *
     * @param productionPieceId 生产工件唯一标识
     * @param productionPiece 生产工件对象（包含要更新的字段）
     */
    public void updateProductionPieceByProductionPieceId(String productionPieceId, ProductionPiece productionPiece) {
        if (StringUtils.isBlank(productionPieceId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件唯一标识不能为空");
        }
        if (productionPiece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件对象不能为空");
        }

        ProductionPiece existingPiece = findByProductionPieceId(productionPieceId);
        if (existingPiece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在：" + productionPieceId);
        }

        // 复制要更新的字段
        if (productionPiece.getStatus() != null) {
            existingPiece.setStatus(productionPiece.getStatus());
        }
        if (productionPiece.getQuantity() != null) {
            existingPiece.setQuantity(productionPiece.getQuantity());
        }
        if (productionPiece.getTemplateCode() != null) {
            existingPiece.setTemplateCode(productionPiece.getTemplateCode());
        }
        if (productionPiece.getPositionType() != null) {
            existingPiece.setPositionType(productionPiece.getPositionType());
        }
        if (productionPiece.getPositionCode() != null) {
            existingPiece.setPositionCode(productionPiece.getPositionCode());
        }
        if (productionPiece.getProductImageFile() != null) {
            existingPiece.setProductImageFile(productionPiece.getProductImageFile());
        }
        if (productionPiece.getMaskImageFile() != null) {
            existingPiece.setMaskImageFile(productionPiece.getMaskImageFile());
        }
        if (productionPiece.getProcedureFlow() != null) {
            existingPiece.setProcedureFlow(productionPiece.getProcedureFlow());
        }

        productionPieceRepository.update(existingPiece);
    }

    /**
     * 批量添加生产工件
     */
    public List<ProductionPiece> batchAddProductionPieces(List<ProductionPiece> productionPieces) {
        if (productionPieces == null || productionPieces.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件列表不能为空");
        }

        for (ProductionPiece piece : productionPieces) {
            if (StringUtils.isBlank(piece.getOrderItemId())) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单项目 ID 不能为空");
            }
            if (StringUtils.isBlank(piece.getProductionPieceType())) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件类型不能为空");
            }
            // 生成唯一的 productionPieceId
            if (StringUtils.isBlank(piece.getProductionPieceId())) {
                piece.setProductionPieceId(com.mes.domain.shared.utils.IdGenerator.generateId("PP"));
            }
        }

        return (List<ProductionPiece>) productionPieceRepository.batchAdd(productionPieces);
    }

    /**
     * 更新生产工件状态
     */
    public void updateProductionPieceStatus(String id, ProductionPieceStatus status) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件 ID 不能为空");
        }
        if (status == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "状态不能为空");
        }

        ProductionPiece productionPiece = findById(id);
        if (productionPiece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在");
        }

        productionPiece.setStatus(status.getCode());
        productionPieceRepository.update(productionPiece);
    }

    /**
     * 根据 productionPieceId 更新生产工件状态
     *
     * @param productionPieceId 生产工件唯一标识
     * @param status 目标状态
     */
    public void updateProductionPieceStatusByproductionPieceId(String productionPieceId, ProductionPieceStatus status) {
        if (StringUtils.isBlank(productionPieceId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件唯一标识不能为空");
        }
        if (status == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "状态不能为空");
        }

        ProductionPiece productionPiece = findByProductionPieceId(productionPieceId);
        if (productionPiece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在：" + productionPieceId);
        }

        productionPiece.setStatus(status.getCode());
        productionPieceRepository.update(productionPiece);
    }

    /**
     * 状态机转换：开始排版
     *
     * @param id 生产工件 ID
     */
    public void startTypesetting(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (!currentStatus.canTypeset()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前状态不能开始排版，当前状态：" + currentStatus.getDescription());
        }

        piece.setStatus(ProductionPieceStatus.TYPESITTING.getCode());
        productionPieceRepository.update(piece);
    }

    /**
     * 状态机转换：排版完成，待确认
     *
     * @param id 生产工件 ID
     */
    public void completeTypesetting(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (currentStatus != ProductionPieceStatus.TYPESITTING) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只有排版中的工件才能完成排版，当前状态：" + currentStatus.getDescription());
        }

        piece.setStatus(ProductionPieceStatus.TYPESITTING_PENDING_CONFIRM.getCode());
        productionPieceRepository.update(piece);
    }

    /**
     * 状态机转换：确认排版
     *
     * @param id 生产工件 ID
     */
    public void confirmTypesetting(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (currentStatus != ProductionPieceStatus.TYPESITTING_PENDING_CONFIRM) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只有排版待确认的工件才能确认排版，当前状态：" + currentStatus.getDescription());
        }

        piece.setStatus(ProductionPieceStatus.PENDING_PRINT.getCode());
        productionPieceRepository.update(piece);
    }

    /**
     * 状态机转换：开始打印
     *
     * @param id 生产工件 ID
     */
    public void startPrinting(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (!currentStatus.canPrint()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前状态不能开始打印，当前状态：" + currentStatus.getDescription());
        }

        piece.setStatus(ProductionPieceStatus.PRINTING.getCode());
        productionPieceRepository.update(piece);
    }

    /**
     * 状态机转换：打印完成
     *
     * @param id 生产工件 ID
     */
    public void completePrinting(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (currentStatus != ProductionPieceStatus.PRINTING) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只有打印中的工件才能完成打印，当前状态：" + currentStatus.getDescription());
        }

        piece.setStatus(ProductionPieceStatus.PENDING_CUTTING.getCode());
        productionPieceRepository.update(piece);
    }

    /**
     * 状态机转换：开始切割
     *
     * @param id 生产工件 ID
     */
    public void startCutting(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (!currentStatus.canCut()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前状态不能开始切割，当前状态：" + currentStatus.getDescription());
        }

        piece.setStatus(ProductionPieceStatus.CUTTING.getCode());
        productionPieceRepository.update(piece);
    }

    /**
     * 状态机转换：切割完成
     *
     * @param id 生产工件 ID
     */
    public void completeCutting(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (currentStatus != ProductionPieceStatus.CUTTING) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只有切割中的工件才能完成切割，当前状态：" + currentStatus.getDescription());
        }

        boolean hasFuBanProcedure = checkHasFuBanProcedure(piece);
        if (hasFuBanProcedure) {
            piece.setStatus(ProductionPieceStatus.PENDING_FUBAN.getCode());
        } else {
            piece.setStatus(ProductionPieceStatus.PENDING_PACKING.getCode());
        }
        productionPieceRepository.update(piece);
    }

    /**
     * 状态机转换：开始覆板
     *
     * @param id 生产工件 ID
     */
    public void startFuBan(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (!currentStatus.canFuBan()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前状态不能开始覆板，当前状态：" + currentStatus.getDescription());
        }

        piece.setStatus(ProductionPieceStatus.FUBAN.getCode());
        productionPieceRepository.update(piece);
    }

    /**
     * 状态机转换：覆板完成
     *
     * @param id 生产工件 ID
     */
    public void completeFuBan(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (currentStatus != ProductionPieceStatus.FUBAN) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只有覆板中的工件才能完成覆板，当前状态：" + currentStatus.getDescription());
        }

        piece.setStatus(ProductionPieceStatus.PENDING_PACKING.getCode());
        productionPieceRepository.update(piece);
    }

    /**
     * 状态机转换：开始打包
     *
     * @param id 生产工件 ID
     */
    public void startPacking(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (!currentStatus.canPack()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前状态不能开始打包，当前状态：" + currentStatus.getDescription());
        }

        piece.setStatus(ProductionPieceStatus.PACKING_COMPLETED.getCode());
        productionPieceRepository.update(piece);
    }

    /**
     * 状态机转换：退单
     *
     * @param id 生产工件 ID
     */
    public void returnOrder(String id) {
        ProductionPiece piece = findById(id);
        validatePieceExists(piece, id);

        ProductionPieceStatus currentStatus = getCurrentStatus(piece);
        if (currentStatus.isFinalState()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "终态订单不能退单，当前状态：" + currentStatus.getDescription());
        }

        piece.setStatus(ProductionPieceStatus.RETURNED.getCode());
        productionPieceRepository.update(piece);
    }

    /**
     * 获取当前状态枚举
     *
     * @param piece 生产工件
     * @return 当前状态枚举
     */
    private ProductionPieceStatus getCurrentStatus(ProductionPiece piece) {
        if (piece.getStatus() == null) {
            return null;
        }

        // status 字段存储的是 String 类型的 code
        return ProductionPieceStatus.getByCode(piece.getStatus());
    }

    /**
     * 验证工件是否存在
     *
     * @param piece 生产工件
     * @param id 工件 ID
     */
    private void validatePieceExists(ProductionPiece piece, String id) {
        if (piece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在：" + id);
        }
    }

    /**
     * 检查是否有覆板工序
     * TODO: 需要根据实际业务逻辑实现
     *
     * @param piece 生产工件
     * @return true 如果有覆板工序，否则返回 false
     */
    private boolean checkHasFuBanProcedure(ProductionPiece piece) {
        if (piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
            return false;
        }

        return piece.getProcedureFlow().getNodes().stream()
                .anyMatch(node -> "覆板".equals(node.getNodeName()) || "双面对裱".equals(node.getNodeName()));
    }

    /**
     * 在工序节点之间划拨数量（从一个节点划转指定数量到另一个节点，保持总数量不变）
     *
     * @param productionPieceId 生产工件 ID
     * @param fromNodeId 源节点 ID
     * @param toNodeId 目标节点 ID
     * @param quantity 划转数量
     */
    public void transferPieceQuantityBetweenNodes(String productionPieceId, String fromNodeId, String toNodeId, Integer quantity) {
        if (StringUtils.isBlank(productionPieceId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件 ID 不能为空");
        }
        if (StringUtils.isBlank(fromNodeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "源节点 ID 不能为空");
        }
        if (StringUtils.isBlank(toNodeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "目标节点 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "划转数量必须大于 0");
        }

        // 查询生产工件
        ProductionPiece piece = findById(productionPieceId);
        if (piece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在：" + productionPieceId);
        }

        // 获取工艺路线和节点列表
        if (piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "该生产工件没有工艺路线或节点信息");
        }

        List<ProcedureFlowNode> nodes = piece.getProcedureFlow().getNodes();

        // 查找源节点和目标节点
        ProcedureFlowNode fromNode = null;
        ProcedureFlowNode toNode = null;

        for (ProcedureFlowNode node : nodes) {
            if (fromNodeId.equals(node.getNodeId())) {
                fromNode = node;
            }
            if (toNodeId.equals(node.getNodeId())) {
                toNode = node;
            }
        }

        if (fromNode == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "源节点不存在：" + fromNodeId);
        }
        if (toNode == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "目标节点不存在：" + toNodeId);
        }

        // 检查源节点数量是否足够
        if (fromNode.getPieceQuantity() == null || fromNode.getPieceQuantity() < quantity) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "源节点数量不足，当前数量：" +
                (fromNode.getPieceQuantity() != null ? fromNode.getPieceQuantity() : 0));
        }

        // 执行数量划转
        fromNode.setPieceQuantity(fromNode.getPieceQuantity() - quantity);
        toNode.setPieceQuantity((toNode.getPieceQuantity() != null ? toNode.getPieceQuantity() : 0) + quantity);

        // 更新生产工件的工艺路线信息
        updateProductionPiece(piece);
    }
}
