package com.mes.infra.db.mongodb;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * 利用 mongodb 中 findAndModify 的原子性构建一个【自增code生成器】，用于需要在库中使用可读唯一键的场景
 */
@Component
public class CodeGenerator {
    @Data
    @Document(collection = "code_counter")
    private static class CodeCounter {
        private String collection;
        private long code = 1;
        public CodeCounter(String collection) {
            this.collection = collection;
        }
    }
    @Autowired
    private MongoTemplate mongoTemplate;
    public long generateAutoIncreasedCode(String collection) {
        Query query =  new Query(Criteria.where("collection").is(collection));
        Update update = new Update().inc("code", 1);
        CodeCounter counter = mongoTemplate.findAndModify(
            query,
            update,
            CodeCounter.class
        );
        if(counter==null){
            mongoTemplate.insert(new CodeCounter(collection));
            return 0;
        }
        return counter.code;
    }
}
