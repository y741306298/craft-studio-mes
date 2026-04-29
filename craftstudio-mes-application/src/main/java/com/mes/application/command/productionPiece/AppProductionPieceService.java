package com.mes.application.command.productionPiece;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.transBox.storageTank.service.StorageOperationRecordService;
import com.mes.domain.manufacturer.transBox.storageTank.service.StorageTankService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AppProductionPieceService {

    @Autowired
    private ProductionPieceService domainProductionPieceService;

    /**
     * 分页查询生产工件列表
     * @param status 状态（可选）
     * @param material 材质（可选）
     * @param nodeName 工序节点名称（可选）
     * @param startDate 开始时间（可选）
     * @param endDate 结束时间（可选）
     * @param query 分页查询参数
     * @return 分页查询结果
     */
    public PagedResult<ProductionPiece> findProductionPieces(
            String manufacturerId,
            String status, 
            String material,
            String nodeName,
            Date startDate,
            Date endDate,
            PagedQuery query) {
        
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        // 查询数据
        java.util.List<ProductionPiece> items = domainProductionPieceService.findProductionPiecesByConditions(manufacturerId,
                status, material, nodeName, startDate, endDate, (int) query.getCurrent(), query.getSize());
        
        // 查询总数
        long total = domainProductionPieceService.countProductionPiecesByConditions(status, material, nodeName, startDate, endDate);
        
        return new PagedResult<>(items, total, query.getSize(), query.getCurrent());
    }

    /**
     * 根据 productionPieceId 查询详情
     * @param productionPieceId 生产工件 ID
     * @return 生产工件详情
     */
    public ProductionPiece findByProductionPieceId(String productionPieceId,String manufacturerId) {
        if (StringUtils.isBlank(productionPieceId)) {
            throw new IllegalArgumentException("生产工件 ID 不能为空");
        }
        
        // 先尝试通过 ID 查找
        ProductionPiece piece = domainProductionPieceService.findById(productionPieceId);
        
        if (piece != null) {
            return piece;
        }
        
        // 如果找不到，尝试通过 productionPieceId 字段查找
        java.util.List<ProductionPiece> pieces = domainProductionPieceService.findProductionPiecesByConditions(manufacturerId,
                null, null, null,null,null,1, 1000);
        
        if (pieces != null && !pieces.isEmpty()) {
            return pieces.stream()
                    .filter(p -> productionPieceId.equals(p.getProductionPieceId()))
                    .findFirst()
                    .orElse(null);
        }
        
        return null;
    }

    /**
     * 根据订单项目 ID 分页查询生产工件
     * @param orderItemId 订单项目 ID
     * @param query 分页查询参数
     * @return 分页查询结果
     */
    public PagedResult<ProductionPiece> findProductionPiecesByOrderItemId(String orderItemId, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (StringUtils.isBlank(orderItemId)) {
            throw new IllegalArgumentException("订单项目 ID 不能为空");
        }
        
        java.util.List<ProductionPiece> items = domainProductionPieceService.findProductionPiecesByOrderItemId(
                orderItemId, (int) query.getCurrent(), query.getSize());
        
        long total = domainProductionPieceService.getTotalCount(orderItemId);
        
        return new PagedResult<>(items, total, query.getSize(), query.getCurrent());
    }

    public ProductionPiece increasePendingTypesettingQuantity(String productionPieceId, Integer increaseQuantity) {
        if (StringUtils.isBlank(productionPieceId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "productionPieceId 不能为空");
        }
        if (increaseQuantity == null || increaseQuantity <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "增加数量必须大于 0");
        }

        ProductionPiece piece = domainProductionPieceService.findByProductionPieceId(productionPieceId);
        if (piece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在：" + productionPieceId);
        }
        if (piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件未配置工序节点");
        }

        List<ProcedureFlowNode> nodes = piece.getProcedureFlow().getNodes();
        ProcedureFlowNode pendingTypesettingNode = nodes.stream()
                .filter(node -> node != null && "待排版".equals(node.getNodeName()))
                .findFirst()
                .orElseThrow(() -> new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "未找到“待排版”节点"));

        int currentPendingQuantity = pendingTypesettingNode.getPieceQuantity() == null ? 0 : pendingTypesettingNode.getPieceQuantity();
        pendingTypesettingNode.setPieceQuantity(currentPendingQuantity + increaseQuantity);

        int currentPieceQuantity = piece.getQuantity() == null ? 0 : piece.getQuantity();
        piece.setQuantity(currentPieceQuantity + increaseQuantity);

        domainProductionPieceService.updateProductionPieceByProductionPieceId(productionPieceId, piece);
        return piece;
    }
}
