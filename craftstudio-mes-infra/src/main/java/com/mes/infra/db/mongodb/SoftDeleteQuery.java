package com.mes.infra.db.mongodb;


import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;

public class SoftDeleteQuery extends Query {
    public final static String DELETED_AT = "deleteAt";

    public SoftDeleteQuery(CriteriaDefinition criteriaDefinition) {
        super(criteriaDefinition);
        addCriteria(Criteria.where(DELETED_AT).is(null));
    }

    public SoftDeleteQuery() {
        addCriteria(Criteria.where(DELETED_AT).is(null));
    }
}
