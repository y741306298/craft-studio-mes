package com.mes.infra.db.mongodb;

import com.mongodb.client.MongoClient;
import org.jspecify.annotations.Nullable;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

public class SoftDeleteMongoTemplate extends MongoTemplate {
    public final static String DELETED_AT = "deleteAt";

    public SoftDeleteMongoTemplate(MongoClient mongoClient, String databaseName) {
        super(mongoClient, databaseName);
    }

    public SoftDeleteMongoTemplate(MongoDatabaseFactory mongoDbFactory) {
        super(mongoDbFactory);
    }

    public SoftDeleteMongoTemplate(MongoDatabaseFactory mongoDbFactory, @Nullable MongoConverter mongoConverter) {
        super(mongoDbFactory, mongoConverter);
    }


}
