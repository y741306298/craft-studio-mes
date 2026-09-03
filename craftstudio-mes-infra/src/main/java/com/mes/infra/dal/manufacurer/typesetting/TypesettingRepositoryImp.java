package com.mes.infra.dal.manufacurer.typesetting;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingRepository;
import com.mes.domain.manufacturer.procedureFlow.vo.ProcessingFlowCondition;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.typesetting.po.TypesettingPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Repository
public class TypesettingRepositoryImp extends BaseRepositoryImp<TypesettingInfo, TypesettingPo> implements TypesettingRepository {

    @Override
    public List<TypesettingInfo> findPendingByConditions(String manufacturerMetaId, String materialName,
            List<ProcessingFlowCondition> processingNames,
            Date startTime, Date endTime, Boolean urgent, long offset, int size) {
        int pageSize = Math.max(1, size);
        Query query = new SoftDeleteQuery(pendingCriteria(manufacturerMetaId, materialName, processingNames,
                startTime, endTime, urgent));
        query.with(urgent == null
                        ? Sort.by(Sort.Order.desc("isUrgent"), Sort.Order.asc("createTime"))
                        : Sort.by(Sort.Order.asc("createTime")))
                .skip(Math.max(0, offset))
                .limit(pageSize);
        query.fields()
                .exclude("procedureFlow")
                .exclude("marks")
                .exclude("deviceCode")
                .exclude("deviceName")
                .exclude("requireJsonFile")
                .exclude("requirePltFile")
                .exclude("requireSvgFile")
                .exclude("codeGenerateType")
                .exclude("tempCodeFormat")
                .exclude("anchorPointShape")
                .exclude("templateCode")
                .exclude("layoutCategory");
        return mongoTemplate.find(query, poClass()).stream().map(TypesettingPo::toDO).toList();
    }

    @Override
    public long countPendingByConditions(String manufacturerMetaId, String materialName,
            List<ProcessingFlowCondition> processingNames,
            Date startTime, Date endTime, Boolean urgent) {
        return mongoTemplate.count(new SoftDeleteQuery(pendingCriteria(manufacturerMetaId, materialName,
                processingNames, startTime, endTime, urgent)), poClass());
    }

    private Criteria pendingCriteria(String manufacturerMetaId, String materialName,
            List<ProcessingFlowCondition> processingNames,
            Date startTime, Date endTime, Boolean urgent) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("manufacturerMetaId").is(manufacturerMetaId));
        criteria.add(Criteria.where("status").is(com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus.PENDING.getCode()));
        if (urgent != null) {
            criteria.add(urgent ? Criteria.where("isUrgent").is(true) : Criteria.where("isUrgent").ne(true));
        }
        criteria.add(Criteria.where("leaveQuantity").gt(0));
        if (StringUtils.isNotBlank(materialName)) {
            criteria.add(Criteria.where("materialConfig.materialSnapshot.name").is(materialName.trim()));
        }
        if (startTime != null) criteria.add(Criteria.where("createTime").gte(startTime));
        if (endTime != null) criteria.add(Criteria.where("createTime").lte(endTime));
        if (processingNames != null) {
            processingNames.stream().filter(Objects::nonNull)
                    .filter(c -> StringUtils.isNotBlank(c.getProcessName()))
                    .forEach(c -> {
                        Criteria node = Criteria.where("nodeName").is(c.getProcessName().trim());
                        if (StringUtils.isNotBlank(c.getAccessoryName())) {
                            node.and("paramConfigs.param.accessorySnapshot.name").is(c.getAccessoryName().trim());
                        }
                        criteria.add(Criteria.where("procedureFlow.nodes").elemMatch(node));
                    });
        }
        return new Criteria().andOperator(criteria.toArray(new Criteria[0]));
    }

    @Override
    public void batchUpdateCallbackFailure(Collection<String> ids, String status, String remark) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Object> mongoIds = new ArrayList<>(ids.size() * 2);
        for (String id : ids) {
            if (StringUtils.isBlank(id)) {
                continue;
            }
            mongoIds.add(id);
            if (ObjectId.isValid(id)) {
                mongoIds.add(new ObjectId(id));
            }
        }
        if (mongoIds.isEmpty()) {
            return;
        }
        Query query = new SoftDeleteQuery(Criteria.where("_id").in(mongoIds));
        Update update = new Update()
                .set("status", status)
                .set("remark", remark)
                .set("updateTime", new Date());
        mongoTemplate.updateMulti(query, update, poClass());
    }

    @Override
    public boolean compareAndSetPrintReport(String id, Integer expectedLeaveQuantity, int leaveQuantity,
                                            String status, String remark) {
        List<Object> mongoIds = new ArrayList<>();
        mongoIds.add(id);
        if (ObjectId.isValid(id)) {
            mongoIds.add(new ObjectId(id));
        }
        Criteria criteria = Criteria.where("_id").in(mongoIds)
                .and("leaveQuantity").is(expectedLeaveQuantity);
        Update update = new Update()
                .set("leaveQuantity", leaveQuantity)
                .set("status", status)
                .set("updateTime", new Date());
        if (StringUtils.isNotBlank(remark)) {
            update.set("remark", remark);
        }
        return mongoTemplate.updateFirst(new SoftDeleteQuery(criteria), update, poClass()).getModifiedCount() == 1;
    }

    @Override
    public Class<TypesettingPo> poClass() {
        return TypesettingPo.class;
    }

    @Override
    public TypesettingInfo findById(String id) {
        TypesettingPo po = findPoById(id);
        return po == null ? null : po.toDO();
    }

    private TypesettingPo findPoById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        TypesettingPo po = mongoTemplate.findOne(
                new SoftDeleteQuery(Criteria.where("_id").is(id)), poClass()
        );
        if (po != null || !ObjectId.isValid(id)) {
            return po;
        }
        return mongoTemplate.findOne(
                new SoftDeleteQuery(Criteria.where("_id").is(new ObjectId(id))), poClass()
        );
    }
}
