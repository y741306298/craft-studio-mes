package com.mes.domain.manufacturer.procedure.service;

import com.mes.domain.manufacturer.procedure.entity.Procedure;
import com.mes.domain.manufacturer.procedure.repository.ProcedureRepository;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.shared.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProcedureService {

    @Autowired
    private ProcedureRepository procedureRepository;

    /**
     * 根据工序名称查询工序（支持分页）
     * @param procedureName 工序名称
     * @param current 当前页码
     * @param size 每页大小
     * @return 工序列表
     */
    public List<Procedure> findProceduresByName(String procedureName, int current, int size) {
        // 参数验证

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在1-100之间");
        }
        if (StringUtils.isBlank(procedureName)) {
            throw new BusinessNotAllowException("工序名称不能为空");
        }

        // 根据procedureName进行模糊查询
        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("procedureName", procedureName);
        return procedureRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 获取工序总数
     * @param procedureName 工序名称，可为空
     * @return 总数
     */
    public long getTotalCount(String procedureName) {
        if (StringUtils.isNotBlank(procedureName)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("procedureName", procedureName);
            return procedureRepository.totalByFuzzySearch(searchFilters);
        } else {
            return procedureRepository.total();
        }
    }

    /**
     * 添加工序
     * @param procedure 工序实体
     * @return 添加后的实体
     */
    public Procedure addProcedure(Procedure procedure) {
        // 业务验证
        if (procedure == null) {
            throw new BusinessNotAllowException("工序不能为空");
        }
        if (StringUtils.isBlank(procedure.getProcedureName())) {
            throw new BusinessNotAllowException("工序名称不能为空");
        }

        return procedureRepository.add(procedure);
    }

    /**
     * 更新工序
     * @param procedure 工序实体
     */
    public void updateProcedure(Procedure procedure) {
        // 业务验证
        if (procedure == null) {
            throw new BusinessNotAllowException("工序不能为空");
        }
        if (StringUtils.isBlank(procedure.getId())) {
            throw new BusinessNotAllowException("工序ID不能为空");
        }
        if (StringUtils.isBlank(procedure.getProcedureName())) {
            throw new BusinessNotAllowException("工序名称不能为空");
        }

        procedureRepository.update(procedure);
    }

    /**
     * 删除工序
     * @param id 工序ID
     */
    public void deleteProcedure(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("ID不能为空");
        }

        Procedure procedure = procedureRepository.findById(id);
        if (procedure != null) {
            procedureRepository.delete(procedure);
        }
    }

    /**
     * 根据ID获取工序
     * @param id 工序ID
     * @return 工序实体
     */
    public Procedure findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("ID不能为空");
        }
        return procedureRepository.findById(id);
    }

    /**
     * 原有的 add 方法保持兼容性
     */
    public Procedure add(Procedure procedure) {
        return addProcedure(procedure);
    }

    /**
     * 处理裁切工艺：生成切割坐标
     * @param processingNodes 工艺流程节点列表
     * @return 切割坐标列表 [x1, y1, x2, y2, ...]
     */
    public List<Double> generateCuttingCoordinates(List<ProcedureFlowNode> processingNodes) {
        // TODO: 根据实际的裁切工艺逻辑生成切割坐标
        // 这里暂时返回示例坐标
        return Arrays.asList(
                0.0, 0.0,      // 起点 (x1, y1)
                1000.0, 1000.0 // 终点 (x2, y2)
        );
    }

    /**
     * 创建生产工件实体
     * @param orderItemId 订单项 ID
     * @param procedureFlowId 工艺流程 ID
     * @param pieceType 工件类型
     * @param quantity 数量
     * @param imageUrl 图片 URL
     * @return 生产工件实体
     */
    public ProductionPiece createProductionPiece(String orderItemId, String procedureFlowId, 
                                                  String pieceType, Integer quantity, String imageUrl) {
        ProductionPiece piece = new ProductionPiece();
        piece.setOrderItemId(orderItemId);
        piece.setProcedureFlowId(procedureFlowId);
        piece.setProductionPieceType(pieceType);
        piece.setStatus("PENDING");
        piece.setQuantity(quantity);
        piece.setTemplateCode(imageUrl);
        return piece;
    }

}
