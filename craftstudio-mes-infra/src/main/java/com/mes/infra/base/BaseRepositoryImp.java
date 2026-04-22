package com.mes.infra.base;


import com.mes.domain.base.BaseEntity;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.base.repository.BaseRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.*;

public abstract class BaseRepositoryImp<DO extends BaseEntity, PO extends BasePO<DO>> implements BaseRepository<DO> {
    @Autowired
    protected MongoTemplate mongoTemplate;
    public abstract Class<PO> poClass();

    @Override
    public DO add(DO _do) {
        Date now = convertToBeijingTime(new Date());
        if (_do.getCreateTime() == null) {
            _do.setCreateTime(now);
        } else {
            _do.setCreateTime(convertToBeijingTime(_do.getCreateTime()));
        }
        if (_do.getUpdateTime() == null) {
            _do.setUpdateTime(now);
        } else {
            _do.setUpdateTime(convertToBeijingTime(_do.getUpdateTime()));
        }
        PO po = mongoTemplate.insert(BasePO.fromDO(_do, poClass()));
        return po.toDO();
    }

    @Override
    public Collection<DO> batchAdd(List<DO> items) {
        Date now = convertToBeijingTime(new Date());
        Collection<PO> pos = mongoTemplate.insertAll(items.stream().map(item -> {
            if (item.getCreateTime() == null) {
                item.setCreateTime(now);
            } else {
                item.setCreateTime(convertToBeijingTime(item.getCreateTime()));
            }
            if (item.getUpdateTime() == null) {
                item.setUpdateTime(now);
            } else {
                item.setUpdateTime(convertToBeijingTime(item.getUpdateTime()));
            }
            return BasePO.fromDO(item, poClass());
        }).toList());
        return pos.stream().map(PO::toDO).toList();
    }

    @Override
    public void delete(DO _do) {
        PO po = BasePO.fromDO(_do, poClass());
        Update update = new Update();
        update.set(SoftDeleteQuery.DELETED_AT, new Date());
        mongoTemplate.updateFirst(
            new Query(Criteria.where("_id").is(po.getId())),
            update,
            poClass()
        );
        //mongoTemplate.remove(fromDO(_do));
    }

    @Override
    public void update(DO _do) {
        _do.setUpdateTime(convertToBeijingTime(new Date()));
        if (_do.getCreateTime() != null) {
            _do.setCreateTime(convertToBeijingTime(_do.getCreateTime()));
        }
        mongoTemplate.save(BasePO.fromDO(_do, poClass()));
    }

    @Override
    public void batchUpdate(List<DO> items) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(
                BulkOperations.BulkMode.UNORDERED,
                poClass()
        );
        for (DO item : items) {
            Query query = Query.query(Criteria.where("_id").is(item.getId()));
            bulkOps.replaceOne(
                query, BasePO.fromDO(item, poClass()), FindAndReplaceOptions.options().upsert()
            );
        }
        bulkOps.execute();

    }

    @Override
    public DO findById(String id) {
        PO po = mongoTemplate.findOne(
            new SoftDeleteQuery(Criteria.where("_id").is(id)), poClass()
        );
        if(po!=null){
            return po.toDO();
        }
        return null;
    }

    @Override
    public Map<String, DO> findByIds(Collection<String> ids) {
        var pos = mongoTemplate.find(
            new SoftDeleteQuery(Criteria.where("_id").in(ids)),
            poClass()
        );
        Map<String, DO> map = new HashMap<>();
        pos.forEach(po->{
            map.put(po.getId(), po.toDO());
        });
        return map;
    }

    @Override
    public List<DO> list(long current, int size) {
        var pos = mongoTemplate.find(
            new SoftDeleteQuery()
                .with(Sort.by(Sort.Direction.DESC, "updateTime"))
                .limit(size).skip((current-1)*size),
            poClass()
        );
        return pos.stream().map(BasePO::toDO).toList();
    }

    @Override
    public List<DO> fuzzySearch(Map<String, String> searchFilters, long current, int size) {
        Criteria criteria = null;
        for(String key:searchFilters.keySet()){
            String value = searchFilters.get(key);
            if(criteria == null){
                criteria = Criteria.where(key).regex(value,"i");
            }else {
                criteria.and(key).regex(value,"i");
            }
        }
        if(criteria == null){
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "查询条件不能为空！");
        }

        var pos = mongoTemplate.find(
            new SoftDeleteQuery(criteria)
                    .with(Sort.by(Sort.Direction.DESC, "updateTime"))
                    .limit(size).skip((current-1)*size),
            poClass()
        );
        return pos.stream().map(BasePO::toDO).toList();
    }

    @Override
    public long totalByFuzzySearch(Map<String, String> searchFilters) {
        Criteria criteria = null;
        for(String key:searchFilters.keySet()){
            String value = searchFilters.get(key);
            if(criteria == null){
                criteria = Criteria.where(key).regex(value,"i");
            }else {
                criteria.and(key).regex(value,"i");
            }
        }
        if(criteria == null){
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "查询条件不能为空！");
        }
        return mongoTemplate.count(
            new SoftDeleteQuery(criteria).with(Sort.by(Sort.Direction.DESC, "updateTime")),
            poClass()
        );
    }

    @Override
    public long total() {
        return mongoTemplate.count(new SoftDeleteQuery(), poClass());
    }

    @Override
    public List<DO> filterList(long current, int size, Map<String, Object> filters) {
        Criteria criteria = null;
        Map<String, RangeCondition> rangeConditions = new HashMap<>();
        
        for(String key : filters.keySet()){
            Object value = filters.get(key);
            
            if (key.endsWith("_gte")) {
                String fieldName = key.substring(0, key.length() - 4);
                rangeConditions.computeIfAbsent(fieldName, k -> new RangeCondition())
                              .setGte(value);
            } else if (key.endsWith("_lte")) {
                String fieldName = key.substring(0, key.length() - 4);
                rangeConditions.computeIfAbsent(fieldName, k -> new RangeCondition())
                              .setLte(value);
            } else {
                if (criteria == null) {
                    criteria = Criteria.where(key).is(value);
                } else {
                    criteria.and(key).is(value);
                }
            }
        }
        
        for (Map.Entry<String, RangeCondition> entry : rangeConditions.entrySet()) {
            String fieldName = entry.getKey();
            RangeCondition rangeCondition = entry.getValue();
            Criteria rangeCriteria = Criteria.where(fieldName);
            
            if (rangeCondition.getGte() != null && rangeCondition.getLte() != null) {
                rangeCriteria.gte(rangeCondition.getGte()).lte(rangeCondition.getLte());
            } else if (rangeCondition.getGte() != null) {
                rangeCriteria.gte(rangeCondition.getGte());
            } else if (rangeCondition.getLte() != null) {
                rangeCriteria.lte(rangeCondition.getLte());
            }
            
            if (criteria == null) {
                criteria = rangeCriteria;
            } else {
                criteria.andOperator(rangeCriteria);
            }
        }
        
        if(criteria == null){
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "查询条件不能为空！");
        }
        var pos = mongoTemplate.find(
                new SoftDeleteQuery(criteria)
                        .with(Sort.by(Sort.Direction.DESC, "updateTime"))
                        .limit(size).skip((current-1)*size),
                poClass()
        );
        return pos.stream().map(BasePO::toDO).toList();
    }

    @Override
    public long filterTotal(Map<String, Object> filters) {
        Criteria criteria = null;
        Map<String, RangeCondition> rangeConditions = new HashMap<>();
        
        for(String key : filters.keySet()){
            Object value = filters.get(key);
            
            if (key.endsWith("_gte")) {
                String fieldName = key.substring(0, key.length() - 4);
                rangeConditions.computeIfAbsent(fieldName, k -> new RangeCondition())
                              .setGte(value);
            } else if (key.endsWith("_lte")) {
                String fieldName = key.substring(0, key.length() - 4);
                rangeConditions.computeIfAbsent(fieldName, k -> new RangeCondition())
                              .setLte(value);
            } else {
                if (criteria == null) {
                    criteria = Criteria.where(key).is(value);
                } else {
                    criteria.and(key).is(value);
                }
            }
        }
        
        for (Map.Entry<String, RangeCondition> entry : rangeConditions.entrySet()) {
            String fieldName = entry.getKey();
            RangeCondition rangeCondition = entry.getValue();
            Criteria rangeCriteria = Criteria.where(fieldName);
            
            if (rangeCondition.getGte() != null && rangeCondition.getLte() != null) {
                rangeCriteria.gte(rangeCondition.getGte()).lte(rangeCondition.getLte());
            } else if (rangeCondition.getGte() != null) {
                rangeCriteria.gte(rangeCondition.getGte());
            } else if (rangeCondition.getLte() != null) {
                rangeCriteria.lte(rangeCondition.getLte());
            }
            
            if (criteria == null) {
                criteria = rangeCriteria;
            } else {
                criteria.andOperator(rangeCriteria);
            }
        }
        
        if(criteria == null){
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "查询条件不能为空！");
        }
        return mongoTemplate.count(
            new SoftDeleteQuery(criteria),
            poClass()
        );
    }

    private static class RangeCondition {
        private Object gte;
        private Object lte;

        public Object getGte() {
            return gte;
        }

        public void setGte(Object gte) {
            this.gte = gte;
        }

        public Object getLte() {
            return lte;
        }

        public void setLte(Object lte) {
            this.lte = lte;
        }
    }

    @Override
    public boolean existById(String id) {
        return mongoTemplate.exists(new SoftDeleteQuery(
            Criteria.where("_id").is(id)), poClass()
        );
    }

    @Override
    public boolean allExistByIds(List<String> ids) {
        long count = mongoTemplate.count(new SoftDeleteQuery(
            Criteria.where("_id").in(ids)), poClass()
        );
        return  count == ids.size();

    }

    /**
     * 将日期转换为北京时间（UTC+8）并截断到天粒度
     * @param date 原始日期
     * @return 北京时间格式的日期（只保留年月日，时分秒为0）
     */
    private Date convertToBeijingTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        
        // 转换为北京时间（UTC+8）
        int offset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
        calendar.add(Calendar.MILLISECOND, -offset);
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        
        // 截断到天粒度
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        return calendar.getTime();
    }

}
