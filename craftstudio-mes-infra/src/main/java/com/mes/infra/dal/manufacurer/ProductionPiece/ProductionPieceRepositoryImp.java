package com.mes.infra.dal.manufacurer.ProductionPiece;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.repository.ProductionPieceRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.ProductionPiece.po.ProductionPiecePo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductionPieceRepositoryImp extends BaseRepositoryImp<ProductionPiece, ProductionPiecePo> implements ProductionPieceRepository {

    @Override
    public Class<ProductionPiecePo> poClass() {
        return ProductionPiecePo.class;
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
    public List<ProductionPiece> listPendingPackagingPiecesByConditions(String manufacturerId, String materialName, String processName, Double width) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("manufacturerId").is(manufacturerId));
        criteriaList.add(Criteria.where("procedureFlow.nodes").elemMatch(
                Criteria.where("nodeName").is("待打包").and("pieceQuantity").gt(0)
        ));

        if (materialName != null && !materialName.isBlank()) {
            criteriaList.add(Criteria.where("materialConfig.materialSnapshot.name").is(materialName));
        }
        if (processName != null && !processName.isBlank()) {
            criteriaList.add(Criteria.where("procedureFlow.nodes.nodeName").is(processName));
        }
        if (width != null) {
            criteriaList.add(Criteria.where("width").is(width));
        }

        Query query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        query.addCriteria(Criteria.where("deleteAt").is(null));
        return mongoTemplate.find(query, ProductionPiecePo.class)
                .stream().map(ProductionPiecePo::toDO).toList();
    }
}
