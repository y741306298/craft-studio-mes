package com.mes.infra.db.mongodb;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandSucceededEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableMongoRepositories(basePackages = {
    "com.mes.infra",
    "com.piliofpala.craftstudio.shared"
})
public class MongoConfig {
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    /**
     * 在驱动层记录每一条 MongoDB 查询耗时。放在驱动层可以覆盖 MongoTemplate、
     * Spring Data Repository 以及共享组件发起的查询，避免遗漏某一种访问方式。
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoQueryTimingCustomizer() {
        return builder -> builder.addCommandListener(new MongoQueryTimingListener());
    }

    @Slf4j
    private static final class MongoQueryTimingListener implements CommandListener {
        private static final java.util.Set<String> QUERY_COMMANDS = java.util.Set.of(
                "find", "aggregate", "count", "distinct", "getMore");

        @Override
        public void commandSucceeded(CommandSucceededEvent event) {
            if (QUERY_COMMANDS.contains(event.getCommandName())) {
                log.info("MongoDB query completed: command={}, database={}, elapsedMs={}",
                        event.getCommandName(), event.getDatabaseName(), event.getElapsedTime(java.util.concurrent.TimeUnit.MICROSECONDS) / 1000.0);
            }
        }

        @Override
        public void commandFailed(CommandFailedEvent event) {
            if (QUERY_COMMANDS.contains(event.getCommandName())) {
                log.warn("MongoDB query failed: command={}, database={}, elapsedMs={}",
                        event.getCommandName(), event.getDatabaseName(), event.getElapsedTime(java.util.concurrent.TimeUnit.MICROSECONDS) / 1000.0);
            }
        }
    }
}
