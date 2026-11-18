package com.mes.infra.dal.delivery.packagingBox;

import com.mes.domain.delivery.packagingBox.entity.PackagingBox;
import com.mes.domain.delivery.packagingBox.repository.PackagingBoxRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.packagingBox.po.PackagingBoxPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Repository
public class PackagingBoxRepositoryImp extends BaseRepositoryImp<PackagingBox, PackagingBoxPo>
        implements PackagingBoxRepository {

    @Override
    public Class<PackagingBoxPo> poClass() {
        return PackagingBoxPo.class;
    }

    @Override
    public boolean updateActive(PackagingBox box, PackagingBox existing) {
        SoftDeleteQuery query = new SoftDeleteQuery(Criteria.where("_id").is(box.getId()));
        Update update = new Update()
                .set("boxName", box.getBoxName())
                .set("code", box.getCode())
                .set("cardboardDepth", box.getCardboardDepth())
                .set("cardboardHeight", box.getCardboardHeight())
                .set("cardboardWidth", box.getCardboardWidth())
                .set("boxLength", box.getBoxLength())
                .set("boxWidth", box.getBoxWidth())
                .set("height", box.getHeight())
                .set("cuttingKnife", box.getCuttingKnife())
                .set("rollingCutter", box.getRollingCutter())
                .set("grindingKnife", box.getGrindingKnife())
                .set("updateTime", new Date());
        // 兼容旧记录，并在同一次写入中登记尺寸，避免并发编辑覆盖历史选项。
        retainOptions(update, "cardboardHeightHistory", existing.getCardboardHeight(), box.getCardboardHeight());
        retainOptions(update, "cardboardWidthHistory", existing.getCardboardWidth(), box.getCardboardWidth());
        return mongoTemplate.updateFirst(query, update, poClass()).getMatchedCount() > 0;
    }

    @Override
    public boolean deleteActive(String id) {
        SoftDeleteQuery query = new SoftDeleteQuery(Criteria.where("_id").is(id));
        Update update = new Update().set(SoftDeleteQuery.DELETED_AT, new Date());
        return mongoTemplate.updateFirst(query, update, poClass()).getMatchedCount() > 0;
    }

    @Override
    public PagedResult<PackagingBox> list(long current, int size, Map<String, Object> filters) {
        SoftDeleteQuery query = activeBoxes(filters);
        long total = mongoTemplate.count(query, poClass());
        query.with(Sort.by(Sort.Direction.DESC, "updateTime", "_id"))
                .skip((current - 1) * size)
                .limit(size);
        List<PackagingBox> items = mongoTemplate.find(query, poClass()).stream().map(PackagingBoxPo::toDO).toList();
        return new PagedResult<>(items, total, size, current);
    }

    @Override
    public List<BigDecimal> listCardboardHeights() {
        return dimensionValues("cardboardHeight", "cardboardHeightHistory");
    }

    @Override
    public List<BigDecimal> listCardboardWidths() {
        return dimensionValues("cardboardWidth", "cardboardWidthHistory");
    }

    private SoftDeleteQuery activeBoxes(Map<String, Object> filters) {
        if (filters.isEmpty()) {
            return new SoftDeleteQuery();
        }
        Map<String, Object> storedFilters = new HashMap<>(filters);
        matchDimension(storedFilters, "cardboardHeight");
        matchDimension(storedFilters, "cardboardWidth");
        return new SoftDeleteQuery(buildFilterCriteria(storedFilters));
    }

    private void matchDimension(Map<String, Object> filters, String field) {
        if (filters.remove(field) instanceof BigDecimal expected) {
            // MongoDB 保存十进制字符串，旧记录中的同一数值可能有不同的小数位数。
            List<String> matches = distinctStrings(field).stream()
                    .filter(Objects::nonNull)
                    .filter(value -> new BigDecimal(value).compareTo(expected) == 0)
                    .toList();
            filters.put(field + "_in", matches);
        }
    }

    private List<BigDecimal> dimensionValues(String field, String historyField) {
        return Stream.concat(distinctStrings(field).stream(), distinctStrings(historyField).stream())
                .filter(Objects::nonNull)
                .map(BigDecimal::new)
                .toList();
    }

    private List<String> distinctStrings(String field) {
        String collection = mongoTemplate.getCollectionName(poClass());
        return mongoTemplate.getCollection(collection).distinct(field, String.class).into(new ArrayList<>());
    }

    private void retainOptions(Update update, String field, BigDecimal previous, BigDecimal current) {
        String[] values = Stream.of(previous, current)
                .filter(Objects::nonNull)
                .map(BigDecimal::toString)
                .distinct()
                .toArray(String[]::new);
        if (values.length > 0) {
            update.addToSet(field).each((Object[]) values);
        }
    }
}
