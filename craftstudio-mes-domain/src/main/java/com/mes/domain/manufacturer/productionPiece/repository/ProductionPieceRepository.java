package com.mes.domain.manufacturer.productionPiece.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.manufacturer.procedureFlow.vo.ProcessingFlowCondition;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ProductionPieceRepository extends BaseRepository<ProductionPiece> {

    List<ProductionPiece> findByProductionPieceIds(Collection<String> productionPieceIds);

    List<ProductionPiece> findByOrderItemIds(Collection<String> orderItemIds);

    List<ProductionPiece> findPendingPackagingPiecesByOrderItemIds(String manufacturerId,
            Collection<String> orderItemIds, String materialName);

    List<ProductionPiece> findPendingTypesettingPiecesByOrderItemIds(String manufacturerId,
            Collection<String> orderItemIds, String materialName, List<ProcessingFlowCondition> processNames,
            String routeId, java.util.Date startTime, java.util.Date endTime);

    /** Clears route bindings for every production piece belonging to the supplied order items. */
    long clearRouteBindingsByOrderItemIds(Collection<String> orderItemIds);

    /** Atomically transfers quantities between procedure nodes using conditional MongoDB updates. */
    long transferTypesettingQuantitiesToPrinting(Map<String, Integer> requiredQuantities);

    /** Atomically reserves pending quantities for layout using conditional MongoDB updates. */
    long reservePendingTypesettingQuantities(Map<String, Integer> requiredQuantities);
    
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

    /** Updates only channel metadata, preserving procedure-flow quantities changed concurrently. */
    long updateChannelByIds(Collection<String> ids, OrderChannelInfo channel);

    /**
     * 根据订单项目 ID 删除全部生产工件。
     *
     * @param orderItemId 订单项目 ID
     * @return 删除的生产工件数量
     */
    long deleteByOrderItemId(String orderItemId);

    /** 根据多个订单项目 ID 批量软删除生产工件。 */
    long deleteByOrderItemIds(Collection<String> orderItemIds);

    java.util.List<ProductionPiece> listPendingPackagingPiecesByConditions(String manufacturerId, String materialName, java.util.List<ProcessingFlowCondition> processNames, Double width, String routeId);

    java.util.List<ProductionPiece> listPendingTypesettingPiecesByConditions(String manufacturerId, String materialName,
            java.util.List<ProcessingFlowCondition> processNames, String orderItemId, String routeId,
            java.util.Date startTime, java.util.Date endTime, boolean urgent, long offset, int size);

    long countPendingTypesettingPiecesByConditions(String manufacturerId, String materialName,
            java.util.List<ProcessingFlowCondition> processNames, String orderItemId, String routeId,
            java.util.Date startTime, java.util.Date endTime, boolean urgent);

    long normalizeInProgressStatuses(String manufacturerId, String packedStatus);
}
