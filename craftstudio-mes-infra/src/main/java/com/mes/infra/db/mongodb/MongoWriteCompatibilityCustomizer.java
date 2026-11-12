package com.mes.infra.db.mongodb;

import com.mongodb.MongoClientSettings;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 当前 MongoDB 部署不支持 retryable writes，在客户端禁用重试写入协议。
 * 在连接串等标准设置应用后执行，无需修改部署或连接串，事务行为保持不变。
 */
@Component
public class MongoWriteCompatibilityCustomizer implements MongoClientSettingsBuilderCustomizer, Ordered {

    @Override
    public void customize(MongoClientSettings.Builder builder) {
        builder.retryWrites(false);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
