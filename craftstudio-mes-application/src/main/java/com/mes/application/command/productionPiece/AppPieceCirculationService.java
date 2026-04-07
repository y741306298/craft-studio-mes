package com.mes.application.command.productionPiece;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.shared.exception.BusinessNotAllowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppPieceCirculationService {

    @Autowired
    private ProductionPieceService productionPieceService;

    /**
     * 生产件进入下一工序节点
     * 将前一个节点状态置为 COMPLETED，当前节点状态置为 PENDING，
     * 并将前一个节点的 pieceQuantity 全部转移到当前节点
     * 
     * @param productionPiece 生产件实体
     * @param nextNodeIndex 当前工序节点索引（从 0 开始）
     * @return 更新后的生产件
     */
    public ProductionPiece moveToNextNode(ProductionPiece productionPiece, int nextNodeIndex) {
        if (productionPiece == null) {
            throw new BusinessNotAllowException("生产件不能为空");
        }

        ProcedureFlow procedureFlow = productionPiece.getProcedureFlow();
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            throw new BusinessNotAllowException("工艺流程或节点列表为空");
        }

        List<ProcedureFlowNode> nodes = procedureFlow.getNodes();
        if (nextNodeIndex < 0 || nextNodeIndex >= nodes.size()) {
            throw new BusinessNotAllowException("节点索引超出范围");
        }


        // 获取前一个节点和当前节点
        ProcedureFlowNode previousNode = nodes.get(nextNodeIndex - 1);
        ProcedureFlowNode currentNode = nodes.get(nextNodeIndex);


        // 将前一个节点状态置为 COMPLETED
        previousNode.setNodeStatus(NodeStatus.COMPLETED);
        Integer previousQuantity = previousNode.getPieceQuantity() != null ? previousNode.getPieceQuantity() : 0;

        currentNode.setNodeStatus(NodeStatus.PENDING);
        
        // 将前一个节点的 pieceQuantity 全部转移到当前节点
        currentNode.setPieceQuantity(previousQuantity);
        previousNode.setPieceQuantity(0);

        // 更新生产件
        productionPieceService.updateProductionPieceByProductionPieceId(productionPiece.getProductionPieceId(),productionPiece);

        return productionPiece;
    }

    /**
     * 生产件进入下一工序节点（按指定数量转移）
     * 将前一个节点状态保持为 ACTIVE，当前节点状态置为 PENDING，
     * 并将指定数量的 pieceQuantity 转移到当前节点
     * 
     * @param productionPiece 生产件实体
     * @param nextNodeIndex 当前工序节点索引（从 0 开始）
     * @param completeQuantity 要转移的数量
     * @return 更新后的生产件
     */
    public ProductionPiece moveToNextNodeWithQuantity(ProductionPiece productionPiece, int nextNodeIndex, Integer completeQuantity) {
        if (productionPiece == null) {
            throw new BusinessNotAllowException("生产件不能为空");
        }
        if (completeQuantity == null || completeQuantity <= 0) {
            throw new BusinessNotAllowException("转移数量必须大于 0");
        }

        ProcedureFlow procedureFlow = productionPiece.getProcedureFlow();
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            throw new BusinessNotAllowException("工艺流程或节点列表为空");
        }

        List<ProcedureFlowNode> nodes = procedureFlow.getNodes();
        if (nextNodeIndex < 0 || nextNodeIndex >= nodes.size()) {
            throw new BusinessNotAllowException("节点索引超出范围");
        }

        // 获取前一个节点和当前节点
        ProcedureFlowNode previousNode = nodes.get(nextNodeIndex - 1);
        ProcedureFlowNode currentNode = nodes.get(nextNodeIndex);


        // 检查前一个节点数量是否足够
        Integer previousQuantity = previousNode.getPieceQuantity() != null ? previousNode.getPieceQuantity() : 0;
        if (previousQuantity < completeQuantity) {
            throw new BusinessNotAllowException("前一个节点数量不足，当前数量：" + previousQuantity);
        }

        // 将前一个节点减少指定数量,如果全部指定则置为 COMPLETED
        previousNode.setPieceQuantity(previousQuantity - completeQuantity);
        if (previousNode.getPieceQuantity() <= 0) {
            previousNode.setNodeStatus(NodeStatus.COMPLETED);
        }
        
        // 将当前节点增加指定数量
        Integer currentQuantity = currentNode.getPieceQuantity() != null ? currentNode.getPieceQuantity() : 0;
        currentNode.setPieceQuantity(currentQuantity + completeQuantity);
        currentNode.setNodeStatus(NodeStatus.PENDING);

        // 更新生产件
        productionPieceService.updateProductionPiece(productionPiece);

        return productionPiece;
    }

    /**
     * 更新指定工序节点的状态
     * 
     * @param productionPiece 生产件实体
     * @param nodeIndex 工序节点索引（从 0 开始）
     * @param newStatus 新的节点状态
     * @return 更新后的生产件
     */
    public ProductionPiece updateNodeStatus(ProductionPiece productionPiece, int nodeIndex, NodeStatus newStatus) {
        if (productionPiece == null) {
            throw new BusinessNotAllowException("生产件不能为空");
        }
        if (newStatus == null) {
            throw new BusinessNotAllowException("新状态不能为空");
        }

        ProcedureFlow procedureFlow = productionPiece.getProcedureFlow();
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            throw new BusinessNotAllowException("工艺流程或节点列表为空");
        }

        List<ProcedureFlowNode> nodes = procedureFlow.getNodes();
        if (nodeIndex < 0 || nodeIndex >= nodes.size()) {
            throw new BusinessNotAllowException("节点索引超出范围");
        }

        // 获取指定节点并更新状态
        ProcedureFlowNode targetNode = nodes.get(nodeIndex);
        targetNode.setNodeStatus(newStatus);

        // 更新生产件
        productionPieceService.updateProductionPiece(productionPiece);

        return productionPiece;
    }


}
