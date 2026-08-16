package com.mes.infra.dal.manufacurer.ProductionPiece;

import com.mes.domain.manufacturer.procedureFlow.vo.ProcessingFlowCondition;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.repository.ProductionPieceRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mongodb.client.result.UpdateResult;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import com.mes.infra.dal.manufacurer.ProductionPiece.po.ProductionPiecePo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Repository
@Slf4j
public class ProductionPieceRepositoryImp extends BaseRepositoryImp<ProductionPiece, ProductionPiecePo> implements ProductionPieceRepository {

    @Override
    public Class<ProductionPiecePo> poClass() {
        return ProductionPiecePo.class;
    }

    @Override
    public List<ProductionPiece> findByProductionPieceIds(Collection<String> productionPieceIds) {
        if (productionPieceIds == null || productionPieceIds.isEmpty()) {
            return Collections.emptyList();
        }
        Query query = new SoftDeleteQuery(new Criteria().orOperator(
                Criteria.where("productionPieceId").in(productionPieceIds),
                Criteria.where("_id").in(productionPieceIds)));
        return mongoTemplate.find(query, poClass()).stream().map(ProductionPiecePo::toDO).toList();
    }
    
    @Override
    public void updateByProductionPieceId(ProductionPiece productionPiece) {
        if (productionPiece == null || productionPiece.getProductionPieceId() == null) {
            throw new IllegalArgumentException("productionPiece 和 productionPieceId 不能为空");
        }
        
        // 先根据 productionPieceId 查询现有记录
        java.util.Map<String, Object> filters = new java.util.HashMap<>();
        filters.put("productionPieceId", productionPiece.getProductionPieceId());
        java.util.List<ProductionPiece> existingList = filterList(1, 1, filters);
        
        if (existingList.isEmpty()) {
            throw new IllegalArgumentException("生产工件不存在：" + productionPiece.getProductionPieceId());
        }
        
        // 获取现有记录的 id（MongoDB 的_id）
        ProductionPiece existing = existingList.get(0);
        
        // 设置 id 后调用父类的 update 方法
        productionPiece.setId(existing.getId());
        update(productionPiece);
    }

    @Override
    public void updateUrgentByOrderItemId(String orderItemId, Boolean isUrgent) {
        if (orderItemId == null || orderItemId.isBlank()) {
            throw new IllegalArgumentException("订单项目 ID 不能为空");
        }
        if (isUrgent == null) {
            throw new IllegalArgumentException("加急状态不能为空");
        }

        Query query = new SoftDeleteQuery(Criteria.where("orderItemId").is(orderItemId));
        Update update = new Update()
                .set("isUrgent", isUrgent)
                .set("updateTime", new Date());
        mongoTemplate.updateMulti(query, update, poClass());
    }

    @Override
    public long deleteByOrderItemId(String orderItemId) {
        if (orderItemId == null || orderItemId.isBlank()) {
            throw new IllegalArgumentException("订单项目 ID 不能为空");
        }

        Query query = new SoftDeleteQuery(Criteria.where("orderItemId").is(orderItemId));
        Update update = new Update()
                .set(SoftDeleteQuery.DELETED_AT, new Date())
                .set("updateTime", new Date());
        UpdateResult result = mongoTemplate.updateMulti(query, update, poClass());
        return result.getModifiedCount();
    }

    @Override
    public List<ProductionPiece> listPendingPackagingPiecesByConditions(String manufacturerId, String materialName, List<ProcessingFlowCondition> processNames, Double width, String routeId) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("manufacturerId").is(manufacturerId));
        criteriaList.add(Criteria.where("status").is(com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus.PROCESSING.getCode()));
        criteriaList.add(Criteria.where("procedureFlow.nodes").elemMatch(
                Criteria.where("nodeName").is("待打包").and("pieceQuantity").gt(0)
        ));

        if (materialName != null && !materialName.isBlank()) {
            criteriaList.add(Criteria.where("materialConfig.materialSnapshot.name").is(materialName));
        }
        List<Criteria> processCriteria = processNames == null ? List.of() : processNames.stream()
                .filter(Objects::nonNull)
                .filter(condition -> condition.getProcessName() != null && !condition.getProcessName().isBlank())
                .map(condition -> {
                    Criteria nodeCriteria = Criteria.where("nodeName").is(condition.getProcessName().trim());
                    if (condition.getAccessoryName() != null && !condition.getAccessoryName().isBlank()) {
                        nodeCriteria.and("paramConfigs.param.accessorySnapshot.name").is(condition.getAccessoryName().trim());
                    }
                    return Criteria.where("procedureFlow.nodes").elemMatch(nodeCriteria);
                })
                .toList();
        criteriaList.addAll(processCriteria);
        if (width != null) {
            criteriaList.add(Criteria.where("width").is(width));
        }
        if (routeId != null && !routeId.isBlank()) {
            criteriaList.add(Criteria.where("routeId").is(routeId));
        }

        Query query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        query.addCriteria(Criteria.where("deleteAt").is(null));
        long start = System.nanoTime();
        List<ProductionPiecePo> pos = mongoTemplate.find(query, ProductionPiecePo.class);
        log.info("MongoDB query listPendingPackagingPiecesByConditions completed: manufacturerId={}, results={}, elapsedMs={}",
                manufacturerId, pos.size(), (System.nanoTime() - start) / 1_000_000.0);
        return pos.stream().map(ProductionPiecePo::toDO).toList();
    }

    @Override
    public List<ProductionPiece> listPendingTypesettingPiecesByConditions(String manufacturerId, String materialName,
            List<ProcessingFlowCondition> processNames, String orderItemId, String routeId, Date startTime, Date endTime) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("manufacturerId").is(manufacturerId));
        criteriaList.add(Criteria.where("status").is(com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus.PROCESSING.getCode()));
        criteriaList.add(Criteria.where("procedureFlow.nodes").elemMatch(
                Criteria.where("nodeName").is("待排版").and("pieceQuantity").gt(0)));
        if (materialName != null && !materialName.isBlank()) {
            criteriaList.add(Criteria.where("materialConfig.materialSnapshot.name")
                    .regex(Pattern.quote(materialName), "i"));
        }
        if (processNames != null) {
            processNames.stream().filter(Objects::nonNull)
                    .filter(condition -> condition.getProcessName() != null && !condition.getProcessName().isBlank())
                    .forEach(condition -> {
                        Criteria node = Criteria.where("nodeName").is(condition.getProcessName().trim());
                        if (condition.getAccessoryName() != null && !condition.getAccessoryName().isBlank()) {
                            node.and("paramConfigs.param.accessorySnapshot.name").is(condition.getAccessoryName().trim());
                        }
                        criteriaList.add(Criteria.where("procedureFlow.nodes").elemMatch(node));
                    });
        }
        if (orderItemId != null && !orderItemId.isBlank()) {
            criteriaList.add(Criteria.where("orderItemId").is(orderItemId));
        }
        if (routeId != null && !routeId.isBlank()) {
            criteriaList.add(Criteria.where("routeId").is(routeId));
        }
        if (startTime != null) {
            criteriaList.add(Criteria.where("createTime").gte(startTime));
        }
        if (endTime != null) {
            criteriaList.add(Criteria.where("createTime").lte(endTime));
        }
        Query query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        query.addCriteria(Criteria.where("deleteAt").is(null));
        return mongoTemplate.find(query, ProductionPiecePo.class).stream().map(ProductionPiecePo::toDO).toList();
    }

    @Override
    public long normalizeInProgressStatuses(String manufacturerId, String packedStatus) {
        Query query = new SoftDeleteQuery(Criteria.where("manufacturerId").is(manufacturerId)
                .and("status").nin(
                        com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus.PROCESSING.getCode(),
                        com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus.RETURNED.getCode(),
                        packedStatus));
        Update update = new Update()
                .set("status", com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus.PROCESSING.getCode())
                .set("updateTime", new Date());
        return mongoTemplate.updateMulti(query, update, ProductionPiecePo.class).getModifiedCount();
    }
}
