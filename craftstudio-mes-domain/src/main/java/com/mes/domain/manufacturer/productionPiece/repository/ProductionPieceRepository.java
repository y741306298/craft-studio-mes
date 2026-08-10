package com.mes.domain.manufacturer.productionPiece.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.manufacturer.procedureFlow.vo.ProcessingFlowCondition;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;

import java.util.Collection;
import java.util.List;

public interface ProductionPieceRepository extends BaseRepository<ProductionPiece> {

    List<ProductionPiece> findByProductionPieceIds(Collection<String> productionPieceIds);
    
    /**
     * 根据 productionPieceId 更新生产工件
     * @param productionPiece 生产工件对象（包含 productionPieceId）
     */
    void updateByProductionPieceId(ProductionPiece productionPiece);

    /**
     * 根据订单项目 ID 批量更新生产工件加急状态。
     *
     * @param orderItemId 订单项目 ID
     * @param isUrgent 加急状态
     */
    void updateUrgentByOrderItemId(String orderItemId, Boolean isUrgent);

    /**
     * 根据订单项目 ID 删除全部生产工件。
     *
     * @param orderItemId 订单项目 ID
     * @return 删除的生产工件数量
     */
    long deleteByOrderItemId(String orderItemId);

    java.util.List<ProductionPiece> listPendingPackagingPiecesByConditions(String manufacturerId, String materialName, java.util.List<ProcessingFlowCondition> processNames, Double width, String routeId);

    java.util.List<ProductionPiece> listPendingTypesettingPiecesByConditions(String manufacturerId, String materialName,
            java.util.List<ProcessingFlowCondition> processNames, String orderItemId, String routeId,
            java.util.Date startTime, java.util.Date endTime);

    long normalizeInProgressStatuses(String manufacturerId, String packedStatus);
}
